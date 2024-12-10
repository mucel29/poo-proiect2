package org.poo.system.exchange;

import java.util.*;

public class Graph <T> {

    private final Map<T, List<Pair<T, Double>>> edges =  new HashMap<>();

    public void addEdge(T src, T dest, double weight) {
        if (!edges.containsKey(src)) {
            edges.put(src, new ArrayList<>());
        }
        edges.get(src).add(new Pair<>(dest, weight));
    }

    public void removeNode(T node) {
        // Remove edges from the node
        edges.remove(node);

        // Remove edges to the node
        edges.keySet().parallelStream().forEach(key -> {
            edges.get(key).removeIf(pair -> pair.getFirst().equals(node));
        });
    }

    private Map<T, Double> getBestPaths(T node) {
        Map<T, Double> bestPaths = new HashMap<>();
        bestPaths.put(node, 1.0);
        Set<T> visited = new HashSet<>();
        visited.add(node);

        Queue<Pair<T, Double>> queue = new LinkedList<>();

        edges.get(node).parallelStream().forEach(queue::add);

        while (!queue.isEmpty()) {
            // Remove node from the queue
            Pair<T, Double> pair = queue.remove();

            // If the node was already visited, we skip it
            if (visited.contains(pair.getFirst())) {
                continue;
            }

            // We mark the node as visited
            visited.add(pair.getFirst());

            if (!bestPaths.containsKey(pair.getFirst())) {
                // If the path doesn't exist, we create it
                bestPaths.put(pair.getFirst(), pair.getSecond());
            } else if (pair.getSecond() > bestPaths.get(pair.getFirst())) {
                // If the path exists, update it if it's better
                bestPaths.put(pair.getFirst(), pair.getSecond());
            }

            // Add the node's neighbours with updated path (it's multiplied not added, weights are 0-1)
            if (edges.containsKey(pair.getFirst())) {
                edges.get(pair.getFirst()).parallelStream().forEach(next -> queue.add(new Pair<>(next.getFirst(), pair.getSecond() * next.getSecond())));
            }

        }

        return bestPaths;
    }

    public Map<Pair<T, T>, Double> computePaths() {
        Map<Pair<T, T>, Double> paths = new HashMap<>();

        edges.keySet().parallelStream().forEach(from -> {
            getBestPaths(from).entrySet().parallelStream().forEach(path -> {
                paths.put(new Pair<>(from, path.getKey()), path.getValue());
            });
        });

        return paths;
    }

}
