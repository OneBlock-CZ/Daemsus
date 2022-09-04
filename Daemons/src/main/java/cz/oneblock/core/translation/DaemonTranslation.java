package cz.oneblock.core.translation;

import cz.oneblock.core.configuration.ConfigurateSection;
import cz.oneblock.core.configuration.CorruptedConfigurationException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static cz.oneblock.core.translation.Presenter.squash;
import static cz.oneblock.core.util.CoreUtil.getOrDefault;
import static xyz.kyngs.utils.legacymessage.LegacyMessage.fromLegacy;

public class DaemonTranslation {

    public static final MiniMessage SERIALIZER = MiniMessage.builder()
            .build();
    public static final TextComponent RESET = Component.text("", Style.style().decoration(TextDecoration.ITALIC, false).build());
    public static final Message NEW_LINE = new Message(null, null, null, List.of(Component.newline()), null, null, null, null, null, null);

    public static final Duration DEFAULT_FADE_IN = Duration.of(1, ChronoUnit.SECONDS);
    public static final Duration DEFAULT_FADE_OUT = Duration.of(1, ChronoUnit.SECONDS);
    public static final Duration DEFAULT_STAY = Duration.of(5, ChronoUnit.SECONDS);

    private final File file;
    private final TranslationPack parent;
    private final HoconConfigurationLoader loader;
    private final CommentedConfigurationNode reference;
    private final Map<String, Message> messages;

    public DaemonTranslation(File file, TranslationPack parent) throws IOException {
        this.file = file;
        this.parent = parent;

        messages = new HashMap<>();

        if (!file.exists()) {
            if (!file.createNewFile()) throw new IOException("Failed to create file " + file.getAbsolutePath());
        }

        loader = HoconConfigurationLoader.builder()
                .file(file)
                .emitComments(true)
                .prettyPrinting(true)
                .build();

        var referenceStream = parent.getSystem().getBootLoader().getResourceAsStream("translations/" + parent.getId() + "/" + file.getName());

        if (referenceStream != null) {
            var referenceLoader = HoconConfigurationLoader.builder()
                    .emitComments(true)
                    .prettyPrinting(true)
                    .source(() -> new BufferedReader(new InputStreamReader(referenceStream)))
                    .build();

            reference = referenceLoader.load();
        } else reference = CommentedConfigurationNode.root();

    }

    private static Duration nullableDuration(Integer amount, TemporalUnit unit) {
        return amount == null ? null : Duration.of(amount, unit);
    }

    private static List<TextComponent> fromString(@Nullable String string) {
        if (string == null) return null;

        var split = string.split("\\\\n");
        var list = new ArrayList<TextComponent>();

        for (String s : split) {
            list.add(RESET.append(SERIALIZER.deserialize(fromLegacy(s.replace('&', '§')))));
        }

        return list;
    }

    private static List<TextComponent> fromList(List<String> list) {
        var componentList = new ArrayList<TextComponent>();

        for (String s : list) {
            componentList.add(RESET.append(SERIALIZER.deserialize(fromLegacy(s.replace('&', '§')))));
        }

        return componentList;
    }

    public void load() throws CorruptedConfigurationException, IOException {
        ConfigurateSection conf;

        try {
            var loaded = loader
                    .load()
                    .mergeFrom(reference);

            conf = new ConfigurateSection(loaded);
        } catch (ConfigurateException e) {
            throw new CorruptedConfigurationException(e);
        }

        loader.save(conf.configuration());

        Function<String, String> replace = string -> {
            for (var placeholder : parent.getPlaceholders().entrySet()) {
                string = string.replace(placeholder.getKey(), placeholder.getValue());
            }

            return string;
        };

        var presenter = parent.getTranslationDaemon().getPresenter();

        for (String key : conf.getKeys()) {
            var value = conf.getObject(key);

            try {
                Message message;

                if (value instanceof String string) {
                    message = new Message(null, null, null, fromString(replace.apply(string)), null, null, null, null, null, presenter);
                } else if (value instanceof List<?> list) {
                    //noinspection unchecked
                    var stringList = (List<String>) list;

                    stringList.replaceAll(replace::apply);

                    message = new Message(null, null, null, fromList(stringList), null, null, null, null, null, presenter);
                } else if (value instanceof ConfigurateSection messageSection) {
                    var soundSection = messageSection.getObject("sound");

                    String sound = null;
                    float pitch, volume;
                    List<TextComponent> text;
                    TextComponent title = null;
                    TextComponent subtitle = null;
                    Duration fadeIn = null;
                    Duration stay = null;
                    Duration fadeOut = null;

                    if (soundSection instanceof String soundName) {
                        sound = soundName;
                        pitch = 1F;
                        volume = 1F;
                    } else if (soundSection instanceof ConfigurateSection section) {
                        var name = section.getString("name");

                        if (name == null) throw new InvalidMessageException();

                        pitch = getOrDefault(section.getFloat("pitch"), 1F);
                        volume = getOrDefault(section.getFloat("volume"), 1F);
                    } else {
                        throw new InvalidMessageException();
                    }

                    var textString = messageSection.getObject("text");

                    if (textString instanceof String string) {
                        text = fromString(replace.apply(string));
                    } else if (textString instanceof List<?> list) {
                        @SuppressWarnings("unchecked") var stringList = (List<String>) list;

                        stringList.replaceAll(replace::apply);

                        text = fromList(stringList);
                    } else {
                        throw new InvalidMessageException();
                    }

                    var titleSection = messageSection.getObject("title");

                    if (titleSection instanceof String titleText) {
                        title = squash(fromString(titleText));
                        subtitle = Component.empty();
                        fadeIn = DEFAULT_FADE_IN;
                        fadeOut = DEFAULT_FADE_OUT;
                        stay = DEFAULT_STAY;
                    } else if (titleSection instanceof ConfigurateSection section) {
                        var titleText = section.getString("text");

                        if (titleText == null) throw new InvalidMessageException();

                        title = squash(fromString(titleText));
                        subtitle = getOrDefault(squash(fromString(section.getString("sub"))), Component.empty());

                        fadeIn = getOrDefault(nullableDuration(section.getInteger("in"), ChronoUnit.SECONDS), DEFAULT_FADE_IN);
                        fadeOut = getOrDefault(nullableDuration(section.getInteger("out"), ChronoUnit.SECONDS), DEFAULT_FADE_OUT);
                        stay = getOrDefault(nullableDuration(section.getInteger("stay"), ChronoUnit.SECONDS), DEFAULT_STAY);
                    }

                    message = new Message(sound, pitch, volume, text, title, subtitle, fadeIn, stay, fadeOut, presenter);

                } else {
                    throw new InvalidMessageException();
                }

                messages.put(key, message);

            } catch (InvalidMessageException | NullPointerException | IllegalArgumentException | ClassCastException e) {
                e.printStackTrace();
                parent.getTranslationDaemon().getLogger().warn("Invalid value for key %s: %s".formatted(key, value));
            }
        }

    }

    public Message get(String key) {
        return messages.get(key.replace('_', '-'));
    }

}
