package de.uni_hamburg.corpora.utilities.quest;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Set;
import java.util.stream.Collectors;

public class ToString {

    // Taken from https://memorynotfound.com/generic-object-tostring-method-reflections-java/
    public static String toString(Object object, boolean recursive) {
        if (object == null) return "null";

        Class<?> clazz = object.getClass();
        StringBuilder sb = new StringBuilder(clazz.getSimpleName()).append(" {");

        while (clazz != null && !clazz.equals(Object.class)) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    try {
                        f.setAccessible(true);
                        sb.append(f.getName()).append(" = ").append(f.get(object)).append(",");
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (!recursive) {
                break;
            }
            clazz = clazz.getSuperclass();
        }

        sb.deleteCharAt(sb.lastIndexOf(","));
        return sb.append("}").toString();
    }
}
