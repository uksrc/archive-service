package org.uksrc.archive.searchrequest.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.ivoa.dm.caom2.Observation;
import org.uksrc.archive.searchrequest.descriptors.SearchParameter;
import org.uksrc.archive.searchrequest.query.QueryContext;
import org.uksrc.archive.searchrequest.schema.ObservationSearchRequest;

import java.util.List;

@ApplicationScoped
public class ObservationRepository {

    @Inject
    EntityManager em;

    public List<Observation> findByRequest(ObservationSearchRequest request, List<SearchParameter> parameters) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Observation> cq = cb.createQuery(Observation.class);
        Root<Observation> root = cq.from(Observation.class);

        // Your existing logic fits perfectly here
        QueryContext ctx = new QueryContext(cb, root);
        for (SearchParameter parameter : parameters) {
            parameter.applyIfPresent(request, ctx);
        }

        cq.where(ctx.predicates().toArray(new Predicate[0]));
        cq.distinct(true);

        return em.createQuery(cq).getResultList();
    }
}
