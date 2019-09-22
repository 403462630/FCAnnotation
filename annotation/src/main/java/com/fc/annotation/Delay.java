package com.fc.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Delay {

    /**
     * 延迟 时间
     * @return
     */
    long value() default 0L;

    /**
     * 定义方法的唯一值，用于 cancel 回调时使用
     * 如果没设置时，调用cancel方法时 可传 method name
     * @return
     */
    String id() default "";

    /**
     * 方法回调线程
     * @return ATMode.ASYNC 子线程
     *         ATMode.MAIN ui线程
     *         ATMode.NONE 如果当前线程是ui线程，则回调在ui线程，否则在子线程
     */
    ATMode threadModel() default ATMode.NONE;

    /**
     * 第一次调用@Delay方法 是否 需要延迟
     * @return false 不需要，true 需要
     */
    boolean isFirstDelay() default false;

    /**
     * 对于一个对象里所有的@Delay方法是否 同时调用 还是 单独调用
     * @return true 表示 每个@Delay方法都是独立的 runnable，并使用自己的 delay time 延迟调用
     *         false 表示 每个@Delay方法是否 共用一个 runnable，实现所有的方法同时调用（以你调用的第一个方法delay time 为延迟时间）
     */
    boolean isSingleMode() default true;

    /**
     * 每次调用方法，是否更新最新的参数
     * @return true 更新，false 不更新
     */
    boolean isUpdateArgs() default true;
}
