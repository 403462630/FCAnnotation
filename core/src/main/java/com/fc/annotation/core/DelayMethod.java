package com.fc.annotation.core;

import com.fc.annotation.ATMode;

import java.lang.reflect.Method;

public class DelayMethod extends ATMethod {
    public final long time;
    public final ATMode atMode;
    public final boolean isFirstDelay;
    public final boolean isSingleMode;
    public final boolean isUpdateArgs;

    public DelayMethod(String id, Method method, Class<?> eventType, long time, ATMode atMode, boolean isFirstDelay, boolean isSingleMode, boolean isUpdateArgs) {
        super(id, method, eventType);
        this.time = time;
        this.atMode = atMode;
        this.isFirstDelay = isFirstDelay;
        this.isSingleMode = isSingleMode;
        this.isUpdateArgs = isUpdateArgs;
    }
}
