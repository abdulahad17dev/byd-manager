package com.byd.vehiclecontrol.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtils {

    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            // Пробуем найти в родительских классах
            Class<?> parent = clazz.getSuperclass();
            if (parent != null) {
                return findMethod(parent, methodName, paramTypes);
            }
            throw new RuntimeException("Method not found: " + methodName, e);
        }
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            // Пробуем найти в родительских классах
            Class<?> parent = clazz.getSuperclass();
            if (parent != null) {
                return findField(parent, fieldName);
            }
            throw new RuntimeException("Field not found: " + fieldName, e);
        }
    }

    public static Object invokeMethod(Object obj, String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    paramTypes[i] = args[i].getClass();
                }
            }

            Method method = findMethod(obj.getClass(), methodName, paramTypes);
            return method.invoke(obj, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke method: " + methodName, e);
        }
    }

    public static void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            Field field = findField(obj.getClass(), fieldName);

            // Убираем final модификатор если есть
            if ((field.getModifiers() & 0x10) != 0) {
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~0x10);
            }

            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}