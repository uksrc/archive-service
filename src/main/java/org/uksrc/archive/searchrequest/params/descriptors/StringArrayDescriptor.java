package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldDefinition;
import org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldType;
import org.uksrc.archive.searchrequest.query.QueryContext;

/**
 * This class is an implementation of the {@link PredicateDescriptor} interface
 * and provides logic to create JPA query predicates specifically for handling
 * string array fields in a database. It is designed to process field definitions
 * with the type {@link FieldType#STRING_ARRAY} and apply case-insensitive matching.
 *
 * <p>The resulting predicate is constructed using the JPA Criteria API, allowing dynamic,
 * type-safe query generation.
 *
 * <p>Utilises:
 * - The {@link QueryContext} to access the {@link CriteriaBuilder} and root entity for the query.
 * - The {@link FieldDefinition} to extract field metadata such as the field type and entity path.
 */
public class StringArrayDescriptor implements PredicateDescriptor {

    private final FieldDefinition fieldDef;
    private final String value;

    public StringArrayDescriptor(FieldDefinition fieldDef, String value) {
        this.fieldDef = fieldDef;
        this.value = value.toUpperCase();
    }

    /**
     * Constructs a JPA predicate to filter query results based on the field definition
     * and value provided within the context of the given {@link QueryContext}.
     * This particular method searches for values in a value separated string.
     *
     * @param ctx the {@link QueryContext} that provides access to the {@link CriteriaBuilder}
     *            and root entity for predicate construction.
     * @return a {@link Predicate} that represents the filtering condition based on
     *         the field definition and input value.
     */
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
