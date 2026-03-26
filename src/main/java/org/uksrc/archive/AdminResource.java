package org.uksrc.archive;


import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/admin")
@RolesAllowed("admin")
public class AdminResource {

    @GET
    @RolesAllowed("admin")
    public String test() {
        return "ok";
    }
}

