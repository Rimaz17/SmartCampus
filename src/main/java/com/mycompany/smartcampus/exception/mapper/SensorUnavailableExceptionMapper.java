package com.mycompany.smartcampus.exception.mapper;

import com.mycompany.smartcampus.exception.SensorUnavailableException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

//Maps SensorUnavailableException to HTTP 403 Forbidden.

@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status",  403);
        body.put("error",   "Forbidden");
        body.put("message", ex.getMessage());

        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
