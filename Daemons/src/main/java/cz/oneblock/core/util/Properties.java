package cz.oneblock.core.util;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple type-unsafe map wrapper.
 *
 * @author kyngs
 */
public class Properties {

    private final Map<String, Object> properties;

    public Properties() {
        this.properties = new HashMap<>();
    }

    public <T> T get(String key) {
        return (T) properties.get(key);
    }

    public void set(String key, Object value) {
        properties.put(key, value);
    }

}
