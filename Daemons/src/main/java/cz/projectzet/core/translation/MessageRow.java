package cz.projectzet.core.translation;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.TextComponent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MessageRow implements Presentable {

    private final List<TextComponent> text = new ArrayList<>();
    private String sound;
    private Float pitch, volume;
    private TextComponent title, subtitle;
    private Duration fadeIn, stay, fadeOut;
    private Presenter presenter;

    public List<TextComponent> getText() {
        return text;
    }

    public TextComponent squash() {
        return Presenter.squash(text);
    }

    public MessageRow add(Message message) {
        if (message.sound() != null) {
            sound = message.sound();
            pitch = message.pitch();
            volume = message.volume();
        }

        if (message.text() != null) text.addAll(message.text());

        if (message.title() != null) {
            title = message.title();
            subtitle = message.subtitle();

            fadeIn = message.fadeIn();
            stay = message.stay();
            fadeOut = message.fadeOut();
        }

        if (presenter == null) presenter = message.presenter();

        return this;
    }

    @Override
    public void present(Audience audience) {
        if (presenter == null) return;
        presenter.presentFromData(audience, text, title, subtitle, fadeIn, stay, fadeOut, sound, volume, pitch);
    }
}
