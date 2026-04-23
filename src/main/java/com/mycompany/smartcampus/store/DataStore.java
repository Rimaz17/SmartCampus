package com.mycompany.smartcampus.store;

import com.mycompany.smartcampus.model.Room;
import com.mycompany.smartcampus.model.Sensor;
import com.mycompany.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


//Application-wide singleton that holds all in-memory data
 
public class DataStore {

    // The one and only instance created once when the class is loaded
    private static final DataStore INSTANCE = new DataStore();

    // ConcurrentHashMap: thread-safe key value store, no explicit synchronization needed
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();

    // Each sensor has its own list of readings; the outer map is concurrent,
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    // Private constructor prevents external instantiation
    private DataStore() {}

    //Returns the singleton instance of the DataStore
    public static DataStore getInstance() {
        return INSTANCE;
    }

    //ROOM OPERATIONS

    public Map<String, Room> getRooms() { return rooms; }

    public Room getRoom(String id) { return rooms.get(id); }

    public void saveRoom(Room room) { rooms.put(room.getId(), room); }

    public boolean deleteRoom(String id) { return rooms.remove(id) != null; }

    public boolean roomExists(String id) { return rooms.containsKey(id); }

    //SENSOR OPERATIONS

    public Map<String, Sensor> getSensors() { return sensors; }

    public Sensor getSensor(String id) { return sensors.get(id); }

    public void saveSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        // Ensure a readings list exists for every sensor from the moment it is saved
        readings.putIfAbsent(sensor.getId(), new ArrayList<>());
    }

    public boolean sensorExists(String id) { return sensors.containsKey(id); }

    //READING OPERATIONS

    /*
     * Returns the readings list for a given sensor.
     * Returns an empty list (never null) if the sensor has no readings yet
     */
    public List<SensorReading> getReadings(String sensorId) {
        return readings.getOrDefault(sensorId, new ArrayList<>());
    }

    //Appends a new reading to the sensor's history
    public void addReading(String sensorId, SensorReading reading) {
        readings.putIfAbsent(sensorId, new ArrayList<>());
        List<SensorReading> list = readings.get(sensorId);
        synchronized (list) {
            list.add(reading);
        }
    }
}
