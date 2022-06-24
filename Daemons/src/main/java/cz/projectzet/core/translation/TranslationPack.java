package cz.projectzet.core.translation;

import cz.projectzet.core.AbstractDaemon;
import cz.projectzet.core.ProjectDaemon;
import cz.projectzet.core.SystemDaemon;
import cz.projectzet.core.configuration.ConfigurateConfiguration;
import cz.projectzet.core.configuration.CorruptedConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TranslationPack {

    private final String id;
    private final TranslationDaemon translationDaemon;
    private final SystemDaemon system;
    private final Map<AbstractDaemon<?>, DaemonTranslation> translations;
    private Map<String, String> placeholders;
    private DaemonTranslation general;
    private ConfigurateConfiguration configuration;
    private Translation type;

    public TranslationPack(String id, TranslationDaemon translationDaemon, SystemDaemon system) {
        this.id = id;
        this.translationDaemon = translationDaemon;

        this.system = system;

        translations = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    protected TranslationDaemon getTranslationDaemon() {
        return translationDaemon;
    }

    protected SystemDaemon getSystem() {
        return system;
    }

    public Translation getType() {
        return type;
    }

    public void load() throws IOException, CorruptedConfigurationException {
        var folder = new File(system.getBootLoader().getDataFolder(), "translations/" + id);

        if (!folder.exists()) if (!folder.mkdir()) throw new RuntimeException("Cannot create translation folder");

        configuration = new ConfigurateConfiguration(new File(system.getBootLoader().getDataFolder(), "translations/" + id), id + ".conf", "translations/" + id + "/", system.getBootLoader());

        configuration.load(true);

        var conf = configuration.getConfiguration();

        type = new Translation(
                conf.getString("local"),
                conf.getString("english"),
                id
        );

        var placeholders = new HashMap<String, String>();
        var placeholderSection = conf.getSection("placeholders");

        if (placeholderSection != null) {
            for (String key : placeholderSection.getKeys()) {
                placeholders.put(";;" + key + ";;", placeholderSection.getString(key));
            }
        }

        this.placeholders = placeholders;

        general = new DaemonTranslation(new File(system.getBootLoader().getDataFolder(), "translations/" + id + "/general.conf"), this);

        general.load();

        for (ProjectDaemon<?> daemon : system.getLoadedDaemons().values().stream()
                .filter(abstractDaemon -> abstractDaemon instanceof ProjectDaemon<?>)
                .map(abstractDaemon -> (ProjectDaemon<?>) abstractDaemon)
                .toList()
        ) {
            if (!daemon.useLanguage()) continue;

            var name = daemon.getShortName().toLowerCase();

            if (name.equals("general") || name.equals(id)) continue;

            var translation = new DaemonTranslation(new File(system.getBootLoader().getDataFolder(), "translations/" + id + "/" + daemon.getShortName().toLowerCase() + ".conf"), this);

            translation.load();

            translations.put(daemon, translation);
        }
    }

    protected Map<String, String> getPlaceholders() {
        return placeholders;
    }

    public Message getMessage(AbstractDaemon<?> daemon, String key) {
        return translations.get(daemon).get(key);
    }

    public Message getGeneralMessage(String key) {
        return general.get(key);
    }
}
