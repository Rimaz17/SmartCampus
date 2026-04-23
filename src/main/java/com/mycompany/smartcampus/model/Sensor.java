package com.mycompany.smartcampus.model;

/**
 * Represents a physical sensor deployed inside a campus room
 * Valid statuses are: "ACTIVE", "MAINTENANCE", "OFFLINE"
 */
public class Sensor {

    private String id;           // Unique identifier
    private String type;         // Category: "Temperature", "Occupancy", "CO2", etc.
    private String status;       // Current state: "ACTIVE", "MAINTENANCE", or "OFFLINE"
    private double currentValue; // Most recent measurement recorded by this sensor
    private String roomId;       // Foreign key linking to the Room this sensor belongs to

    public Sensor() {}

    public Sensor(String id, String type, String status, double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
}
