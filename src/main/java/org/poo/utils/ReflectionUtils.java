package org.poo.utils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public final class ReflectionUtils {

    private ReflectionUtils() {

    }

    /**
     * Adds a given `Field` to an `ObjectNode`
     * @param root the ObjectNode
     * @param field the Field
     * @param caller the Declaring object of the field
     */
    public static void addField(final ObjectNode root, final Field field, final Object caller) {
        field.setAccessible(true);
        try {
            // Check if the field exists
            if (field.get(caller) == null) {
                return;
            }
            // Check to which object to cast
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

    /**
     * Copy fields from one object to another.
     * `to` and `from` are needed because the
     * caller can specify fields from the object's superclass
     * @param from the fields to copy from
     * @param to the fields to copy to
     * @param caller the object to which `from` belongs
     * @param toObject the object to which `to` belongs
     */
    public static void copyFields(
            final Field[] from,
            final Field[] to,
            final Object caller,
            final Object toObject
    ) {
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

                    toField.set(toObject, fromField.get(caller));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find an object's field
     * @param fieldName the name of the field
     * @param caller the owner of the field
     * @return the Field if it can be accessed or null
     */
    public static Field findField(final String fieldName, final Object caller) {
        try {
            return Arrays.stream(caller.getClass().getDeclaredFields())
                    .filter((field) -> field.getName().equals(fieldName))
                    .findFirst()
                    .get();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

}
