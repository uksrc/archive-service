package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldDefinition;
import org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldType;
import org.uksrc.archive.searchrequest.query.QueryContext;

public class StringArrayDescriptor implements PredicateDescriptor {

    private final FieldDefinition fieldDef;
    private final String value;

    public StringArrayDescriptor(FieldDefinition fieldDef, String value) {
        this.fieldDef = fieldDef;
        this.value = value.toUpperCase();
    }

    @Override
    public Predicate toPredicate(QueryContext<?> ctx) {
        CriteriaBuilder cb = ctx.criteriaBuilder();
        Path<?> rawPath = resolvePath(ctx.root(), fieldDef.entityPath());
        Expression<String> fieldPath = rawPath.as(String.class);

        if (fieldDef.type() == FieldType.STRING_ARRAY) {
            String searchTerm = "#" + value.toUpperCase() + "#";

            // Concat '#' to the beginning and end of the column value
            // Resulting SQL: UPPER('#' || col || '#') LIKE '%#VALUE#%'
            Expression<String> paddedColumn = cb.upper(
                    cb.concat("#", cb.concat(fieldPath, "#"))
            );

            return cb.like(paddedColumn, "%" + searchTerm + "%");
        }

        return cb.equal(cb.upper(fieldPath), value.toUpperCase());
    }
}
