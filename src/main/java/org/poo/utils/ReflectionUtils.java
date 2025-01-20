package org.poo.utils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ReflectionUtils {

    private ReflectionUtils() {

    }

    /**
     * Adds a given {@code Field} to an {@code ObjectNode}
     *
     * @param root the {@code ObjectNode}
     * @param field the {@code Field}
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
                ArrayNode arr = root.putArray(field.getName());

                // List<Double> support

                // Get generic type and check if it's Double
                Class<?> genericClass = (Class<?>) ((ParameterizedType) field.getGenericType())
                        .getActualTypeArguments()[0];
                if (genericClass.isAssignableFrom(Double.class)) {
                    List<Double> fieldList = (List<Double>) field.get(caller);
                    for (Double el : fieldList) {
                        arr.add(el);
                    }
                } else {
                    // Fallback to String
                    List<?> fieldList = (List<?>) field.get(caller);
                    for (Object el : fieldList) {
                        arr.add(el.toString());
                    }
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
     * {@code to} and {@code from} are needed because the
     * caller can specify fields from the object's superclass
     *
     * @param from the fields to copy from
     * @param to the fields to copy to
     * @param caller the object to which {@code from}  belongs
     * @param toObject the object to which {@code to} belongs
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
     *
     * @param fieldName the name of the field
     * @param caller the owner of the field
     * @return the {@code Field} if it can be accessed or {@code null}
     */
    public static Field findField(final String fieldName, final Object caller) {
         Optional<Field> targetField =
                 Arrays.stream(caller.getClass().getDeclaredFields())
                         .filter((field) -> field.getName().equals(fieldName))
                         .findFirst();

        return targetField.orElse(null);
    }

}
