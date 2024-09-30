package org.uksrc.archive.utils.responses;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hibernate.PropertyValueException;

public class Responses {
    /**
     * Generate an error response
     * @param e Whatever exception has been thrown
     * @return A 400 response containing the exception error.
     */
    public static Response errorResponse (@NotNull Exception e){
        String additional = "";
        if (e instanceof PropertyValueException){
            //Inform caller of exact property that's missing/invalid
            additional = ((PropertyValueException)e).getPropertyName();
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.TEXT_PLAIN)
                .entity(e.getMessage() + " " + additional)
                .build();
    }

    /**
     * Generate an error message
     * @param message Message to return to the caller.
     * @return A 400 response containing the supplied message
     */
    public static Response errorResponse (@NotNull String message){
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.TEXT_PLAIN)
                .entity(message)
                .build();
    }
}
