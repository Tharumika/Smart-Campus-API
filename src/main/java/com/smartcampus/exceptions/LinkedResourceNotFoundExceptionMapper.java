package com.smartcampus.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

/**
 * ExceptionMapper that intercepts LinkedResourceNotFoundException and returns
 * an HTTP 422 Unprocessable Entity with a structured JSON body.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("status", 422); // Unprocessable Entity
        errorBody.put("error", "Unprocessable Entity");
        errorBody.put("message", exception.getMessage());

        return Response.status(422)
                .entity(errorBody)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}