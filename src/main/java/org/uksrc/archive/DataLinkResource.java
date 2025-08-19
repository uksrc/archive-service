package org.uksrc.archive;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.uksrc.archive.datalink.VOTableGenerator;

@Path("/datalink")
public class DataLinkResource {

    @Inject
    VOTableGenerator voTableGenerator;

    @GET
    @Path("/links")
    @Produces(MediaType.APPLICATION_XML)
    public Response getDataLinkedObject(@QueryParam("ID") String id){
        //TODO match ID with database object

        StreamingOutput out = voTableGenerator.createDocument();
        return Response.ok(out).build();
    }
}
