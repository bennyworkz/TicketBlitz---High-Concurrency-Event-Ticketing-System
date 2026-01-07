package com.ticketblitz.common.exceptions;

/**
 * Exception thrown when event is sold out (Tatkal mode)
 */
public class SoldOutException extends RuntimeException {
    
    public SoldOutException(String message) {
        super(message);
    }
    
    public SoldOutException(String message, Throwable cause) {
        super(message, cause);
    }
}
