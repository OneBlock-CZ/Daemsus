package cz.oneblock.core.util;

import cz.oneblock.core.AbstractDaemon;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

@SuppressWarnings("unchecked")
public class ReflectionUtil {

    @Nullable
    public static final Unsafe UNSAFE;

    static {
        Unsafe unsafe;
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (Unsafe) unsafeField.get(null);
        } catch (Exception e) {
            unsafe = null;
        }
        UNSAFE = unsafe;
    }

    public static Collection<Class<? extends AbstractDaemon<?>>> getConflictingDaemons(Class<? extends AbstractDaemon<?>> clazz) {
        var conflicts = new HashSet<Class<? extends AbstractDaemon<?>>>();

        var annotation = clazz.getAnnotation(DaemonConflict.class);

        if (annotation != null) {
            conflicts.addAll(Arrays.asList(annotation.value()));
        }

        return conflicts;
    }

    public static Collection<Field> getAllFields(Class<?> clazz) {
        var fields = new HashSet<Field>();
        var current = clazz;

        while (current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }

        return fields;
    }

}
