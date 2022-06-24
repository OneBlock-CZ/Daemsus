package cz.projectzet.core;

import cz.projectzet.core.configuration.ConfigurateConfiguration;
import cz.projectzet.core.configuration.ConfigurateSection;
import cz.projectzet.core.configuration.ConfigurationDaemon;
import cz.projectzet.core.configuration.CorruptedConfigurationException;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public abstract class ProjectDaemon<B extends BootLoader> extends AbstractDaemon<B> {

    protected final Lock configurationLock;
    private final ConfigurateConfiguration daemonConfiguration;

    public ProjectDaemon(SystemDaemon system) {
        super(system);

        if (useConfiguration()) {
            daemonConfiguration = obtainDependency(ConfigurationDaemon.class).getConfiguration(this);
            configurationLock = new ReentrantLock();
        } else {
            daemonConfiguration = null;
            configurationLock = null;
        }
    }

    protected ConfigurateSection getConfiguration() {
        return daemonConfiguration.getConfiguration();
    }

    protected void loadConfiguration(ConfigurateSection configuration) {
    }

    protected void configurationAction(Consumer<ConfigurateSection> editor) {
        configurationLock.lock();
        try {
            editor.accept(getConfiguration());
            daemonConfiguration.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            configurationLock.unlock();
        }
    }

    public final void reloadConfiguration() {
        if (allowReload()) {
            configurationLock.lock();
            try {
                daemonConfiguration.load(repairConfiguration());
                loadConfiguration(getConfiguration());
            } catch (CorruptedConfigurationException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                configurationLock.unlock();
            }
        } else {
            throw new IllegalStateException("Reloading configuration is not allowed");
        }
    }

    public boolean useConfiguration() {
        return false;
    }

    public boolean repairConfiguration() {
        return true;
    }

    public boolean allowReload() {
        return false;
    }

    public boolean useLanguage() {
        return false;
    }
}
