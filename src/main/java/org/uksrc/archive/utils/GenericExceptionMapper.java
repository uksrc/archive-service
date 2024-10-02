package org.uksrc.archive.utils;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        exception.printStackTrace();

        return Response.status(Response.Status.BAD_REQUEST)
                .entity("Error: " + exception.getMessage())
                .build();
    }
}
