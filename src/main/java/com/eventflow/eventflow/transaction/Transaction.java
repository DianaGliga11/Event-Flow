package com.eventflow.eventflow.transaction;

public class Transaction {
    public final long id;
    public final long timestamp;

    public volatile TransactionStatus status;

    public Transaction(long id, long timestamp) {
        this.id = id;
        this.timestamp = timestamp;
        this.status = TransactionStatus.ACTIVE;
    }

    public long getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(TransactionStatus status) {
        if(this.status != TransactionStatus.ACTIVE){
            throw new IllegalStateException("Transaction " + id + " is already finished");
        };
        this.status = status;
    }

    public boolean isActive(){
        return status == TransactionStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return "Transaction{"
                + "id=" + id
                + ", timestamp=" + timestamp
                + ", status=" + status + '}';
    }
}
