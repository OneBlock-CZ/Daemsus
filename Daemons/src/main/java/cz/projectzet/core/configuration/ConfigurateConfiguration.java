package cz.projectzet.core.configuration;

import cz.projectzet.core.BootLoader;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConfigurateConfiguration {

    private final CommentedConfigurationNode reference;
    private final HoconConfigurationLoader loader;
    private final File file;
    private ConfigurateSection configuration;

    public ConfigurateConfiguration(File dataFolder, String name, BootLoader bootLoader) {
        this(dataFolder, name, "configuration/", bootLoader);
    }

    public ConfigurateConfiguration(File dataFolder, String name, String defaultResourceFolder, BootLoader bootLoader) {
        file = new File(dataFolder, name);

        loader = HoconConfigurationLoader.builder()
                .file(file)
                .emitComments(true)
                .prettyPrinting(true)
                .emitJsonCompatible(false)
                .build();

        var referenceLoader = HoconConfigurationLoader.builder()
                .emitComments(true)
                .prettyPrinting(true)
                .source(() -> new BufferedReader(new InputStreamReader(bootLoader.getResourceAsStream(defaultResourceFolder + name))))
                .emitJsonCompatible(false)
                .build();

        try {
            reference = referenceLoader.load();
        } catch (ConfigurateException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean load(boolean repair) throws CorruptedConfigurationException, IOException {
        var newlyCreated = false;

        if (!file.exists()) {
            newlyCreated = true;
            if (!file.createNewFile()) throw new IOException("Could not create configuration file!");
        }

        try {
            if (!repair || newlyCreated) {
                configuration = new ConfigurateSection(loader.load()
                        .mergeFrom(reference));
            } else {
                configuration = new ConfigurateSection(loader.load());
            }

        } catch (ConfigurateException e) {
            throw new CorruptedConfigurationException(e);
        }

        save();

        return newlyCreated;
    }

    public void save() throws IOException {
        loader.save(configuration.configuration());
    }

    public ConfigurateSection getConfiguration() {
        return configuration;
    }
}
