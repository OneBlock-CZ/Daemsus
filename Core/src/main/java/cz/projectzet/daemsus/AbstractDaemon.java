package cz.projectzet.daemsus;

import cz.projectzet.daemsus.state.State;
import cz.projectzet.daemsus.state.StateHolder;
import cz.projectzet.daemsus.util.InjectMeta;

public class AbstractDaemon<B extends BootLoader<B>> {

    private final StateHolder state = new StateHolder(State.LOADING);
    @InjectMeta
    protected SystemDaemon<B> system;
    @InjectMeta
    protected BootLoader<B> bootLoader;

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

    protected void unLoad() {
    }

}
