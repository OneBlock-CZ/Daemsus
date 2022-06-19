package cz.projectzet.daemsus;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import cz.projectzet.daemsus.state.StateHolder;
import cz.projectzet.daemsus.util.DependencyUtil;
import cz.projectzet.daemsus.util.InjectDaemon;
import cz.projectzet.daemsus.util.InjectMeta;
import cz.projectzet.daemsus.util.ReflectionUtil;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Predicate;

import static cz.projectzet.daemsus.state.State.*;

@SuppressWarnings({"UnstableApiUsage", "unchecked"})
public class SystemDaemon<B extends BootLoader<B>> {

    private final B bootLoader;
    private final StateHolder state;
    private final MutableGraph<Class<? extends AbstractDaemon<B>>> registeredDaemons;
    private final Map<Class<AbstractDaemon<B>>, AbstractDaemon<B>> loadedDaemons;
    private final List<AbstractDaemon<B>> startedDaemons;
    private final Predicate<Class<? extends AbstractDaemon<B>>> daemonFilter;
    private Logger logger;

    public SystemDaemon(B bootLoader, Predicate<Class<? extends AbstractDaemon<B>>> daemonFilter) {
        this.bootLoader = bootLoader;
        this.daemonFilter = daemonFilter;

        this.registeredDaemons = GraphBuilder
                .directed()
                .allowsSelfLoops(false)
                .build();
        this.loadedDaemons = new LinkedHashMap<>();
        this.startedDaemons = new ArrayList<>();

        this.state = new StateHolder(INITIALIZED);
    }

    public B getBootLoader() {
        return bootLoader;
    }

    public void register(Class<? extends AbstractDaemon<B>> daemonClass) {
        if (!daemonFilter.test(daemonClass)) {
            return;
        }
        state.requireStates(() -> {
            registeredDaemons.addNode(daemonClass);

            var dependencies = ReflectionUtil.getDependencies(daemonClass);

            for (var dependency : dependencies) {
                registeredDaemons.putEdge(daemonClass, dependency);
            }

        }, INITIALIZED);
    }

    public void load() {
        this.logger = bootLoader.getSystemLogger();

        state.setStateOrThrow(LOADING, INITIALIZED);

        for (Class<? extends AbstractDaemon<B>> node : registeredDaemons.nodes()) {
            var conflicts = ReflectionUtil.getConflictingDaemons(node);

            for (var conflict : conflicts) {
                if (registeredDaemons.nodes().contains(conflict)) {
                    logger.error("Daemon {} conflicts with {}", node.getName(), conflict.getName());
                    panic();
                }
            }
        }

        for (Class<? extends AbstractDaemon<B>> node : registeredDaemons.nodes()) {
            registeredDaemons.successors(node).forEach(dependency -> {
                if (!registeredDaemons.nodes().contains(dependency)) {
                    logger.error("Daemon {} depends on {}, however, it is not available", node.getName(), dependency.getName());
                    panic();
                }
            });
        }

        var order = DependencyUtil.constructLoadingQueue(registeredDaemons);

        order.forEach(this::loadDaemon);

        state.setStateOrThrow(POST_LOADING, LOADING);

        loadedDaemons.values().forEach(this::postLoadDaemon);

        state.setStateOrThrow(LOADED, POST_LOADING);
    }

    private void loadDaemon(Class<? extends AbstractDaemon<B>> clazz) {
        try {
            AbstractDaemon<B> instance;
            try {
                Constructor<? extends AbstractDaemon<B>> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);

                instance = constructor.newInstance();
            } catch (NoSuchMethodException e) {
                var unsafe = ReflectionUtil.UNSAFE;

                if (unsafe == null) {
                    throw new IllegalStateException("Unsafe is not available and an no-arg constructor is not available for class " + clazz.getName());
                }
                instance = (AbstractDaemon<B>) unsafe.allocateInstance(clazz);
            }

            Class<?> toInject = clazz;

            while (toInject != Object.class) {
                var dependencies = ReflectionUtil.getDependencies(clazz).stream()
                        .map(loadedDaemons::get)
                        .toArray(AbstractDaemon[]::new);

                ReflectionUtil.inject(InjectMeta.class, toInject, instance,
                        bootLoader, this
                );

                ReflectionUtil.inject(InjectDaemon.class, toInject, instance, (Object[]) dependencies);

                toInject = toInject.getSuperclass();
            }

            loadedDaemons.put((Class<AbstractDaemon<B>>) clazz, instance);
            instance.getState().setStateOrThrow(POST_LOADING, LOADING);
        } catch (Exception e) {
            reactToDaemonException(e, clazz.getSimpleName(), "Exception while loading daemon {}");
        }
    }

    private void postLoadDaemon(AbstractDaemon<B> daemon) {
        daemon.getState().requireStatesOrThrow(POST_LOADING);
        try {
            daemon.postLoad();
        } catch (Exception e) {
            reactToDaemonException(e, daemon.getClass().getSimpleName(), "Exception while post-loading daemon {}");
        }
        daemon.getState().setStateOrThrow(LOADED, POST_LOADING);
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
        state.setStateOrThrow(PANICKING, LOADING, POST_LOADING, STARTING, POST_STARTING);
        logger.error("PANIC - PANIC - PANIC");
        logger.info("Stopping all daemons");
        startedDaemons.forEach(this::stopDaemon);
        logger.info("All daemons stopped");
        logger.info("Unloading all daemons");
        loadedDaemons.values().forEach(this::unLoadDaemon);
        logger.info("All daemons unloaded");
        logger.error("PANIC - PANIC - PANIC");
        System.exit(1);
    }

    public void unLoad() {
        state.setStateOrThrow(UNLOADING, LOADED);
        var reversed = new ArrayList<>(loadedDaemons.values());

        Collections.reverse(reversed);

        reversed.forEach(this::unLoadDaemon);
        state.setStateOrThrow(UNLOADED, UNLOADING);
    }

    private void unLoadDaemon(AbstractDaemon<B> daemon) {
        daemon.getState().setStateOrThrow(UNLOADING, LOADED);
        try {
            daemon.unLoad();
        } catch (Exception e) {
            reactToDaemonException(e, daemon.getClass().getSimpleName(), "Exception while unloading daemon {}, ignoring", false);
        }
        daemon.getState().setStateOrThrow(UNLOADED, UNLOADING);
    }

    public void stop() {
        state.setStateOrThrow(STOPPING, STARTED);

        var reversed = new ArrayList<>(startedDaemons);

        Collections.reverse(reversed);

        reversed.forEach(this::stopDaemon);

        state.setStateOrThrow(LOADED, STOPPING);
    }

    private void stopDaemon(AbstractDaemon<B> daemon) {
        daemon.getState().setStateOrThrow(STOPPING, STARTED);
        try {
            daemon.stop();
        } catch (Exception e) {
            reactToDaemonException(e, daemon.getClass().getSimpleName(), "Exception while stopping daemon {}, ignoring", false);
        }
        daemon.getState().setStateOrThrow(LOADED, STOPPING);
    }

    public void start() {
        state.setStateOrThrow(STARTING, LOADED);

        loadedDaemons.values().forEach(this::startDaemon);

        state.setStateOrThrow(POST_STARTING, STARTING);

        loadedDaemons.values().forEach(this::postStartDaemon);

        state.setStateOrThrow(STARTED, POST_STARTING);
    }

    private void postStartDaemon(AbstractDaemon<B> daemon) {
        daemon.getState().requireStatesOrThrow(POST_STARTING);
        try {
            daemon.postStart();
        } catch (Exception e) {
            reactToDaemonException(e, daemon.getClass().getSimpleName(), "Exception while post-starting daemon {}");
        }
        daemon.getState().setStateOrThrow(STARTED, POST_STARTING);
    }

    private void startDaemon(AbstractDaemon<B> daemon) {
        daemon.getState().setStateOrThrow(STARTING, LOADED);
        try {
            daemon.start();
            startedDaemons.add(daemon);
        } catch (Exception e) {
            reactToDaemonException(e, daemon.getClass().getSimpleName(), "Exception while starting daemon {}");
        }
        daemon.getState().setStateOrThrow(POST_STARTING, STARTING);
    }

}
