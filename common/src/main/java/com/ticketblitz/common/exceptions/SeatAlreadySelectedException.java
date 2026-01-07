package com.ticketblitz.common.exceptions;

/**
 * Exception thrown when a seat is already locked by another user
 */
public class SeatAlreadySelectedException extends RuntimeException {
    
    public SeatAlreadySelectedException(String message) {
        super(message);
    }
    
    public SeatAlreadySelectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
