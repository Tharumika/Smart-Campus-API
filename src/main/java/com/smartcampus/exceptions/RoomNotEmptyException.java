package com.smartcampus.exceptions;

/**
 * Custom exception thrown when attempting to delete a Room that still
 * has Sensors assigned to it. Maps to HTTP 409 Conflict.
 */
public class RoomNotEmptyException extends RuntimeException {
    public RoomNotEmptyException(String message) {
        super(message);
    }
}