package com.fc.annotation.core;

import androidx.annotation.NonNull;
import com.fc.annotation.Debounce;
import com.fc.annotation.Delay;
import com.fc.annotation.Throttle;
import org.aspectj.lang.JoinPoint;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ATMethodMatcher {
    public static Map<Class<?>, List<DebounceMethod>> debounceMethodCache = new HashMap<>();
    public static Map<Class<?>, List<ThrottleMethod>> throttleMethodCache = new HashMap<>();
    public static Map<Class<?>, List<DelayMethod>> delayMethodCache = new HashMap<>();

    public List<DebounceMethod> findDebounceMethods(Class<?> classes) {
        List<DebounceMethod> list = debounceMethodCache.get(classes);
        if (list != null) {
            return list;
        }
        synchronized (classes) {
            List<DebounceMethod> methods = debounceMethodCache.get(classes);
            if (methods == null) {
                resolveMethod(classes);
            }
        }
        return debounceMethodCache.get(classes);
    }

    public List<ThrottleMethod> findThrottleMethods(Class<?> classes) {
        List<ThrottleMethod> list = throttleMethodCache.get(classes);
        if (list != null) {
            return list;
        }
        synchronized (classes) {
            List<ThrottleMethod> methods = throttleMethodCache.get(classes);
            if (methods == null) {
                resolveMethod(classes);
            }
        }
        return throttleMethodCache.get(classes);
    }

    public List<DelayMethod> findDelayMethods(Class<?> classes) {
        List<DelayMethod> list = delayMethodCache.get(classes);
        if (list != null) {
            return list;
        }
        synchronized (classes) {
            List<DelayMethod> methods = delayMethodCache.get(classes);
            if (methods == null) {
                resolveMethod(classes);
            }
        }
        return delayMethodCache.get(classes);
    }

    public DebounceMethod findDebounceMethod(JoinPoint joinPoint) {
        return (DebounceMethod) findFromJoinPoint(joinPoint, "Debounce");
    }

    public ThrottleMethod findThrottleMethod(JoinPoint joinPoint) {
        return (ThrottleMethod) findFromJoinPoint(joinPoint, "Throttle");
    }

    public DelayMethod findDelayMethod(JoinPoint joinPoint) {
        return (DelayMethod) findFromJoinPoint(joinPoint, "Delay");
    }

    private ATMethod findFromJoinPoint(JoinPoint joinPoint, String type) {
        Object object = joinPoint.getTarget();
        if (object == null) {
            return null;
        }
        List<? extends ATMethod> list = null;
        if (type.equals("Debounce")) {
            list = findDebounceMethods(object.getClass());
        } else if (type.equals("Throttle")) {
            list = findThrottleMethods(object.getClass());
        } else if (type.equals("Delay")) {
            list = findDelayMethods(object.getClass());
        } else {
            return null;
        }
        ATMethod result = null;
        Object[] args = null;
        for (ATMethod atMethod : list) {
            Method method = atMethod.method;
            String methodName = joinPoint.getSignature().getName();
            if (methodName == null || !methodName.equals(method.getName())) {
                continue;
            }
            if (args == null) {
                args = joinPoint.getArgs();
            }
            Type[] types = method.getGenericParameterTypes();
            if (args == null && (types != null && types.length > 0)) { // 参数个数不一样
                continue;
            }
            if (types == null && (args != null && args.length > 0)) { // 参数个数不一样
                continue;
            }
            if (args == null && args == null) {
                result = atMethod;
                break;
            } else {
                if (types.length != args.length) { // 参数个数不一样
                    continue;
                }
                boolean flag = true;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] == null) {
                        if (isBaseClass(types[i])) { // 基本数据类型 不能为 null
                            flag = false;
                            break;
                        }
                    } else {
                        if (!typeMatch(args[i].getClass(), types[i])) {
                            flag = false;
                            break;
                        }
                    }
                }
                if (flag) {
                    result = atMethod;
                    break;
                }
            }
        }
        return result;
    }

    private boolean typeMatch(@NonNull Class cl, Type type) {
        if (type instanceof Class) {
            boolean result = cl == type;
            if (!result) {
                if ("int".equals(((Class) type).getName())) {
                    return cl.getName().equals("java.lang.Integer");
                } else if ("boolean".equals(((Class) type).getName())) {
                    return cl.getName().equals("java.lang.Boolean");
                } else if ("float".equals(((Class) type).getName())) {
                    return cl.getName().equals("java.lang.Float");
                } else if ("double".equals(((Class) type).getName())) {
                    return cl.getName().equals("java.lang.Double");
                } else if ("long".equals(((Class) type).getName())) {
                    return cl.getName().equals("java.lang.Long");
                } else if ("short".equals(((Class) type).getName())) {
                    return cl.getName().equals("java.lang.Short");
                } else if ("byte".equals(((Class) type).getName())) {
                    return cl.getName().equals("java.lang.Byte");
                } else if ("char".equals(((Class) type).getName())) {
                    return cl.getName().equals("java.lang.Character");
                }
            }
            return result;
        } else if (type instanceof ParameterizedType) {
            Type clType = ((ParameterizedType) type).getRawType();
            if (clType instanceof Class) {
                return ((Class) clType).isAssignableFrom(cl);
            }
        }
        return false;
    }

    private boolean isBaseClass(Type type) {
        if (type instanceof Class) {
            if ("int".equals(((Class) type).getName())) {
                return true;
            } else if ("boolean".equals(((Class) type).getName())) {
                return true;
            } else if ("float".equals(((Class) type).getName())) {
                return true;
            } else if ("double".equals(((Class) type).getName())) {
                return true;
            } else if ("long".equals(((Class) type).getName())) {
                return true;
            } else if ("short".equals(((Class) type).getName())) {
                return true;
            } else if ("byte".equals(((Class) type).getName())) {
                return true;
            } else if ("char".equals(((Class) type).getName())) {
                return true;
            }
        }
        return false;
    }

    private void resolveMethod(Class<?> classes) {
        Method[] methods;
        try {
            methods = classes.getDeclaredMethods();
        } catch (Throwable e) {
            e.printStackTrace();
            methods = classes.getMethods();
        }
        List<DebounceMethod> debounceList = new ArrayList<>();
        List<ThrottleMethod> throttleList = new ArrayList<>();
        List<DelayMethod> delayList = new ArrayList<>();

        for (Method method : methods) {
            Debounce debounce = method.getAnnotation(Debounce.class);
            if (debounce != null) {
                DebounceMethod atMethod = new DebounceMethod(debounce.id(), method, classes, debounce.value(), debounce.threadModel());
                debounceList.add(atMethod);
                continue;
            }
            Throttle throttle = method.getAnnotation(Throttle.class);
            if (throttle != null) {
                ThrottleMethod atMethod = new ThrottleMethod(throttle.id(), method, classes, throttle.value(), throttle.threadModel());
                throttleList.add(atMethod);
                continue;
            }
            Delay delay = method.getAnnotation(Delay.class);
            if (delay != null) {
                DelayMethod atMethod = new DelayMethod(delay.id(), method, classes, delay.value(), delay.threadModel(), delay.isFirstDelay(), delay.isSingleMode(), delay.isUpdateArgs());
                delayList.add(atMethod);
                continue;
            }
        }
        debounceMethodCache.put(classes, debounceList);
        throttleMethodCache.put(classes, throttleList);
        delayMethodCache.put(classes, delayList);
    }
}
