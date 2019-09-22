package com.fc.annotation.core;

import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ATMethodManager {
    private static ATMethodManager instance;
    private static long CLEAR_TIME_PERIOD = 30 * 1000;
    private Map<WeakReference<Object>, ATMethodHandler> atMethodHandlerCache = new HashMap<>();
    private Map<String, WeakReference<Object>> keyCache = new HashMap<>();
    private long clearTime = 0;
    /** 对于activity 和 fragment 是否自动在onDestroy release */
    private boolean isAutoRelease = true;

    public static ATMethodManager getInstance() {
        if (instance == null) {
            synchronized (ATMethodManager.class) {
                if (instance == null) {
                    instance = new ATMethodManager();
                }
            }
        }
        return instance;
    }

    public boolean isAutoRelease() {
        return isAutoRelease;
    }

    public void setAutoRelease(boolean autoRelease) {
        isAutoRelease = autoRelease;
    }

    void runDelayAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        Object target = joinPoint.getTarget();
        if (target == null) {
            joinPoint.proceed();
        } else {
            getATMethodHandler(target, true).runDelayAdvice(joinPoint);
        }
    }

    void runThrottleAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        Object target = joinPoint.getTarget();
        if (target == null) {
            joinPoint.proceed();
        } else {
            getATMethodHandler(target, true).runThrottleAdvice(joinPoint);
        }
    }

    void runDebounceAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        Object target = joinPoint.getTarget();
        if (target == null) {
            joinPoint.proceed();
        } else {
            getATMethodHandler(target, true).runDebounceAdvice(joinPoint);
        }
    }

    private ATMethodHandler getATMethodHandler(Object target, boolean createFlag) {
        clearInValidCache();
        String key = "ATMethodHandler_" + target;
        WeakReference<Object> objectWeakReference = keyCache.get(key);
        if (objectWeakReference != null) {
            Object object = objectWeakReference.get();
            if (object != null) {
                ATMethodHandler atMethodHandler = atMethodHandlerCache.get(objectWeakReference);
                if (atMethodHandler != null) {
                    return atMethodHandler;
                }
            } else {
                atMethodHandlerCache.remove(objectWeakReference);
                keyCache.remove(key);
            }
        }
        if (createFlag) {
            ATMethodHandler atMethodHandler = new ATMethodHandler();
            objectWeakReference = new WeakReference(target);
            atMethodHandlerCache.put(objectWeakReference, atMethodHandler);
            keyCache.put(key, objectWeakReference);
            return atMethodHandler;
        } else {
            return null;
        }
    }

    public void cancelDelayMethod(Object target, String methodId) {
        ATMethodHandler atMethodHandler = getATMethodHandler(target, false);
        if (atMethodHandler != null) {
            atMethodHandler.cancelMethodDelay(target, methodId);
        }
    }

    public void cancelAllDelayMethod(Object target) {
        ATMethodHandler atMethodHandler = getATMethodHandler(target, false);
        if (atMethodHandler != null) {
            atMethodHandler.cancelAllMethodDelay();
        }
    }

    public void release(Object target) {
        String key = "ATMethodHandler_" + target;
        ATMethodHandler atMethodHandler = atMethodHandlerCache.remove(keyCache.get(key));
        if (atMethodHandler != null) {
            atMethodHandler.release();
        }
        keyCache.remove(key);
    }

    /**
     * 时间超过30秒钟
     */
    void clearInValidCache() {
        if ((System.currentTimeMillis() - clearTime) > CLEAR_TIME_PERIOD) {
            clearTime = System.currentTimeMillis();
            List<String> clearList = new ArrayList<>();
            for (String key : keyCache.keySet()) {
                if (keyCache.get(key).get() == null) {
                    clearList.add(key);
                }
            }
            if (clearList.size() > 0) {
                for (String key : clearList) {
                    ATMethodHandler atMethodHandler = atMethodHandlerCache.remove(keyCache.get(key));
                    if (atMethodHandler != null) {
                        atMethodHandler.release();
                    }
                    keyCache.remove(key);
                }
            }
        }
    }
}
