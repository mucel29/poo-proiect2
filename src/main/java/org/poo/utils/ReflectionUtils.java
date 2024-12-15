package org.poo.utils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReflectionUtils {

    private ReflectionUtils() {

    }

    public static void addField(ObjectNode root, Field field, Object caller) {
        field.setAccessible(true);
        try {
            if (field.get(caller) == null) {
                return;
            }
            if (field.getType().isAssignableFrom(String.class)) {
                root.put(field.getName(), (String) field.get(caller));
            } else if (field.getType().isAssignableFrom(Integer.class)) {
                root.put(field.getName(), (Integer) field.get(caller));
            } else if (field.getType().isAssignableFrom(Double.class)) {
                root.put(field.getName(), (Double) field.get(caller));
            } else if (field.getType().equals(Double.TYPE)) {
                root.put(field.getName(), field.getDouble(caller));
            } else if (field.getType().equals(Integer.TYPE)) {
                root.put(field.getName(), field.getInt(caller));
            } else if (field.getType().isAssignableFrom(List.class)) {
                // Only String lists supported
                ArrayNode arr = root.putArray(field.getName());
                List<?> fieldList = (List<?>) field.get(caller);
                for (Object el : fieldList) {
                    arr.add(el.toString());
                }
            } else {
                    // Fallback to String
                    root.put(field.getName(), field.get(caller).toString());
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    public static void copyFields(Field[] from, Field[] to, Object caller, Object toObject) {
        Map<String, Field> fieldMap = new HashMap<>();
        Arrays.stream(to).forEach(field -> fieldMap.put(field.getName(), field));
        try {
            for (Field fromField : from) {
                Field toField = fieldMap.get(fromField.getName());
                if (toField != null) {
                    if (Modifier.isFinal(toField.getModifiers())) {
                        // Can't modify final field
                        // (because we don't share the same package as the declaring class)
                        continue;
                    }
                    toField.setAccessible(true);
                    fromField.setAccessible(true);
//                    System.out.println("setting " + fromField.get(caller));
                    toField.set(toObject, fromField.get(caller));
//                    System.out.println("set? " + toField.get(toObject));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
