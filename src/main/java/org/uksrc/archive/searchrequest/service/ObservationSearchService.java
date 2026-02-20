package org.uksrc.archive.searchrequest.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ivoa.dm.caom2.Observation;
import org.uksrc.archive.searchrequest.params.descriptors.*;
import org.uksrc.archive.searchrequest.query.QueryContext;

import java.util.List;

/**
 * A service class responsible for dynamically constructing and executing search queries
 * for the `Observation` entity using the Java Persistence API (JPA) Criteria API.
 * This class facilitates flexible and reusable query construction based on filtering
 * conditions represented by `PredicateDescriptor` objects.
 * <p>
 * The primary role of this service is to generate JPA `TypedQuery` instances based
 * on user-defined search criteria encapsulated within the descriptors, allowing
 * dynamic filtering of `Observation` data.
 * <p>
 * This class is application-scoped, ensuring a single shared instance can be
 * injected and used across the application.
 */
@ApplicationScoped
public class ObservationSearchService {

    @Inject
    EntityManager em;

    /**
     * Constructs and returns a JPA TypedQuery for retrieving Observation entities
     * based on the given list of PredicateDescriptor objects, which specify filtering criteria.
     * <p>
     * The method uses the Java Persistence API Criteria API to dynamically build
     * the query by applying the predicates generated from the provided descriptors.
     *
     * @param descriptors a list of PredicateDescriptor objects, each of which encapsulates
     *        logic for generating a predicate to be included in the query criteria. These
     *        descriptors define the filtering conditions for fetching Observation entities.
     * @return a TypedQuery instance configured to execute the search based on the
     *         constructed criteria and the predicates derived from the given descriptors.
     */
    public TypedQuery<Observation> searchQuery(List<PredicateDescriptor> descriptors) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Observation> cq = cb.createQuery(Observation.class);
        Root<Observation> root = cq.from(Observation.class);

        QueryContext<Observation> ctx = new QueryContext<>(cb, root);

        List<Predicate> predicates = descriptors.stream()
                .map(d -> d.toPredicate(ctx))
                .toList();

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        return em.createQuery(cq);
    }
}

