package cz.oneblock.core.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ComponentUtil {

    @Nullable
    public static TextComponent formatComponent(TextComponent component, String key, Component replacement) {
        if (component == null) return null;
        return (TextComponent) component.replaceText(key, replacement);
    }

    @Nullable
    public static TextComponent formatComponent(@Nullable TextComponent component, Map<String, String> replacements) {
        if (component == null) return null;

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            //noinspection UnstableApiUsage
            component = (TextComponent) component.replaceText(entry.getKey(), Component.text(entry.getValue()));
        }
        return component;
    }

    public static List<TextComponent> formatComponents(List<TextComponent> components, String key, Component replacement) {
        return components.stream().map(component -> formatComponent(component, key, replacement)).toList();
    }

    public static List<TextComponent> formatComponents(List<TextComponent> components, Map<String, String> replacements) {
        return components.stream().map(component -> formatComponent(component, replacements)).toList();
    }


}
