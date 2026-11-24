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
        Transaction transaction = transactions.get(id);
        transaction.setStatus(TransactionStatus.COMMITTED);

        System.out.println("[TRANSACTION] Committed: " + id);
    }

    public void rollbackTransaction(long id){
        Transaction transaction = transactions.get(id);
        transaction.setStatus(TransactionStatus.ABORTED);

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

