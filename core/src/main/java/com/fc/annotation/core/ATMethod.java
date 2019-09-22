package com.fc.annotation.core;

import java.lang.reflect.Method;

public class ATMethod {
    public final String id;
    public final Method method;
    public final Class<?> eventType;

    public ATMethod(String id, Method method, Class<?> eventType) {
        this.id = id;
        this.method = method;
        this.eventType = eventType;
    }
}
