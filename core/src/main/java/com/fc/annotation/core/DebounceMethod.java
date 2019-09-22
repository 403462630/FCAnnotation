package com.fc.annotation.core;

import com.fc.annotation.ATMode;

import java.lang.reflect.Method;

public class DebounceMethod extends ATMethod {
    public final long time;
    public final ATMode atMode;

    public DebounceMethod(String id, Method method, Class<?> eventType, long time, ATMode atMode) {
        super(id, method, eventType);
        this.time = time;
        this.atMode = atMode;
    }
}
