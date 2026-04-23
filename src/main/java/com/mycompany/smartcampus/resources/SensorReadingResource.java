package com.mycompany.smartcampus.resources;

import com.mycompany.smartcampus.exception.SensorUnavailableException;
import com.mycompany.smartcampus.model.Sensor;
import com.mycompany.smartcampus.model.SensorReading;
import com.mycompany.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//Historical Reading Management (Sub-Resource)
 
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    /**
     * Called by SensorResource's sub-resource locator.
     * The sensorId is captured from the parent path and injected here.
     */
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

   
    //Returns the complete historical reading log for the given sensor.
     
    @GET
    public Response getReadings() {
        // Verify the sensor actually exists before returning its readings
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' was not found."))
                    .build();
        }

        List<SensorReading> history = store.getReadings(sensorId);
        return Response.ok(history).build();
    }

    //Appends a new reading to the sensor's history.
    
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensor(sensorId);

        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' was not found."))
                    .build();
        }

        // Block readings for sensors under maintenance
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is currently under MAINTENANCE and " +
                "cannot accept new readings. Bring the sensor back ONLINE first."
            );
        }

        // Also block OFFLINE sensors they are not transmitting data
        if ("OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is OFFLINE and cannot accept new readings."
            );
        }

        // Auto-assign a UUID if the client didn't provide one
        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }

        // Auto-assign the current timestamp if not supplied
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        store.addReading(sensorId, reading);

        // Side effect: update parent sensor's currentValue
        sensor.setCurrentValue(reading.getValue());

        Map<String, Object> body = new HashMap<>();
        body.put("message",   "Reading recorded successfully.");
        body.put("readingId", reading.getId());
        body.put("sensorCurrentValue", sensor.getCurrentValue());

        return Response.status(Response.Status.CREATED).entity(body).build();
    }

    private Map<String, String> errorBody(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
