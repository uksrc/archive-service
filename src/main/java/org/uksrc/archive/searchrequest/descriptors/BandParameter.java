package org.uksrc.archive.searchrequest.descriptors;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import org.ivoa.dm.caom2.EnergyBand;
import org.ivoa.dm.caom2.Observation;
import org.ivoa.dm.caom2.Plane;
import org.uksrc.archive.searchrequest.query.QueryContext;
import org.uksrc.archive.searchrequest.schema.ObservationSearchRequest;

public class BandParameter implements SearchParameter {

    @Override
    public void applyIfPresent(ObservationSearchRequest request, QueryContext ctx) {
        request.band().ifPresent(value -> {
            // Convert to Enum to ensure valid input
            EnergyBand bandEnum = EnergyBand.valueOf(value.toUpperCase());

            // Join the planes
            Join<Observation, Plane> planeJoin = ctx.root().join("planes");

            // Get the path as a basic object to avoid the Collection cast error
            Path<EnergyBand> energyBandsPath = planeJoin.get("energy").get("energyBands");

            // Use a predicate that doesn't force a Collection cast
            // We use 'equal' if Hibernate maps the array to a single Enum instance,
            ctx.add(
                    ctx.criteriaBuilder().equal(energyBandsPath, bandEnum)
            );
        });
    }
}
