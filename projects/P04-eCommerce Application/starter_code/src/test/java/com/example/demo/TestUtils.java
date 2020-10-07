package com.example.demo;

import java.lang.reflect.Field;

public class TestUtils {

    public static void injectObject(Object target, String fieldName, Object toInject) throws NoSuchFieldException, IllegalAccessException {
        boolean wasPrivate = false;
        Field field = target.getClass().getDeclaredField(fieldName);
        if (!field.isAccessible()){
            wasPrivate = true;
            field.setAccessible(true);
        }
        field.set(target,toInject);
        if (wasPrivate){
            field.setAccessible(false);
        }


    }
}
