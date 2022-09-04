package cz.oneblock.core;

import cz.oneblock.core.state.State;
import cz.oneblock.core.state.StateHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public abstract class AbstractDaemon<B extends BootLoader> {

    private final StateHolder state = new StateHolder(State.LOADING);
    protected final Logger logger;
    protected SystemDaemon systemDaemon;
    protected B bootLoader;

    public AbstractDaemon(SystemDaemon systemDaemon) {
        this.systemDaemon = systemDaemon;
        this.bootLoader = (B) systemDaemon.getBootLoader();

        this.logger = LoggerFactory.getLogger(getLongName());
    }

    protected <D extends AbstractDaemon<B>, B extends BootLoader> D obtainDependency(Class<D> daemonClass) {
        return systemDaemon.obtainDependency(this, daemonClass, false);
    }

    protected <D extends AbstractDaemon<B>, B extends BootLoader> D obtainWeakDependency(Class<D> daemonClass) {
        return systemDaemon.obtainDependency(this, daemonClass, true);
    }

    protected <D extends AbstractDaemon<B>, B extends BootLoader> void whenLoaded(Class<D> clazz, Consumer<D> consumer) {
        systemDaemon.whenLoaded(clazz, consumer);
    }

    protected StateHolder getState() {
        return state;
    }

    protected void postLoad() {
    }

    protected void start() {
    }

    protected void postStart() {
    }

    protected void stop() {
    }

    /**
     * <b>This method may be run before {@link #postLoad()}</b>
     */
    protected void unLoad() {
    }

    public String getShortName() {
        return getClass().getSimpleName().replace("Daemon", "");
    }

    public String getLongName() {
        return getClass().getSimpleName();
    }
}
