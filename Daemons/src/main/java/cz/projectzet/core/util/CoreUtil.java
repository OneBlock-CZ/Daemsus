package cz.projectzet.core.util;

public class CoreUtil {

    public static <T> T getOrDefault(T value, T defaultKys) {
        return value == null ? defaultKys : value;
    }

}
