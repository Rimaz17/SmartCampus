package com.mycompany.smartcampus.exception.mapper;

import com.mycompany.smartcampus.exception.LinkedResourceNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

//Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity.

@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status",  422);
        body.put("error",   "Unprocessable Entity");
        body.put("message", ex.getMessage());

        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
