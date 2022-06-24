package cz.projectzet.core.translation;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static cz.projectzet.core.util.ComponentUtil.formatComponent;
import static cz.projectzet.core.util.ComponentUtil.formatComponents;

public record Message(String sound, Float pitch, Float volume, List<TextComponent> text, TextComponent title,
                      TextComponent subtitle, Duration fadeIn, Duration stay, Duration fadeOut,
                      Presenter presenter) implements Presentable {

    public Message format(Object... replacements) {
        if (replacements.length == 0) return this;

        var replaceMap = new HashMap<String, String>();

        String key = null;

        for (int i = 0; i < replacements.length; i++) {
            if (i % 2 != 0) {
                replaceMap.put(key, replacements[i].toString());
            } else {
                key = replacements[i].toString();
            }
        }

        return new Message(
                sound,
                pitch,
                volume,
                formatComponents(text, replaceMap),
                formatComponent(title, replaceMap),
                formatComponent(subtitle, replaceMap),
                fadeIn,
                stay,
                fadeOut,
                presenter
        );
    }

    public Message format(Object key, Component replacement) {
        if (replacement == null) return this;
        var keyName = key.toString();
        return new Message(
                sound,
                pitch,
                volume,
                formatComponents(text, keyName, replacement),
                formatComponent(title, keyName, replacement),
                formatComponent(subtitle, keyName, replacement),
                fadeIn,
                stay,
                fadeOut,
                presenter
        );
    }

    public Message startStyle(Style style) {
        var dummy = Component.empty().style(style);

        var newText = new ArrayList<TextComponent>();

        for (TextComponent component : text) {
            newText.add(dummy.append(component));
        }

        return new Message(
                sound,
                pitch,
                volume,
                newText,
                title == null ? null : dummy.append(title),
                subtitle == null ? null : dummy.append(subtitle),
                fadeIn,
                stay,
                fadeOut,
                presenter
        );
    }

    @Override
    public void present(Audience audience) {
        presenter.presentFromData(audience, text, title, subtitle, fadeIn, stay, fadeOut, sound, volume, pitch);
    }

    public TextComponent squash() {
        return Presenter.squash(text);
    }
/*
    public String getText() {
        return TranslationPack.SERIALIZER.serialize(text);
    }

    @Override
    public String toString() {
        return getText();
    }*/
}
