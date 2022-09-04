package cz.oneblock.core.configuration;

public class CorruptedConfigurationException extends Exception {
    public CorruptedConfigurationException(Throwable cause) {
        super(cause);
    }

    public CorruptedConfigurationException(String message) {
        super(message);
    }
}
