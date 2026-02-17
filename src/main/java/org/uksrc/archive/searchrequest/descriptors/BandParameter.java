package org.uksrc.archive.searchrequest.descriptors;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import org.ivoa.dm.caom2.EnergyBand;
import org.ivoa.dm.caom2.Observation;
import org.ivoa.dm.caom2.Plane;
import org.uksrc.archive.searchrequest.query.QueryContext;
import org.uksrc.archive.searchrequest.schema.ObservationSearchRequest;

/**
 * The BandParameter class is an implementation of the SearchParameter interface,
 * designed to filter observation queries based on a specified energy band.
 * <p>
 * This class checks whether a band value is provided in an ObservationSearchRequest,
 * and if present, it adds a filtering condition to the QueryContext. The filter ensures
 * that query results are constrained to those observations where the energy bands
 * match the requested value. The provided band is converted into the corresponding
 * Enum value to guarantee valid input.
 * <p>
 * Responsibilities of this class include:
 * - Retrieving the band value from the search request if present.
 * - Converting the provided band string into an Enum value of type EnergyBand
 *   to ensure input validity.
 * - Constructing a predicate that compares the energy band of observations
 *   to the requested value.
 * - Adding this predicate to the query context, which is later executed as
 *   part of the search request.
 * <p>
 * This parameter operates specifically within the context of the ObservationSearchRequest
 * and QueryContext classes, enabling dynamic query filtering for data involving energy bands.
 */
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
