package org.uksrc.archive;


import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.ivoa.dm.caom2.Observation;
import org.uksrc.archive.utils.responses.Responses;
import org.uksrc.archive.utils.tools.Tools;

@Path("/admin")
@RolesAllowed("admin")
public class AdminResource {

  ///  @PersistenceContext
  //  protected EntityManager em;

    @GET
    @RolesAllowed("admin")
    public String test() {
        return "ok";
    }

    /**
     * Adds an observation to the database
     * @param observation Either a SimpleObservation or a DerivedObservation
     * @return Response containing status code and added observation (if successful)
     */
  /*  private Response submitObservation(Observation observation) {
        if (findObservation(observation.getId()) != null) {
            return Responses.errorResponse("Observation.id " + observation.getId() + " already exists.");
        }

        try {
            em.persist(observation);
            em.flush();

            Object formattedObs = Tools.formatObservation(observation);
            return Response.status(Response.Status.CREATED)
                    .entity(formattedObs)
                    .build();

        } catch (Exception e) {
            return Responses.errorResponse(e);
        }
    }*/

    /**
     * Checks to see if an observation with the supplied ID already exists
     * @param id Observation.id
     * @return The observation if found, null if not
     */
  /*  private Observation findObservation(String id) {
        TypedQuery<Observation> existsQuery = em.createQuery(
                "SELECT o FROM Observation o WHERE o.id = :id", Observation.class
        );

        try {
            existsQuery.setParameter("id", id);
            return existsQuery.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }*/
}

