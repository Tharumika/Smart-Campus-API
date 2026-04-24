package com.smartcampus.exceptions;

/**
 * Custom exception thrown when a sensor currently marked with the status "MAINTENANCE"
 * or "OFFLINE" is physically disconnected and cannot accept new readings. Maps to 403.
 */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) {
        super(message);
    }
}