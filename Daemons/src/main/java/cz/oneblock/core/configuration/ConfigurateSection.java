package cz.oneblock.core.configuration;

import com.google.common.base.Splitter;
import cz.oneblock.core.util.ThrowableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public record ConfigurateSection(CommentedConfigurationNode configuration) {

    public String getString(String path) {
        return get(String.class, path);
    }

    public Integer getInteger(String path) {
        return get(Integer.class, path);
    }

    public Boolean getBoolean(String path) {
        return get(Boolean.class, path);
    }

    public Long getLong(String path) {
        return get(Long.class, path);
    }

    public Float getFloat(String path) {
        return get(Float.class, path);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return get(Boolean.class, path, defaultValue);
    }

    public int getInt(String path, int defaultValue) {
        return get(Integer.class, path, defaultValue);
    }

    public long getLong(String path, long defaultValue) {
        return get(Long.class, path, defaultValue);
    }

    public float getFloat(String path, float defaultValue) {
        return get(Float.class, path, defaultValue);
    }

    public <T> T get(Class<T> clazz, String path) {
        return configurationFunction(path, node -> {
            if (node.isList()) return null;
            return node.get(clazz);
        });
    }

    @NotNull
    public <T> T get(Class<T> clazz, String path, T defaultValue) {
        var present = get(clazz, path);

        return present != null ? present : defaultValue;
    }

    public Object getObject(String path) {
        return configurationFunction(path, node -> {
            if (node.isList()) {
                return node.getList(String.class);
            } else if (node.isMap()) return new ConfigurateSection(node);
            else {
                return node.get(Object.class);
            }
        });
    }

    public List<String> getStringList(String path) {
        return getList(String.class, path);
    }

    public <T> List<T> getList(Class<T> clazz, String path) {
        return configurationFunction(path, node -> {
            if (!node.isList()) return null;
            return node.getList(clazz);
        });
    }

    public void set(String path, Object value) {
        try {
            resolve(path)
                    .set(value);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    public CommentedConfigurationNode resolve(String key) {
        return configuration.node(Splitter.on('.').splitToList(key.replace('_', '-')).toArray());
    }

    public <T> T configurationFunction(String path, ThrowableFunction<CommentedConfigurationNode, T, Exception> function) {
        try {
            var node = resolve(path);
            if (node == null || node.isNull() || node.virtual()) return null;
            return function.run(resolve(path));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ConfigurateSection getSection(String path) {
        var node = resolve(path);

        if (node.isNull() || node.virtual()) {
            try {
                node.set(new HashMap<>());
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        if (!node.isMap()) return null;

        return new ConfigurateSection(node);
    }

    public String name() {
        var key = configuration.key();
        return key == null ? null : key.toString();
    }

    public Collection<String> getKeys() {
        return configuration.childrenMap().keySet().stream().map(Object::toString).toList();
    }

    public double getDouble(String path) {
        return get(Double.class, path);
    }

    @Nullable
    public List<ConfigurateSection> getSectionList(String path) {
        return configurationFunction(path, node -> {
            if (!node.isList()) return null;
            return node.getList(CommentedConfigurationNode.class).stream().map(ConfigurateSection::new).toList();
        });
    }
}
