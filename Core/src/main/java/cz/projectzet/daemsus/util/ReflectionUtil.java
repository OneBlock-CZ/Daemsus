package cz.projectzet.daemsus.util;

import cz.projectzet.daemsus.AbstractDaemon;
import cz.projectzet.daemsus.BootLoader;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
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

    public static void inject(Class<? extends Annotation> annotation, Class<?> clazz, Object instance, Object... values) {
        try {
            for (Object value : values) {
                var toInject = Arrays.stream(clazz.getDeclaredFields())
                        .filter(field -> field.isAnnotationPresent(annotation))
                        .filter(field -> field.getType().isAssignableFrom(value.getClass()))
                        .toArray(Field[]::new);

                for (var field : toInject) {
                    field.setAccessible(true);
                    field.set(instance, value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <B extends BootLoader<B>> Collection<Class<? extends AbstractDaemon<B>>> getDependencies(Class<? extends AbstractDaemon<B>> clazz) {
        var dependencies = new HashSet<Class<? extends AbstractDaemon<B>>>();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(InjectDaemon.class)) {
                var type = field.getType();

                if (!AbstractDaemon.class.isAssignableFrom(type)) {
                    continue;
                }

                dependencies.add((Class<? extends AbstractDaemon<B>>) field.getType());
            }
        }

        return dependencies;
    }

    public static <B extends BootLoader<B>> Collection<Class<? extends AbstractDaemon<B>>> getConflictingDaemons(Class<? extends AbstractDaemon<B>> clazz) {
        var conflicts = new HashSet<Class<? extends AbstractDaemon<B>>>();

        var annotation = clazz.getAnnotation(DaemonConflict.class);

        if (annotation != null) {
            for (Class<? extends AbstractDaemon<?>> conflict : annotation.value()) {
                conflicts.add((Class<? extends AbstractDaemon<B>>) conflict);
            }
        }

        return conflicts;
    }
}
