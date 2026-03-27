package org.uksrc.archive.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.beanutils.BeanUtils;
import org.ivoa.dm.caom2.Observation;
import org.uksrc.archive.utils.responses.Responses;
import org.uksrc.archive.utils.tools.Tools;

@ApplicationScoped
public class ObservationService {

    @PersistenceContext
    protected EntityManager em;

    /**
     * Adds an observation to the database
     * @param observation Either a SimpleObservation or a DerivedObservation
     * @return Response containing status code and added observation (if successful)
     */
    @Transactional
    public Response submitObservation(Observation observation) {
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
    }

    @Transactional
    public Response updateObservation(String id, Observation observation) {
        try {
            if(id == null || id.isEmpty()) {
                return Responses.errorResponse("Invalid ID");
            }
            else if (!id.equals(observation.getId())){
                return Responses.errorResponse("id MUST be the same as observation.id");
            }

            //Only update IF found
            Observation existing = findObservation(id);
            if (existing != null) {
                //Copy all properties from the supplied observation over the existing observation.
                //Observation.uri MUST remain the same and won't be affected.
                BeanUtils.copyProperties(existing, observation);
                Object formattedObs = Tools.formatObservation(existing);
                return Response.ok(formattedObs).build();
            }
        } catch (Exception e) {
            return Responses.errorResponse(e);
        }

        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.TEXT_PLAIN)
                .entity("Observation not found")
                .build();
    }

    @Transactional
    public Response getObservation(String id) {
        try {
            Observation observation = findObservation(id);
            if (observation != null) {
                Object formattedObs = Tools.formatObservation(observation);

                return Response.status(Response.Status.OK)
                        .entity(formattedObs).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("Observation with ID " + id + " not found").build();
            }
        } catch (Exception e) {
            return Responses.errorResponse(e);
        }
    }

    @Transactional
    public Response deleteObservation(String id) {
        try {
            Observation observation = findObservation(id);
            if (observation != null) {
                em.remove(observation);
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("Observation with ID " + id + " not found")
                        .build();
            }
        } catch (Exception e) {
            return Responses.errorResponse(e);
        }
    }

    @Transactional
    public Response getAllObservations(String collection, Integer page, Integer size) {
        //Both page and size need to be supplied OR neither
        if ((page != null) ^ (size != null)) {
            return Responses.errorResponse("Both 'page' and 'size' must be provided together or neither.");
        } else if ((page != null && page < 0) || (size != null && size < 1)) {
            return Responses.errorResponse("Page must be 0 or greater and size must be greater than 0.");
        }

        try {
            TypedQuery<Observation> query;
            if (collection != null && !collection.isEmpty()) {
                query = em.createQuery("SELECT o FROM Observation o WHERE o.collection = :collection", Observation.class);
                query.setParameter("collection", collection);
            } else {
                query = em.createQuery("SELECT o FROM Observation o", Observation.class);
            }
            return Tools.performQuery(page, size, query);
        } catch (Exception e) {
            return Responses.errorResponse(e);
        }
    }

    /**
     * Checks to see if an observation with the supplied ID already exists
     * @param id Observation.id
     * @return The observation if found, null if not
     */
    private Observation findObservation(String id) {
        TypedQuery<Observation> existsQuery = em.createQuery(
                "SELECT o FROM Observation o WHERE o.id = :id", Observation.class
        );

        try {
            existsQuery.setParameter("id", id);
            return existsQuery.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }
}
