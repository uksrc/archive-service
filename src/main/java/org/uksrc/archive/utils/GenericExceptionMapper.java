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
        log.error(exception.getCause());

        return Response.status(Response.Status.BAD_REQUEST)
                .entity("Error: " + exception.getCause().getMessage())
                .build();
    }
}
