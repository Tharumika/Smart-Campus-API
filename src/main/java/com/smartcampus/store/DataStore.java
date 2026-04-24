package com.smartcampus.store;

import com.smartcampus.models.Room;
import com.smartcampus.models.Sensor;
import com.smartcampus.models.SensorReading;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton DataStore using Thread-Safe Collections.
 * Essential for JAX-RS because a new Resource instance is created per request.
 * Utilizing ConcurrentHashMap ensures safe data access across concurrent HTTP requests.
 */
public class DataStore {
    private static DataStore instance;

    // Using ConcurrentHashMap to ensure thread-safety during concurrent reads/writes
    private Map<String, Room> rooms = new ConcurrentHashMap<>();
    private Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    
    // Maps a sensorId to a thread-safe list of readings
    private Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    private DataStore() {
        // Pre-populate 1 room
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        rooms.put(r1.getId(), r1);
    }

    // Singleton Pattern
    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    // Safely returns or creates a new reading list for a specific sensor
    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return sensorReadings.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>());
    }
}
