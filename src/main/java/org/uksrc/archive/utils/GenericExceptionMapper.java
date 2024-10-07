package org.uksrc.archive.utils;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        LOG.error(exception);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity("Error: " + exception.getMessage())
                .build();
    }
}
