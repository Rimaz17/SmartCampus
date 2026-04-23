package com.mycompany.smartcampus.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

//GET /api/v1 returns API metadata
@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {

        // Build the links map so clients can navigate to each collection
        Map<String, String> links = new HashMap<>();
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");

        // Top-level metadata object
        Map<String, Object> response = new HashMap<>();
        response.put("apiVersion",   "1.0");
        response.put("description",  "Smart Campus Sensor & Room Management API");
        response.put("contact",      "admin@smartcampus.ac.uk");
        response.put("status",       "operational");
        response.put("resources",    links);

        return Response.ok(response).build();
    }
}
