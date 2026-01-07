package com.ticketblitz.common.exceptions;

/**
 * Exception thrown when booking is not found
 */
public class BookingNotFoundException extends RuntimeException {
    
    public BookingNotFoundException(String message) {
        super(message);
    }
    
    public BookingNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
