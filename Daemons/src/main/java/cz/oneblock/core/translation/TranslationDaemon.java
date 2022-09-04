package cz.oneblock.core.translation;

import cz.oneblock.core.AbstractDaemon;
import cz.oneblock.core.BootLoader;
import cz.oneblock.core.ProjectDaemon;
import cz.oneblock.core.SystemDaemon;
import cz.oneblock.core.configuration.ConfigurateSection;
import org.slf4j.Logger;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public abstract class TranslationDaemon extends ProjectDaemon<BootLoader> {

    private final Map<String, TranslationPack> translations;
    private Presenter presenter;
    private String defaultTranslation;
    private File dataFolder;

    public TranslationDaemon(SystemDaemon system) {
        super(system);
        translations = new HashMap<>();
    }

    public String getDefaultTranslation() {
        return defaultTranslation;
    }

    public Presenter getPresenter() {
        return presenter;
    }

    @Override
    public void start() {
        dataFolder = new File(bootLoader.getDataFolder(), "translations");

        if (!dataFolder.exists())
            if (!dataFolder.mkdir()) throw new RuntimeException("Failed to create translations folder");

        presenter = supplyPresenter();

        loadConfiguration(getConfiguration());
    }

    @Override
    public void stop() {

    }

    @Override
    public void loadConfiguration(ConfigurateSection configuration) {
        defaultTranslation = configuration.getString("default_translation");

        var names = Arrays.stream(dataFolder.listFiles())
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toCollection(HashSet::new));

        names.addAll(getIncludedTranslations());

        for (String key : names) {
            var pack = new TranslationPack(key, this, systemDaemon);

            try {
                pack.load();
                this.translations.put(key, pack);
            } catch (Exception e) {
                logger.error("Failed to load/create " + key + " translation");
                throw new RuntimeException(e);
            }

        }

        logger.info("Loaded %s translations".formatted(translations.size()));

    }

    public Message getMessage(AbstractDaemon<?> daemon, String key, String translationID) {
        Message translated = getByID(translationID).getMessage(daemon, key);

        if (translated == null) translated = getByID(defaultTranslation).getMessage(daemon, key);

        return translated;
    }

    public Message getEffectiveMessage(AbstractDaemon<?> daemon, String key, String translationID) {
        var translated = getMessage(daemon, key, translationID);

        if (translated == null) translated = getGeneralMessage(key, translationID);

        return translated;
    }

    public Message getGeneralMessage(String key, String translationID) {
        Message translated = getByID(translationID).getGeneralMessage(key);

        if (translated == null) translated = getByID(defaultTranslation).getGeneralMessage(key);

        return translated;
    }

    @Override
    public boolean useConfiguration() {
        return true;
    }

    @Override
    public boolean allowReload() {
        return true;
    }

    private TranslationPack getByID(String id) {
        return translations.get(id);
    }

    protected abstract Presenter supplyPresenter();

    protected abstract Collection<String> getIncludedTranslations();

    protected Logger getLogger() {
        return logger;
    }

    protected BootLoader getBootLoader() {
        return bootLoader;
    }
}
