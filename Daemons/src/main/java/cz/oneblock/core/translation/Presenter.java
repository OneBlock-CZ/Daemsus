package cz.oneblock.core.translation;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.time.Duration;
import java.util.List;

public interface Presenter {

    static TextComponent squash(List<TextComponent> components) {
        if (components.size() == 1) return components.get(0);
        var result = Component.empty();

        for (int i = 0; i < components.size(); i++) {
            result = result.append(components.get(i));
            if (i != components.size() - 1) result = result.append(Component.newline());
        }

        return result;
    }

    void presentFromData(Audience audience, List<TextComponent> text, TextComponent title, TextComponent subtitle, Duration fadeIn, Duration stay, Duration fadeOut, String sound, Float volume, Float pitch);

}
