package com.eventflow.eventflow.transaction;

public class UndoLogException extends RuntimeException{
    public UndoLogException(String message){
        super(message);
    }

    public UndoLogException(String message, Throwable cause){
        super(message, cause);
    }
}
