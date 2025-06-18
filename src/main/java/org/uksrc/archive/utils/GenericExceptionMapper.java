package org.uksrc.archive.utils;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@SuppressWarnings("unused")
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger log = Logger.getLogger(GenericExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        String message = exception.getMessage();
        if (message == null && exception.getCause() != null) {
            message = exception.getCause().getMessage();
        }
        log.error(message);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity("Error: " + message)
                .build();
    }
}
