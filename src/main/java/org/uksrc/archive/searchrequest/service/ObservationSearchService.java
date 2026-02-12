package org.uksrc.archive.searchrequest.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ivoa.dm.caom2.Artifact;
import org.ivoa.dm.caom2.Observation;
import org.uksrc.archive.searchrequest.descriptors.BandParameter;
import org.uksrc.archive.searchrequest.descriptors.SearchParameter;
import org.uksrc.archive.searchrequest.descriptors.StartDateParameter;
import org.uksrc.archive.searchrequest.descriptors.TargetParameter;
import org.uksrc.archive.searchrequest.query.QueryContext;
import org.uksrc.archive.searchrequest.schema.ObservationSearchRequest;

import java.util.List;

import static jakarta.persistence.criteria.JoinType.LEFT;

@ApplicationScoped
public class ObservationSearchService {

    @Inject
    EntityManager em;

    private final List<SearchParameter> parameters = List.of(
            new TargetParameter(),
            new BandParameter(),
            new StartDateParameter()
    );

    public TypedQuery<Observation> searchQuery(ObservationSearchRequest request) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Observation> cq = cb.createQuery(Observation.class);
        Root<Observation> root = cq.from(Observation.class);

        cq.distinct(true);

        QueryContext ctx = new QueryContext(cb, root);

        for (SearchParameter parameter : parameters) {
            parameter.applyIfPresent(request, ctx);
        }

        cq.where(ctx.predicates().toArray(new Predicate[0]));

        return em.createQuery(cq);
    }
}

