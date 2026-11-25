package com.eventflow.eventflow.transaction;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TransactionManager {
    private final AtomicLong transactionIdGenerator = new AtomicLong();

    private final AtomicLong timestampGenerator = new AtomicLong();

    private final ConcurrentMap<Long, Transaction> transactions = new ConcurrentHashMap<>();

    private final LockManager lockManager;

    private final UndoLog undoLog;

    public TransactionManager(LockManager lockManager, UndoLog undoLog) {
        this.lockManager = lockManager;
        this.undoLog = undoLog;
    }
    public Transaction beginTransaction() {
        long id = transactionIdGenerator.incrementAndGet();
        long timestamp = timestampGenerator.incrementAndGet();

        Transaction transaction = new Transaction(id, timestamp);
        transactions.put(id, transaction);

        System.out.println("[TRANSACTION] Started: " + timestamp);

        return transaction;
    }

    public Transaction getTransaction(long id) {
        return transactions.get(id);
    }

    public Collection<Transaction> getTransactions() {
        return transactions.values();
    }

    public void commitTransaction(long id){
        Transaction transaction = getExistingTransaction(id);

        undoLog.clear(id);

        transaction.setStatus(TransactionStatus.COMMITTED);

        lockManager.releaseAllLocks(transaction);

        System.out.println("[TRANSACTION] Committed: " + id);
    }

    public void abortTransaction(long id){
        Transaction transaction = getExistingTransaction(id);

        undoLog.rollback(id);

        transaction.setStatus(TransactionStatus.ABORTED);

        lockManager.releaseAllLocks(transaction);

        System.out.println("[TRANSACTION] Aborted: " + id);
    }

    private Transaction getExistingTransaction(long id) {
        Transaction transaction = transactions.get(id);

        if(transaction == null) {
            throw new IllegalArgumentException("Transaction not found " + id);
        }

        return transaction;
    }
}

