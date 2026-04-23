package com.mycompany.smartcampus.exception.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


 /*
 This mapper intercepts any exception not caught by a more specific mapper
 (NullPointerException, IndexOutOfBoundsException, etc.) and returns a
 clean HTTP 500 Internal Server Error with a generic JSON body*/

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        // Log the full details server-side so developers can diagnose the issue
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by GlobalExceptionMapper", ex);

        // Return a clean, non-revealing response to the client
        Map<String, Object> body = new HashMap<>();
        body.put("status",  500);
        body.put("error",   "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please contact the API administrator.");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
