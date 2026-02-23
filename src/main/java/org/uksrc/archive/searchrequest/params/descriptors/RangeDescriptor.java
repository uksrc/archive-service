package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.uksrc.archive.searchrequest.query.QueryContext;
import static org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldDefinition;

public class RangeDescriptor<V extends Comparable<? super V>>
        implements PredicateDescriptor {

    private final FieldDefinition fieldDef;
    private final V min;
    private final V max;

    public RangeDescriptor(FieldDefinition fieldDef, V min, V max) {
        this.fieldDef = fieldDef;
        this.min = min;
        this.max = max;
    }

    @Override
    public Predicate toPredicate(QueryContext<?> ctx) {
        CriteriaBuilder cb = ctx.criteriaBuilder();

        // 1. Join through planes/energy to get the parent of the 'bounds' object
        From<?, ?> parent = resolveParentPath(ctx.root(), fieldDef);

        // 2. Since 'bounds' is @Embedded, we use .get() twice:
        // once for the embedded object, once for its attribute.
        String lastPart = getLastPart(fieldDef.entityPath());
        Path<V> dbMin = parent.get(lastPart).get(fieldDef.minAttribute());
        Path<V> dbMax = parent.get(lastPart).get(fieldDef.maxAttribute());

        // Overlap Logic: (dbMin <= queryMax) AND (dbMax >= queryMin)
        return cb.and(
                cb.lessThanOrEqualTo(dbMin, max),
                cb.greaterThanOrEqualTo(dbMax, min)
        );
    }

    private From<?, ?> resolveParentPath(From<?, ?> root, FieldDefinition def) {
        String[] parts = def.entityPath().split("\\.");
        From<?, ?> current = root;
        // Join associations (like 'plane' or 'energy')
        // but STOP before the @Embedded 'bounds' object.
        for (int i = 0; i < parts.length - 1; i++) {
            current = current.join(parts[i]);
        }
        return current;
    }

    private String getLastPart(String path) {
        String[] parts = path.split("\\.");
        return parts[parts.length - 1];
    }
}