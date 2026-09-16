package com.this4u.data.engine.exception;

public class MetaEngineException extends RuntimeException {
    public MetaEngineException(String message) {
        super(message);
    }

    public MetaEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
