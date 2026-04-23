package com.mycompany.smartcampus.resources;

import com.mycompany.smartcampus.exception.RoomNotEmptyException;
import com.mycompany.smartcampus.model.Room;
import com.mycompany.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


 // Handles all CRUD operations for the /api/v1/rooms collection.

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    // access the shared singleton store
    private final DataStore store = DataStore.getInstance();

    //Returns the full list of all rooms currently in the system.
   
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(store.getRooms().values());
        return Response.ok(roomList).build();
    }

    //Creates a new room. Returns 201 Created with a Location header
     
    @POST
    public Response createRoom(Room room) {
        // Basic validation: id and name must be present
        if (room == null || room.getId() == null || room.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Room 'id' is required."))
                    .build();
        }
        if (room.getName() == null || room.getName().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Room 'name' is required."))
                    .build();
        }

        // Reject duplicates
        if (store.roomExists(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("A room with ID '" + room.getId() + "' already exists."))
                    .build();
        }

        store.saveRoom(room);

        // Build the URI of the new resource for the Location header
        URI location = UriBuilder.fromResource(RoomResource.class)
                .path(room.getId())
                .build();

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("message", "Room created successfully.");
        body.put("roomId",  room.getId());

        return Response.created(location).entity(body).build();
    }

    
     //Returns full metadata for a single room. 404 if not found.
     
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room with ID '" + roomId + "' was not found."))
                    .build();
        }
        return Response.ok(room).build();
    }

    
    //DELETE /api/v1/rooms/{roomId}
     
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);

        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room with ID '" + roomId + "' was not found."))
                    .build();
        }

        // Block deletion if any sensors are still deployed in this room
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                "Room '" + roomId + "' cannot be deleted — it still has " +
                room.getSensorIds().size() + " sensor(s) assigned to it. " +
                "Remove all sensors from the room before decommissioning it."
            );
        }

        store.deleteRoom(roomId);

        Map<String, String> body = new java.util.HashMap<>();
        body.put("message", "Room '" + roomId + "' has been successfully deleted.");
        return Response.ok(body).build();
    }

    // Helper to build a consistent error JSON body
    private Map<String, String> errorBody(String message) {
        Map<String, String> error = new java.util.HashMap<>();
        error.put("error", message);
        return error;
    }
}
