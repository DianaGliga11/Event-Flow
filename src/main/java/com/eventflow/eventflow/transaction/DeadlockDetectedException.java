package com.eventflow.eventflow.transaction;

public class DeadlockDetectedException extends RuntimeException{
    public DeadlockDetectedException(String message) {
        super(message);
    }
}
