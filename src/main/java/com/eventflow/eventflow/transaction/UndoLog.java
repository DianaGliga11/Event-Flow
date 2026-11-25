package com.eventflow.eventflow.transaction;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UndoLog {
    private final Map<Long, Deque<UndoOperation>> undoOperations = new ConcurrentHashMap<>();

    /**
     * Register an undo operation for a transaction
     * The operation must be registered before executing the corresponding database modification
     */
    public void addOperation(long transactionId, UndoOperation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }

        Deque<UndoOperation> operations = undoOperations.computeIfAbsent(transactionId, id -> new ArrayDeque<>());

        synchronized (operations) {
            operations.push(operation);
        }

        System.out.println("[UNDO LOG] Registered operation for transaction " + transactionId);
    }

    /**
     * Executes auto undo operations in reverse order
     */
    public void rollback(long transactionId) {
        Deque<UndoOperation> operations = undoOperations.get(transactionId);

        if (operations == null) {
            System.out.println("[UNDO LOG] No operation to undo for transaction  " + transactionId + " not found");
            return;
        }

        Exception firtError = null;

        while (true) {
            UndoOperation operation;
            synchronized (operations) {
                operation = operations.pollFirst();
            }
            if (operation == null) {
                break;
            }

            try {
                operation.undo();

                System.out.println("[UNDO LOG] Operation undone for transaction " + transactionId);
            } catch (Exception e) {
                System.err.println("[UNDO LOG] Failed to undo an operation for transaction " + transactionId + ": " + e.getMessage());
                if (firtError == null) {
                    firtError = e;
                } else {
                    firtError.addSuppressed(e);
                }
            }
        }
        undoOperations.remove(transactionId, operations);

        if (firtError != null) {
            throw new UndoLogException("Rollback was incomplete for transaction " + transactionId, firtError);
        }

        System.out.println("[UNDO LOG] Rollback completed for transaction " + transactionId);
    }

    /**
     * Discards undo operations after a successful commit
     */
    public void clear(long transactionId) {
        undoOperations.remove(transactionId);

        System.out.println("[UNDO LOG] Cleared for committed transaction " + transactionId);
    }

    /**
     * Returns the number of pending undo operations
     */
    public int getOperationCount(long transactionId) {
        Deque<UndoOperation> operations = undoOperations.get(transactionId);

        if (operations == null) {
            return 0;
        }
        synchronized (operations) {
            return operations.size();
        }
    }

    /**
     * Checks whether a transaction has pending undo operations
     */
    public boolean hasOperations(long transactionId) {
        return getOperationCount(transactionId) > 0;
    }
}

