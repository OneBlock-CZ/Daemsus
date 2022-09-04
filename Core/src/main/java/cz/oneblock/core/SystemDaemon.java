package cz.oneblock.core;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import cz.oneblock.core.state.State;
import cz.oneblock.core.state.StateHolder;
import cz.oneblock.core.util.NeedsConfigurationException;
import cz.oneblock.core.util.ReflectionUtil;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings({"UnstableApiUsage", "unchecked"})
public class SystemDaemon {

    private final BootLoader bootLoader;
    private final StateHolder state;
    private final Set<Class<? extends AbstractDaemon<?>>> registeredDaemons;
    private final Map<Class<AbstractDaemon<?>>, AbstractDaemon<?>> loadedDaemons;
    private final MutableGraph<Class<? extends AbstractDaemon<?>>> daemonDependencyGraph;
    private final List<AbstractDaemon<?>> startedDaemons;
    private final Predicate<Class<? extends AbstractDaemon<?>>> daemonFilter;
    private final Multimap<Class<? extends AbstractDaemon<?>>, Consumer<AbstractDaemon<?>>> whenLoaded;
    private final Set<AbstractDaemon<?>> needsConfiguration;
    private final AbstractDaemon<?> dummyDaemon;
    private Logger logger;

    public SystemDaemon(BootLoader bootLoader, Predicate<Class<? extends AbstractDaemon<?>>> daemonFilter) {
        this.bootLoader = bootLoader;
        this.daemonFilter = daemonFilter;
        this.dummyDaemon = new AbstractDaemon<>(this) {
        };

        this.registeredDaemons = new HashSet<>();
        this.loadedDaemons = new LinkedHashMap<>();
        this.startedDaemons = new ArrayList<>();
        this.whenLoaded = HashMultimap.create();
        this.daemonDependencyGraph = GraphBuilder.directed()
                .allowsSelfLoops(false)
                .build();
        this.needsConfiguration = new HashSet<>();

        this.state = new StateHolder(State.INITIALIZED);
    }

    public void addDaemonInNeedOfConfiguration(AbstractDaemon<?> daemon) {
        needsConfiguration.add(daemon);
    }

    public Map<Class<AbstractDaemon<?>>, AbstractDaemon<?>> getLoadedDaemons() {
        return loadedDaemons;
    }

    public BootLoader getBootLoader() {
        return bootLoader;
    }

    public void registerDaemon(Class<? extends AbstractDaemon<?>> daemonClass) {
        if (!daemonFilter.test(daemonClass)) {
            return;
        }
        state.requireStates(() -> {
            registeredDaemons.add(daemonClass);
            daemonDependencyGraph.addNode(daemonClass);
        }, State.INITIALIZED);
    }

    public void load() {
        this.logger = bootLoader.getSystemLogger();

        state.setStateOrThrow(State.LOADING, State.INITIALIZED);

        for (Class<? extends AbstractDaemon<?>> daemon : registeredDaemons) {
            var conflicts = ReflectionUtil.getConflictingDaemons(daemon);

            for (var conflict : conflicts) {
                if (registeredDaemons.contains(conflict)) {
                    logger.error("Daemon {} conflicts with {}", daemon.getName(), conflict.getName());
                    panic();
                }
            }
        }

        var clone = new HashSet<>(registeredDaemons);

        clone.forEach(this::loadDaemon);

        needsConfiguration.forEach(daemon -> logger.warn("Daemon {} needs configuration", daemon.getShortName()));

        if (!needsConfiguration.isEmpty()) {
            logger.warn("Some daemons need to be configured. Please configure them.");
            panic();
        }

        state.setStateOrThrow(State.POST_LOADING, State.LOADING);

        loadedDaemons.values().forEach(this::postLoadDaemon);

        state.setStateOrThrow(State.LOADED, State.POST_LOADING);
    }

    private <D extends AbstractDaemon<?>> D loadDaemon(Class<D> clazz) {
        var loaded = loadedDaemons.get(clazz);

        if (loaded != null) {
            if (loaded == dummyDaemon) {
                throw new NeedsConfigurationException();
            }
            return (D) loaded;
        }

        try {
            D instance;
            try {
                Constructor<D> constructor = clazz.getDeclaredConstructor(getClass());

                constructor.setAccessible(true);

                instance = constructor.newInstance(this);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Daemon class " + clazz.getName() + " does not have a constructor with one argument of type " + getClass().getName());
            }

            instance.getState().setStateOrThrow(State.POST_LOADING, State.LOADING);
            loadedDaemons.put((Class<AbstractDaemon<?>>) clazz, instance);
            whenLoaded.get(clazz).forEach(consumer -> consumer.accept(instance));
            return instance;
        } catch (Exception e) {
            if (e instanceof InvocationTargetException inv) {
                if (inv.getCause() instanceof NeedsConfigurationException) {
                    loadedDaemons.put((Class<AbstractDaemon<?>>) clazz, dummyDaemon);
                    return null;
                }
            }
            reactToDaemonException(e, clazz.getSimpleName(), "Exception while loading daemon {}");
            return null;
        }
    }

    private void postLoadDaemon(AbstractDaemon<?> daemon) {
        daemon.getState().requireStatesOrThrow(State.POST_LOADING);
        try {
            daemon.postLoad();
        } catch (Exception e) {
            reactToDaemonException(e, daemon.getClass().getSimpleName(), "Exception while post-loading daemon {}");
        }
        daemon.getState().setStateOrThrow(State.LOADED, State.POST_LOADING);
    }

    private void reactToDaemonException(Throwable e, String daemonName, String message) {
        reactToDaemonException(e, daemonName, message, true);
    }

    private void reactToDaemonException(Throwable e, String daemonName, String message, boolean panic) {
        logger.error(message, daemonName, e);
        e.printStackTrace();
        if (panic) panic();
    }

    /**
     * This method never returns
     */
    private void panic() {
        state.setStateOrThrow(State.PANICKING, State.LOADING, State.POST_LOADING, State.STARTING, State.POST_STARTING);
        logger.error("PANIC - PANIC - PANIC");
        logger.info("Stopping all daemons");
        var reversed = new ArrayList<>(startedDaemons);

        Collections.reverse(reversed);

        reversed.forEach(this::stopDaemon);
        logger.info("All daemons stopped");
        logger.info("Unloading all daemons");
        var reversed2 = new ArrayList<>(loadedDaemons.values());

        Collections.reverse(reversed2);

        reversed2.forEach(this::unLoadDaemon);
        logger.info("All daemons unloaded");
        logger.error("PANIC - PANIC - PANIC");

        try {
            Thread.sleep(2500);
        } catch (InterruptedException ignored) {
        }

        System.exit(1);
    }

    public void unLoad() {
        state.setStateOrThrow(State.UNLOADING, State.LOADED);
        var reversed = new ArrayList<>(loadedDaemons.values());

        Collections.reverse(reversed);

        reversed.forEach(this::unLoadDaemon);
        state.setStateOrThrow(State.UNLOADED, State.UNLOADING);
    }

    private void unLoadDaemon(AbstractDaemon<?> daemon) {
        daemon.getState().setStateOrThrow(State.UNLOADING, State.LOADED, State.POST_LOADING);
        try {
            daemon.unLoad();
        } catch (Exception e) {
            reactToDaemonException(e, daemon.getClass().getSimpleName(), "Exception while unloading daemon {}, ignoring", false);
        }
        daemon.getState().setStateOrThrow(State.UNLOADED, State.UNLOADING);
    }

    public void stop() {
        state.setStateOrThrow(State.STOPPING, State.STARTED);

        var reversed = new ArrayList<>(startedDaemons);

        Collections.reverse(reversed);

        reversed.forEach(this::stopDaemon);

        state.setStateOrThrow(State.LOADED, State.STOPPING);
    }

    private void stopDaemon(AbstractDaemon<?> daemon) {
        daemon.getState().setStateOrThrow(State.STOPPING, State.STARTED, State.POST_STARTING);
        try {
            daemon.stop();
        } catch (Exception e) {
            reactToDaemonException(e, daemon.getClass().getSimpleName(), "Exception while stopping daemon {}, ignoring", false);
        }
        daemon.getState().setStateOrThrow(State.LOADED, State.STOPPING);
    }

    public void start() {
        state.setStateOrThrow(State.STARTING, State.LOADED);

        loadedDaemons.values().forEach(this::startDaemon);

        state.setStateOrThrow(State.POST_STARTING, State.STARTING);

        loadedDaemons.values().forEach(this::postStartDaemon);

        state.setStateOrThrow(State.STARTED, State.POST_STARTING);
    }

    private void postStartDaemon(AbstractDaemon<?> daemon) {
        daemon.getState().requireStatesOrThrow(State.POST_STARTING);
        try {
            daemon.postStart();
        } catch (Exception e) {
            reactToDaemonException(e, daemon.getClass().getSimpleName(), "Exception while post-starting daemon {}");
        }
        daemon.getState().setStateOrThrow(State.STARTED, State.POST_STARTING);
    }

    private void startDaemon(AbstractDaemon<?> daemon) {
        daemon.getState().setStateOrThrow(State.STARTING, State.LOADED);
        try {
            daemon.start();
            startedDaemons.add(daemon);
        } catch (Exception e) {
            reactToDaemonException(e, daemon.getClass().getSimpleName(), "Exception while starting daemon {}");
        }
        daemon.getState().setStateOrThrow(State.POST_STARTING, State.STARTING);
    }

    protected <B extends BootLoader, D extends AbstractDaemon<B>> D obtainDependency(AbstractDaemon<?> caller, Class<D> clazz, boolean nullOnCycle) {
        if (caller.getClass().isAssignableFrom(clazz)) {
            return (D) caller;
        }

        daemonDependencyGraph.putEdge((Class<? extends AbstractDaemon<?>>) caller.getClass(), clazz);
        if (Graphs.hasCycle(daemonDependencyGraph)) {
            if (nullOnCycle) {
                daemonDependencyGraph.removeEdge((Class<? extends AbstractDaemon<?>>) caller.getClass(), clazz);
                return null;
            }

            throw new IllegalStateException("Detected a cyclic dependency between daemons %s and %s".formatted(caller.getClass().getSimpleName(), clazz.getSimpleName()));
        }

        registeredDaemons.add(clazz);

        var result = loadDaemon(clazz);

        if (result == null) {
            throw new NeedsConfigurationException();
        }

        return result;
    }

    public <B extends BootLoader, D extends AbstractDaemon<B>> void whenLoaded(Class<D> clazz, Consumer<D> consumer) {
        var loaded = loadedDaemons.get(clazz);

        if (loaded != null) {
            consumer.accept((D) loaded);
        } else {
            whenLoaded.put(clazz, k -> consumer.accept((D) k));
        }
    }

}
