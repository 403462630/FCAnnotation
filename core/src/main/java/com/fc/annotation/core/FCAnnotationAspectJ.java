package com.fc.annotation.core;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class FCAnnotationAspectJ {

    @Pointcut("execution(@com.fc.annotation.Debounce * *(..))")
    public void debouncePointcut() { }

    @Pointcut("execution(@com.fc.annotation.Throttle * *(..))")
    public void throttlePointcut() { }

    @Pointcut("execution(@com.fc.annotation.Delay * *(..))")
    public void delayPointcut() { }

    @Pointcut("execution(void onDestroy()) && within(androidx.fragment.app.FragmentActivity)")
    public void fragmentActivityOnDestroyPointcut() { }

    @Pointcut("execution(void onDestroy()) && within(android.support.v4.app.FragmentActivity)")
    public void v4FragmentActivityOnDestroyPointcut() { }

    @Pointcut("execution(void onDestroyView()) && within(androidx.fragment.app.Fragment)")
    public void fragmentOnDestroyViewPointcut() { }

    @Pointcut("execution(void onDestroyView()) && within(android.support.v4.app.Fragment)")
    public void v4FragmentOnDestroyViewPointcut() { }

    @Pointcut("execution(void onResume()) && within(androidx.fragment.app.FragmentActivity)")
    public void fragmentActivityOnResume() { }

    @Pointcut("execution(void onResume()) && within(android.support.v4.app.FragmentActivity)")
    public void v4FragmentActivityOnResume() { }

    @Pointcut("execution(void onDestroyView()) && within(androidx.fragment.app.DialogFragment)")
    public void fragmentDialogOnDestroyView() { }

    @Pointcut("execution(void onDestroyView()) && within(android.support.v4.app.DialogFragment)")
    public void v4FragmentDialogOnDestroyView() { }

    @Around("debouncePointcut()")
    public void debounceAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        ATMethodManager.getInstance().runDebounceAdvice(joinPoint);
        // 下面的方法 不起作用（真奇怪，不知道为什么）
//        ATMethodManager.getInstance().runDebounceAdvice((JoinPoint) joinPoint);
    }

    @Around("throttlePointcut()")
    public void throttleAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        ATMethodManager.getInstance().runThrottleAdvice(joinPoint);
        // 下面的方法 不起作用（真奇怪，不知道为什么）
//        ATMethodManager.getInstance().runThrottleAdvice((JoinPoint) joinPoint);
    }

    @Around("delayPointcut()")
    public void delayAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        ATMethodManager.getInstance().runDelayAdvice(joinPoint);
        // 下面的方法 不起作用（真奇怪，不知道为什么）
//        ATMethodManager.getInstance().runThrottleAdvice((JoinPoint) joinPoint);
    }

    /**
     * 多余的操作，因为ATMethodManager持有的是弱引用，不会造成内存泄露
     * @param joinPoint
     */
    @After("fragmentActivityOnDestroyPointcut() || v4FragmentActivityOnDestroyPointcut() || fragmentOnDestroyViewPointcut() || v4FragmentOnDestroyViewPointcut() || fragmentDialogOnDestroyView() || v4FragmentDialogOnDestroyView()")
    public void releaseAdvice(JoinPoint joinPoint) {
        if (joinPoint.getTarget() != null) {
            if (ATMethodManager.getInstance().isAutoRelease()) {
                ATMethodManager.getInstance().release(joinPoint.getTarget());
            }
        }
    }

    @After("fragmentActivityOnResume() || v4FragmentActivityOnResume()")
    public void clearCacheAdvice(JoinPoint joinPoint) {
        if (joinPoint.getTarget() != null) {
            ATMethodManager.getInstance().clearInValidCache();
        }
    }
}
