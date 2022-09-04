package cz.oneblock.core.configuration;

import cz.oneblock.core.BootLoader;
import cz.oneblock.core.ProjectDaemon;
import cz.oneblock.core.SystemDaemon;
import cz.oneblock.core.util.NeedsConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationDaemon extends ProjectDaemon<BootLoader> {

    private final Map<ProjectDaemon<?>, ConfigurateConfiguration> configurations;
    private final File data;

    private ConfigurationDaemon(SystemDaemon system) {
        super(system);
        configurations = new HashMap<>();

        data = new File(bootLoader.getDataFolder(), "configuration");

        if (data.exists() && !data.isDirectory()) {
            if (!data.delete()) throw new RuntimeException("Failed to delete file: " + data.getAbsolutePath());
        } else if (!data.exists()) {
            if (!data.mkdirs()) throw new RuntimeException("Failed to create directory: " + data.getAbsolutePath());
        }
    }

    public ConfigurateConfiguration getConfiguration(ProjectDaemon<?> daemon) {
        return configurations.computeIfAbsent(daemon, x -> {
            var config = new ConfigurateConfiguration(
                    data,
                    daemon.getShortName().toLowerCase() + ".conf",
                    bootLoader
            );

            try {
                if (config.load(daemon.repairConfiguration())) {
                    systemDaemon.addDaemonInNeedOfConfiguration(daemon);
                    throw new NeedsConfigurationException();
                }
            } catch (CorruptedConfigurationException | IOException e) {
                throw new RuntimeException(e);
            }

            return config;
        });
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
