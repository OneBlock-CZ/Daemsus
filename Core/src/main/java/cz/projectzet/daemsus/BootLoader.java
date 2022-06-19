package cz.projectzet.daemsus;

import org.slf4j.Logger;

import java.io.File;
import java.io.InputStream;

public interface BootLoader<B extends BootLoader<B>> {

    SystemDaemon<B> getSystemDaemon();

    Logger getSystemLogger();

    File getDataFolder();

    InputStream getResourceAsStream(String resource);

}
