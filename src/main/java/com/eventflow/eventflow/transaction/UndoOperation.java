package com.eventflow.eventflow.transaction;

@FunctionalInterface
public interface UndoOperation {
    void undo() throws Exception;
}