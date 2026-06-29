package com.citics.glxtapi.plugin.db.exception;

/**
 * exception when druid dataSource init failed
 */
public class ErrorCreateDataSourceException extends RuntimeException {

    public ErrorCreateDataSourceException(String message) {
        super(message);
    }

    public ErrorCreateDataSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
