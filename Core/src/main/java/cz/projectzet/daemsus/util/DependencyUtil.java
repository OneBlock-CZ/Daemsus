package cz.projectzet.daemsus.util;

import com.google.common.graph.Graph;
import cz.projectzet.daemsus.AbstractDaemon;
import cz.projectzet.daemsus.BootLoader;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Most parts are kindly borrowed from the <a href="https://velocitypowered.com/">Velocity project</a>.
 */
@SuppressWarnings("UnstableApiUsage")
public class DependencyUtil {

    public static <B extends BootLoader<B>> Collection<Class<? extends AbstractDaemon<B>>> constructLoadingQueue(Graph<Class<? extends AbstractDaemon<B>>> graph) {
        List<Class<? extends AbstractDaemon<B>>> sorted = new ArrayList<>();
        Map<Class<? extends AbstractDaemon<B>>, Mark> marks = new HashMap<>();

        for (Class<? extends AbstractDaemon<B>> node : graph.nodes()) {
            visitNode(graph, node, marks, sorted, new ArrayDeque<>());
        }

        return sorted;
    }

    private static <B extends BootLoader<B>> void visitNode(Graph<Class<? extends AbstractDaemon<B>>> dependencyGraph, Class<? extends AbstractDaemon<B>> current,
                                                            Map<Class<? extends AbstractDaemon<B>>, Mark> visited, List<Class<? extends AbstractDaemon<B>>> sorted,
                                                            Deque<Class<? extends AbstractDaemon<B>>> currentDependencyScanStack) {
        Mark mark = visited.getOrDefault(current, Mark.NOT_VISITED);
        if (mark == Mark.VISITED) {
            // Visited this node already, nothing to do.
            return;
        } else if (mark == Mark.VISITING) {
            // A circular dependency has been detected. (Specifically, if we are visiting any dependency
            // and a dependency we are looking at depends on any dependency being visited, we have a
            // circular dependency, thus we do not have a directed acyclic graph and therefore no
            // topological sort is possible.)
            currentDependencyScanStack.addLast(current);
            final String loop = currentDependencyScanStack.stream().map(Class::getSimpleName)
                    .collect(Collectors.joining(" -> "));
            throw new IllegalStateException("Circular dependency detected: " + loop);
        }

        // Visiting this node. Mark this node as having a visit in progress and scan its edges.
        currentDependencyScanStack.addLast(current);
        visited.put(current, Mark.VISITING);
        for (Class<? extends AbstractDaemon<B>> edge : dependencyGraph.successors(current)) {
            visitNode(dependencyGraph, edge, visited, sorted, currentDependencyScanStack);
        }

        // All other dependency nodes were visited. We are clear to mark as visited and add to the
        // sorted list.
        visited.put(current, Mark.VISITED);
        currentDependencyScanStack.removeLast();
        sorted.add(current);
    }

    private enum Mark {
        NOT_VISITED,
        VISITING,
        VISITED
    }

}
