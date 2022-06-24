package cz.projectzet.core;

import org.slf4j.Logger;

import java.io.File;
import java.io.InputStream;

public interface BootLoader {

    Logger getSystemLogger();

    File getDataFolder();

    InputStream getResourceAsStream(String resource);

}
