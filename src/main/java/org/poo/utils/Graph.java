package org.poo.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Graph<T> {

    @FunctionalInterface
    public interface PathComposer {
        /**
         * Calculates the contribution of the second node weight to te current path
         * @param firstWeight the current path weight
         * @param secondWeight the next node's weight
         * @return the new weight from the start node to the next node
         */
        double composePaths(
                double firstWeight,
                double secondWeight
        );
    }

    private final Map<T, List<Pair<T, Double>>> edges =  new HashMap<>();

    /**
     * Adds a weighted edge to the graph
     * @param src the source node
     * @param dest the destination node
     * @param weight the edge's weight
     */
    public void addEdge(
            final T src,
            final T dest,
            final double weight
    ) {
        if (!edges.containsKey(src)) {
            edges.put(src, new ArrayList<>());
        }
        edges.get(src).add(new Pair<>(dest, weight));

        // Also add reverse edge
        if (!edges.containsKey(dest)) {
            edges.put(dest, new ArrayList<>());
        }
        edges.get(dest).add(new Pair<>(src, 1 / weight));
    }

    /**
     * Removes a node from the graph and all its associated edges
     * @param node the node to remove
     */
    public void removeNode(final T node) {
        // Remove edges from the node
        edges.remove(node);

        // Remove edges to the node
        edges.keySet().parallelStream().forEach(key ->
            edges.get(key).removeIf(pair -> pair.first().equals(node))
        );
    }

    /**
     * Computes all best paths from the given node to the rest of the nodes of the graph
     * @param node the start node
     * @param composer the rule to calculate the contribution of a node to the current path
     * @return a map of target nodes and their weights
     */
    private Map<T, Double> getBestPaths(
            final T node,
            final PathComposer composer
    ) {
        Map<T, Double> bestPaths = new HashMap<>();
        bestPaths.put(node, 1.0);
        Set<T> visited = new HashSet<>();
        visited.add(node);

        Queue<Pair<T, Double>> queue = new LinkedList<>(edges.get(node));

        while (!queue.isEmpty()) {
            // Remove node from the queue
            Pair<T, Double> pair = queue.poll();
            if (pair == null) {
                break;
            }

            // If the node was already visited, we skip it
            if (visited.contains(pair.first())) {
                continue;
            }

            // We mark the node as visited
            visited.add(pair.first());

            if (!bestPaths.containsKey(pair.first())) {
                // If the path doesn't exist, we create it
                bestPaths.put(pair.first(), pair.second());
            } else if (pair.second() > bestPaths.get(pair.first())) {
                // If the path exists, update it if it's better
                bestPaths.put(pair.first(), pair.second());
            }

            // Add the node's neighbours with updated path (it's multiplied not added)
            if (edges.containsKey(pair.first())) {
                edges.get(pair.first()).forEach(
                        next -> queue.add(new Pair<>(
                                next.first(),
                                composer.composePaths(pair.second(), next.second())
                        ))
                );
            }

        }

        return bestPaths;
    }

    /**
     * Computes all best paths from every node to every other node in the graph
     * @param composer the rule to calculate the contribution of a node to the current path
     * @return a map of every node pair and their weights
     */
    public Map<Pair<T, T>, Double> computePaths(final PathComposer composer) {
        Map<Pair<T, T>, Double> paths = new ConcurrentHashMap<>();

        edges.keySet().forEach(from ->
            getBestPaths(from, composer).forEach(
                    (key, value) -> paths.put(new Pair<>(from, key), value)
            )
        );

        return paths;
    }

}
