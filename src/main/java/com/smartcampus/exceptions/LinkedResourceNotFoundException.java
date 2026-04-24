package com.smartcampus.exceptions;

/**
 * Custom exception thrown when attempting to POST a new Sensor with a roomId
 * that does not exist in the system. Maps to HTTP 422 Unprocessable Entity.
 */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}