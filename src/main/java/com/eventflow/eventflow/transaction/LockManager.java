package com.eventflow.eventflow.transaction;

import org.springframework.stereotype.Component;

import java.beans.IntrospectionException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class LockManager {
    //Resource: Transaction holding a lock on that resource
    private final Map<LockResource, Map<Long, LockType>> locks = new HashMap<>();

    //Transaction: Resources locked by that transaction
    private final Map<Long, Set<LockResource>> transactionLocks = new HashMap<>();

    private final WaitForGraph waitForGraph;

    public LockManager(WaitForGraph waitForGraph){
        this.waitForGraph = waitForGraph;
    }

    /**
     * @param transaction
     * @param resource
     * @param requestedType
     * @throws IntrospectionException Request a READ or Write lock for a transaction
     *                                A transaction waits if another transaction holds an incompatible lock
     */
    public synchronized void acquireLock(
            Transaction transaction,
            LockResource resource,
            LockType requestedType
    ) throws InterruptedException {
        long transactionId = transaction.getId();
        if (!transaction.isActive()) {
            throw new IllegalStateException("Transaction is not active: " + transactionId);
        }

        while (!canGrantLock(transactionId, resource, requestedType)) {
            if (!transaction.isActive()) {
                waitForGraph.removeTransaction(transactionId);
                throw new IllegalStateException("Transaction was aborted while waiting for a lock: " + transactionId);
            }

            //Identify transactions that prevent this lock from being granted
            Set<Long> blockers = getBlockingTransactionIds(transaction, resource, requestedType);

            //Add waiting dependencies to Wait-For Graph
            for(Long blockerId : blockers) {
                waitForGraph.addDependency(transactionId, blockerId);
            }

            System.out.println("[LOCK] Transaction " + transactionId + " is waiting for " + requestedType + " lock on " + resource);

            //Detect a deadlock immidiately after adding dependencies
            if(waitForGraph.hasCycle()){
                System.out.println("[DEADLOCK] Cycle detected: " + waitForGraph.findCycle());

                //The transaction requesting the last lock becoms a victim
                waitForGraph.removeTransaction(transactionId);

                throw new DeadlockDetectedException("Deadlock detected. Transaction " + transaction + " was selected as a vistim");
            }
            try{
                wait();
            }catch (InterruptedException e){
                waitForGraph.removeTransaction(transactionId);
                Thread.currentThread().interrupt();
                throw e;
            }

            //The transaction woke up; remove its old dependencies
            waitForGraph.removeTransaction(transactionId);
            if(!transaction.isActive()){
                throw new IllegalStateException("Transaction is not active: " + transactionId);
            }
        }
        //The requested lock can now be granted
        grantLock(transactionId, resource, requestedType);

        waitForGraph.removeTransaction(transactionId);

        System.out.println("[LOCK] Transaction " + transactionId + " aquired " + requestedType + " lock on " + resource);
    }

    /**
     * @param transactionId
     * @param resource
     * @param requestedType Grants the requested lock and updates both data structures
     */
    private void grantLock(long transactionId, LockResource resource, LockType requestedType) {
        //Map of transactions holding locks on this resource
        Map<Long, LockType> resourceLocks = locks.computeIfAbsent(resource, key -> new HashMap<>());

        //Check whether the transaction already has a lock
        LockType existingLock = resourceLocks.get(transactionId);

        /*
        If the transaction has a WRITE lock -> does not need another lock
        If it has a READ lock and request a WRITE -> the lock is upgraded
         */
        if (existingLock == LockType.WRITE) {
            return;
        }

        if (existingLock == LockType.READ && requestedType == LockType.WRITE) {
            resourceLocks.put(transactionId, LockType.WRITE);
        } else if (existingLock == null) {
            resourceLocks.put(transactionId, requestedType);
        }
        //Update the resource locked by this transaction
        transactionLocks.computeIfAbsent(transactionId, key -> new HashSet<>()).add(resource);
    }

    /**
     * @param transactionId
     * @param resource
     * @param requestedType
     * @return Check whether a requestes lock is compatible with the loks currently held by another transaction
     */
    private boolean canGrantLock(long transactionId, LockResource resource, LockType requestedType) {
        Map<Long, LockType> resourceLocks = locks.get(resource);

        //No transaction holds a lock on this resource
        if (resourceLocks == null) {
            return true;
        }

        for (Map.Entry<Long, LockType> entry : resourceLocks.entrySet()) {
            long holderId = entry.getKey();
            LockType heldType = entry.getValue();

            //A transaction does not conflict with its own lock
            if (holderId == transactionId) {
                continue;
            }
            //Other combinations are incompatible
            return false;
        }
        return true;
    }

    /**
     * Releases all locks held by a transaction
     * Method must be called after COMMIT or ABORT
     */
    public synchronized void releaseAllLocks(Transaction transaction) {
        long transactionId = transaction.getId();

        Set<LockResource> resources = transactionLocks.remove(transactionId);

        if (resources == null) {
            return;
        }

        for (LockResource resource : resources) {
            Map<Long, LockType> resourceLocks = locks.get(resource);
            if (resourceLocks != null) {
                resourceLocks.remove(transactionId);
                if (resourceLocks.isEmpty()) {
                    locks.remove(resource);
                }

                System.out.println("[LOCK] Transaction " + transactionId + " released lock on " + resource);
            }
        }
        waitForGraph.removeTransaction(transactionId);
        //Wake up transactions waiting for locks
        notifyAll();
    }

    /**
     * Returns the transactions holding incompatible locks on a resource
     */
    public synchronized Set<Long> getBlockingTransactionIds(
            Transaction transaction,
            LockResource resource,
            LockType requestedType
    ) {
        Set<Long> blockingTransactions = new HashSet<>();
        Map<Long, LockType> resourceLocks = locks.get(resource);

        if (resourceLocks == null) {
            return blockingTransactions;
        }

        for (Map.Entry<Long, LockType> entry : resourceLocks.entrySet()) {
            long holderId = entry.getKey();
            LockType heldType = entry.getValue();

            if (holderId == transaction.getId()) {
                continue;
            }

            boolean compatible = requestedType == LockType.READ && heldType == LockType.WRITE;
            if (!compatible) {
                blockingTransactions.add(holderId);
            }
        }

        return blockingTransactions;
    }

    /**
     * Checks whether a transaction holds a lock on a reasource
     */
    public synchronized boolean hasLock(
            Transaction transaction,
            LockResource resource
    ) {
        Set<LockResource> resources = transactionLocks.get(transaction.getId());
        return resources != null && resources.contains(resource);
    }

    /**
     * Returns the type of lock held by a transaction
     */
    public synchronized LockType getLockType(
            Transaction transaction,
            LockResource resource
    ){
        Map<Long, LockType> resourceLocks = locks.get(resource);
        if(resourceLocks == null) {
            return null;
        }

        return resourceLocks.get(transaction.getId());
    }

    /**
     * Returns the number of resources currently locked
     */
    public synchronized int getLockedResourceCount(){
        return locks.size();
    }
}
