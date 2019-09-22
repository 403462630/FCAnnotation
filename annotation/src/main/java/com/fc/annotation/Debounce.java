package com.fc.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Debounce {

    /**
     * 时间间隔
     * @return
     */
    long value() default 0L;

    /**
     * 方法唯一id，暂时没用，只有delay用到了
     * @return
     */
    String id() default "";

    /**
     * 方法回调线程
     * @return ATMode.ASYNC 子线程
     *         ATMode.MAIN ui线程
     *         ATMode.NONE 当前线程
     */
    ATMode threadModel() default ATMode.NONE;
}
