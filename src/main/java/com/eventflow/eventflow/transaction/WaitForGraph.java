package com.eventflow.eventflow.transaction;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WaitForGraph {
    private final Map<Long, Set<Long>> graph = new HashMap<>();

    /**
     * Adds a dependency between two transactions
     * waiting transaction -> holding transaction
     */
    public synchronized void addDependency(long waitingTransaction, long holdingTransaction) {
        if (waitingTransaction == holdingTransaction) {
            return;
        }

        graph.computeIfAbsent(waitingTransaction, key -> new HashSet<>()).add(holdingTransaction);

        //Ensure that the holding transaction also exists as a node
        graph.computeIfAbsent(holdingTransaction, key -> new HashSet<>());

        System.out.println("[WAIT-FOR] Transaction " + waitingTransaction + " is waiting for Transaction " + holdingTransaction);
    }

    /**
     * Removes a dependency between two transactions
     */
    public synchronized void removeDependency(long waitingTransaction, long holdingTransaction) {
        Set<Long> dependencies = graph.get(waitingTransaction);

        if (dependencies != null) {
            dependencies.remove(holdingTransaction);
            if (dependencies.isEmpty()) {
                graph.remove(waitingTransaction);
            }
        }

        System.out.println("[WAIT-FOR] Removed dependency Transaction " + waitingTransaction + " + Transaction " + holdingTransaction);
    }

    /**
     * Removes all dependencies involving a transaction
     */
    public synchronized void removeTransaction(long transactionId) {
        graph.remove(transactionId);

        for (Set<Long> dependencies : graph.values()) {
            dependencies.remove(transactionId);
        }

        graph.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        System.out.println("[WAIT-FOR] Removed Transaction " + transactionId);
    }

    /**
     * Checks whether the graph contains a cycle
     */
    public synchronized boolean hasCycle() {
        return !findCycle().isEmpty();
    }

    /**
     * Finds and returns one cycle in the graph
     */
    public synchronized List<Long> findCycle() {
        Set<Long> visited = new HashSet<>();
        Set<Long> recursionStack = new HashSet<>();

        List<Long> path = new ArrayList<>();

        for (Long transactionId : graph.keySet()) {
            if (!visited.contains(transactionId)) {
                List<Long> cycle = new ArrayList<>();
                if (detectCycle(transactionId, visited, recursionStack, path, cycle)) {
                    return cycle;
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * DFS alg used for cycle detection
     */
    public boolean detectCycle(Long current, Set<Long> visited, Set<Long> recursionStack, List<Long> path, List<Long> cycle) {
        visited.add(current);
        recursionStack.add(current);
        path.add(current);

        Set<Long> neighbors = graph.getOrDefault(current, new HashSet<>());

        for (Long neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                if (detectCycle(neighbor, visited, recursionStack, path, cycle)) {
                    return true;
                }
            } else if (recursionStack.contains(neighbor)) {
                int cycleStart = path.indexOf(neighbor);
                cycle.addAll(path.subList(cycleStart, path.size()));
                return true;
            }
        }
        path.remove(path.size() - 1);
        recursionStack.remove(current);
        return false;
    }

    /**
     * Returns a copy of the current graph
     */
    public synchronized Map<Long, Set<Long>> getGraphSnapshot() {
        Map<Long, Set<Long>> snapshot = new HashMap<>();

        for (Map.Entry<Long, Set<Long>> entry : graph.entrySet()) {
            snapshot.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return snapshot;
    }

    /**
     * Returns the number of dependencies in the graph
     */
    public synchronized int getDependencyCount() {
        int count = 0;
        for (Set<Long> dependencies : graph.values()) {
            count += dependencies.size();
        }
        return count;
    }
}
