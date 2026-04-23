package com.mycompany.smartcampus.resources;

import com.mycompany.smartcampus.exception.LinkedResourceNotFoundException;
import com.mycompany.smartcampus.model.Room;
import com.mycompany.smartcampus.model.Sensor;
import com.mycompany.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


//Manages the /api/v1/sensors collection.
 
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    //Returns all sensors, or filters by type when the optional @QueryParam
     
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> all = new ArrayList<>(store.getSensors().values());

        if (type != null && !type.trim().isEmpty()) {
            // Case-insensitive filter so "co2" and "CO2" both work
            String filter = type.trim().toLowerCase();
            all = all.stream()
                    .filter(s -> s.getType() != null && s.getType().toLowerCase().equals(filter))
                    .collect(Collectors.toList());
        }

        return Response.ok(all).build();
    }

    
     /*Registers a new sensor. The roomId in the request body MUST reference
       an existing room — if not, we throw LinkedResourceNotFoundException
       which maps to 422 Unprocessable Entity.*/
    
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Sensor 'id' is required."))
                    .build();
        }

        // Validate that the referenced room actually exists
        if (sensor.getRoomId() == null || !store.roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                "Cannot register sensor: the roomId '" + sensor.getRoomId() +
                "' does not refer to any existing room in the system."
            );
        }

        // Reject duplicate sensor IDs
        if (store.sensorExists(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("A sensor with ID '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // Default status to ACTIVE if not provided
        if (sensor.getStatus() == null || sensor.getStatus().trim().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }

        store.saveSensor(sensor);

        // Also register this sensor's ID on the parent Room so the room
        // knows which sensors are deployed in it (used in delete safety check)
        Room room = store.getRoom(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().add(sensor.getId());
        }

        URI location = UriBuilder.fromResource(SensorResource.class)
                .path(sensor.getId())
                .build();

        Map<String, Object> body = new HashMap<>();
        body.put("message",  "Sensor registered successfully.");
        body.put("sensorId", sensor.getId());

        return Response.created(location).entity(body).build();
    }

    
    //Returns a single sensor by its ID.
     
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor with ID '" + sensorId + "' was not found."))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    //Sub-Resource Locator
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        // Pass the sensorId so the sub-resource knows which sensor it is scoped to
        return new SensorReadingResource(sensorId);
    }

    private Map<String, String> errorBody(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
