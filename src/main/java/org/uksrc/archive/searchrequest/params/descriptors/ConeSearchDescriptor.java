package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.*;
import org.uksrc.archive.searchrequest.query.QueryContext;
import static org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldDefinition;

public class ConeSearchDescriptor implements PredicateDescriptor {

    private final FieldDefinition fieldDef;
    private final double ra;     // Degrees
    private final double dec;    // Degrees
    private final double radius; // Degrees

    public ConeSearchDescriptor(FieldDefinition fieldDef, double ra, double dec, double radius) {
        this.fieldDef = fieldDef;
        this.ra = ra;
        this.dec = dec;
        this.radius = radius;
    }

    @Override
    public Predicate toPredicate(QueryContext<?> context) {
        CriteriaBuilder cb = context.criteriaBuilder();

        // 1. Resolve the path to the coordinate entity (e.g., tp.coordinates)
        // This uses your existing logic to join through the entity graph
        Path<?> coordsPath = resolvePath(context.root(), fieldDef.entityPath());

        // 2. Access the coordinate values (cval1 = RA/Lon, cval2 = Dec/Lat)
        // Note: These are usually stored in radians in pgsphere
        Expression<Double> raProp = coordsPath.get("cval1").as(Double.class);
        Expression<Double> decProp = coordsPath.get("cval2").as(Double.class);

        // 3. Define the distance function: pgsphere_distance(p1, p2, p3, p4)
        // We pass the parameters as radians
        Expression<Double> distanceFunc = cb.function(
                "pgsphere_distance",
                Double.class,
                raProp,
                decProp,
                cb.literal(ra),         //NOTE: not converted to radians as the pgsphere_distance function expects degrees
                cb.literal(dec)
        );

        // 4. Compare distance to the radius (converted to radians)
        return cb.lessThanOrEqualTo(distanceFunc, Math.toRadians(radius));
    }
}
