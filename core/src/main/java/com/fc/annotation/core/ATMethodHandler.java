package com.fc.annotation.core;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.collection.LongSparseArray;
import com.fc.annotation.ATMode;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ATMethodHandler {
    private static int index = 0;
    private static ExecutorService executorService = null;
    private static ExecutorService delayExecutorService = null;
    private static Handler mainHandler = null;
    private static Handler asyncHandler = null;
    private static ATMethodMatcher methodMatcher = null;

    private static ArrayMap<String, DebounceMethod> debounceMethodSparseArray = null;
    private static ArrayMap<String, ThrottleMethod> throttleMethodSparseArray = null;
    private static ArrayMap<String, DelayMethod> delayMethodSparseArray = null;

    private Map<String, MethodRunnable> delayRunnableCache = new HashMap<>();
    private Map<Object, Long> timeCache = new HashMap<>();
    private ArrayMap<String, Boolean> isFirstCallMethod = new ArrayMap<>();

    private Object delayLock = new Object();
    private boolean isRelease = false;

    public ATMethodHandler() {
        if (methodMatcher == null) {
            methodMatcher = new ATMethodMatcher();
        }
        if (debounceMethodSparseArray == null) {
            debounceMethodSparseArray = new ArrayMap<>();
        }
        if (delayMethodSparseArray == null) {

            delayMethodSparseArray = new ArrayMap<>();
        }
        if (throttleMethodSparseArray == null) {
            throttleMethodSparseArray = new ArrayMap<>();
        }
        if (mainHandler == null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                mainHandler = new Handler();
            } else {
                mainHandler = new Handler(Looper.getMainLooper());
            }
        }
    }

    private void initAsyncHandler() {
        if (asyncHandler == null) {
            index ++;
            HandlerThread handlerThread = new HandlerThread("async-handler-thread-" + index);
            handlerThread.start();
            asyncHandler = new Handler(handlerThread.getLooper());
        }
    }

    private void initExecutorService() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor(r -> {
                index++;
                return new Thread(r, "ATMethod-default-pool-" + index);
            });
        }
    }

    private void initDelayExecutorService() {
        if (delayExecutorService == null || delayExecutorService.isShutdown()) {
            delayExecutorService = Executors.newSingleThreadExecutor(r -> {
                index++;
                return new Thread(r, "delay-executor-default-pool-" + index);
            });
        }
    }

    /** 在子线程池中release */
    public void release() {
        this.isRelease = true;
        isFirstCallMethod.clear();
        timeCache.clear();
        cancelAllMethodDelay();
    }

    void cancelMethodDelay(Object target, String methodKey) {
        if (isRelease) {
            return ;
        }
        if (!delayRunnableCache.isEmpty()) {
            synchronized (delayLock) {
                List<String> removeList = new ArrayList<>();
                String methodPrefixKey = getDelayMethodKey(methodKey + "_");
                String runnablePrefixKey = methodPrefixKey;
                String singleRunnablePrefixKey = "Delay_" + target.getClass().getName();
                for (String key : delayRunnableCache.keySet()) {
                    if (key.startsWith(runnablePrefixKey)) { // remove isSingleMode == true 的 runnable
                        MethodRunnable runnable = delayRunnableCache.get(key);
                        if (runnable != null) {
                            mainHandler.removeCallbacks(runnable);
                            asyncHandler.removeCallbacks(runnable);
                        }
                        removeList.add(key);
                    }
                    if (key.equals(singleRunnablePrefixKey)) { // remove isSingleMode == false 的 runnable
                        MethodRunnable runnable = delayRunnableCache.get(key);
                        if (runnable != null) {
                            if (runnable.linkedHashMap.size() <= 1) { // 小于等于一个method，直接remove runnable
                                mainHandler.removeCallbacks(runnable);
                                asyncHandler.removeCallbacks(runnable);
                                removeList.add(key);
                            } else { // 否则 remove 对应 method
                                runnable.remove(methodPrefixKey);
                            }
                        } else {
                            removeList.add(key);
                        }
                    }
                }
                if (!removeList.isEmpty()) {
                    for (String key : removeList) {
                        delayRunnableCache.remove(key);
                    }
                }
            }
        }
    }

    void cancelAllMethodDelay() {
        if (!delayRunnableCache.isEmpty()) {
            synchronized (delayLock) {
                for (String key : delayRunnableCache.keySet()) {
                    Runnable runnable = delayRunnableCache.get(key);
                    if (runnable != null) {
                        mainHandler.removeCallbacks(runnable);
                        asyncHandler.removeCallbacks(runnable);
                    }
                }
                delayRunnableCache.clear();
            }
        }
    }

    public Object runDebounceAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        return runDebounceAdvice((JoinPoint) joinPoint);
    }

    public Object runDebounceAdvice(JoinPoint joinPoint) throws Throwable {
        if (isRelease) {
            return null;
        }
        Object result = null;
        if (joinPoint.getTarget() == null) {
            result = process(null, joinPoint);
        } else {
            String id = joinPoint.getStaticPart().getSignature().getDeclaringType().getSimpleName() + "_" + joinPoint.getStaticPart().getId();
            DebounceMethod debounceMethod = debounceMethodSparseArray.get(id);
            if (debounceMethod == null) {
                debounceMethod = methodMatcher.findDebounceMethod(joinPoint);
                debounceMethodSparseArray.put(id, debounceMethod);
            }
            if (debounceMethod != null) {
                if (canDebounceHandler(debounceMethod)) {
                    result = process(debounceMethod, joinPoint);
                }
            } else {
                result = process(null, joinPoint);
            }
        }
        return result;
    }

    public Object runThrottleAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        return runThrottleAdvice((JoinPoint) joinPoint);
    }

    public Object runThrottleAdvice(JoinPoint joinPoint) throws Throwable {
        if (isRelease) {
            return null;
        }
        Object result = null;
        if (joinPoint.getTarget() == null) {
            result = process(null, joinPoint);
        } else {
            String id = joinPoint.getStaticPart().getSignature().getDeclaringType().getSimpleName() + "_" + joinPoint.getStaticPart().getId();
            ThrottleMethod throttleMethod = throttleMethodSparseArray.get(id);
            if (throttleMethod == null) {
                throttleMethod = methodMatcher.findThrottleMethod(joinPoint);
                throttleMethodSparseArray.put(id, throttleMethod);
            }
            if (throttleMethod != null) {
                if (canThrottleHandler(throttleMethod)) {
                    result = process(throttleMethod, joinPoint);
                }
            } else {
                result = process(null, joinPoint);
            }
        }
        return result;
    }

    public Object runDelayAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        if (isRelease) {
            return null;
        }
        Object result = null;
        if (joinPoint.getTarget() == null) {
            result = process(null, joinPoint);
        } else {
            String id = joinPoint.getStaticPart().getSignature().getDeclaringType().getSimpleName() + "_" + joinPoint.getStaticPart().getId();
            DelayMethod delayMethod = delayMethodSparseArray.get(id);
            if (delayMethod == null) {
                delayMethod = methodMatcher.findDelayMethod(joinPoint);
                delayMethodSparseArray.put(id, delayMethod);
            }
            if (delayMethod != null) {
                if (delayMethod.time <= 0) {
                    result = process(delayMethod, joinPoint);
                } else if (!delayMethod.isFirstDelay && isFirstCallMethod.get(id) == null) {
                    isFirstCallMethod.put(id, true);
                    result = process(delayMethod, joinPoint);
                } else {
                    delayProcess(delayMethod, joinPoint);
                }
            } else {
                result = process(null, joinPoint);
            }
        }
        return result;
    }

    private Object process(ATMethod atMethod, JoinPoint joinPoint) throws Throwable {
        if (joinPoint instanceof ProceedingJoinPoint) {
            if (atMethod == null) {
                return ((ProceedingJoinPoint) joinPoint).proceed();
            } else {
                ATMode atMode = ATMode.NONE;
                if (atMethod instanceof DebounceMethod) {
                    atMode = ((DebounceMethod) atMethod).atMode;
                } else if (atMethod instanceof ThrottleMethod) {
                    atMode = ((ThrottleMethod) atMethod).atMode;
                }
                if (atMode == ATMode.MAIN) {
                    runUI(() -> {
                        if (isRelease) {
                            return ;
                        }
                        try {
                            ((ProceedingJoinPoint) joinPoint).proceed();
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    });
                    return null;
                } else if (atMode == ATMode.ASYNC) {
                    async(() -> {
                        if (isRelease) {
                            return ;
                        }
                        try {
                            ((ProceedingJoinPoint) joinPoint).proceed();
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    });
                    return null;
                } else {
                    return ((ProceedingJoinPoint) joinPoint).proceed();
                }
            }
        } else {
            return null;
        }
    }

    private void delayProcess(@NonNull DelayMethod delayMethod, @NonNull ProceedingJoinPoint joinPoint) {
        initDelayExecutorService();
        boolean isMainUI = Looper.getMainLooper() == Looper.myLooper();
        delayExecutorService.execute(() -> {
            String runnableKey;
            String methodKey;
            if (TextUtils.isEmpty(delayMethod.id)) { // 没有设置id，则以joinPoint name + id为methodKey
                methodKey = getDelayMethodKey(joinPoint.getSignature().getName() + "_" + joinPoint.getStaticPart().getId());
                if (delayMethod.isSingleMode) {
                    runnableKey = methodKey; // 约定 isSingleMode == true，runnableKey == methodKey，方便处理
                } else {
                    runnableKey = "DELAY_" + joinPoint.getTarget().getClass().getName();
                }
            } else { //设置了id
                methodKey = getDelayMethodKey(delayMethod.id + "_");
                if (delayMethod.isSingleMode) {
                    runnableKey = methodKey;
                } else {
                    runnableKey = "DELAY_" + joinPoint.getTarget().getClass().getName();
                }
            }
            synchronized (delayLock) {
                if (isRelease) {
                    return ;
                }
                MethodRunnable runnable = delayRunnableCache.get(runnableKey);
                if (runnable == null) {
                    runnable = new MethodRunnable(runnableKey);
                    runnable.put(methodKey, joinPoint, delayMethod.isUpdateArgs);
                    delayRunnableCache.put(runnableKey, runnable);
                    if (delayMethod.atMode == ATMode.MAIN) {
                        mainHandler.postDelayed(runnable, delayMethod.time);
                    } else if (delayMethod.atMode == ATMode.NONE && isMainUI) {
                        mainHandler.postDelayed(runnable, delayMethod.time);
                    } else {
                        initAsyncHandler();
                        asyncHandler.postDelayed(runnable, delayMethod.time);
                    }
                } else {
                    runnable.put(methodKey, joinPoint, delayMethod.isUpdateArgs);
                }
            }
        });
    }

    private void removeCacheMethodRunnable(String key) {
        delayRunnableCache.remove(key);
    }

    private void async(Runnable runnable) {
        initExecutorService();
        executorService.execute(runnable);
    }

    private void runUI(Runnable runnable) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    private boolean canDebounceHandler(DebounceMethod debounceMethod) {
        long time = debounceMethod.time;
        if (time <= 0) {
            return true;
        } else {
            long currentTime = System.currentTimeMillis();
            Long lastTime = timeCache.get(debounceMethod);
            timeCache.put(debounceMethod, currentTime);
            if (lastTime == null || (currentTime - lastTime) > time) {
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean canThrottleHandler(ThrottleMethod throttleMethod) {
        long time = throttleMethod.time;
        if (time <= 0) {
            return true;
        } else {
            long currentTime = System.currentTimeMillis();
            Long lastTime = timeCache.get(throttleMethod);
            if (lastTime == null || (currentTime - lastTime) > time) {
                timeCache.put(throttleMethod, currentTime);
                return true;
            } else {
                return false;
            }
        }
    }

    private String getDelayMethodKey(String key) {
        return "Delay_" + key;
    }

    class MethodRunnable implements Runnable {

        public String key;
        public LinkedHashMap<String, ProceedingJoinPoint> linkedHashMap = new LinkedHashMap<>();

        public MethodRunnable(String key) {
            this.key = key;
        }

        public void put(String methodKey, ProceedingJoinPoint joinPoint, boolean isUpdate) {
            if (isUpdate || linkedHashMap.get(methodKey) == null) {
                linkedHashMap.put(methodKey, joinPoint);
            }
        }

        public void remove(String keyPrefix) {
            List<String> list = new ArrayList<>();
            for (String key : linkedHashMap.keySet()) {
                if (key.startsWith(keyPrefix)) {
                    list.add(key);
                }
            }
            for (String key : list) {
                linkedHashMap.remove(key);
            }
        }

        @Override
        public void run() {
            synchronized (delayLock) {
                try {
                    if (!isRelease) {
                        for (String key : linkedHashMap.keySet()) {
                            ProceedingJoinPoint joinPoint = linkedHashMap.get(key);
                            if (joinPoint != null) {
                                joinPoint.proceed();
                            }
                        }
                    }
                    removeCacheMethodRunnable(key);
                } catch (Throwable e) {
                    removeCacheMethodRunnable(key);
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
