package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.*;
import org.uksrc.archive.searchrequest.query.QueryContext;
import static org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldDefinition;
import static org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldType;

/**
 * The CollectionDescriptor class is used to construct a JPA Predicate
 * for handling fields that may represent either COLLECTIONs or scalar ENUM/STRING values.
 * It implements the PredicateDescriptor interface and is responsible for generating
 * query predicates based on specific field definitions and values.
 * <p>
 * Note that this is intended for single-column VARCHAR[] array values that hibernate cannot parse into
 * a collection (stored as a scalar value).
 */
public class CollectionDescriptor implements PredicateDescriptor {

    private final FieldDefinition fieldDef;
    private final String value;

    public CollectionDescriptor(FieldDefinition fieldDef, String value) {
        this.fieldDef = fieldDef;
        this.value = value.toUpperCase();
    }

    /**
     * Builds a JPA Predicate based on the specified query context and the field definition.
     * This method determines whether the field is a collection or a scalar and generates
     * the appropriate condition to filter database results.
     *
     * @param ctx The QueryContext containing the CriteriaBuilder and root entity necessary
     *            for constructing the Predicate.
     * @return A Predicate that represents the constructed query condition.
     */
    @Override
    public Predicate toPredicate(QueryContext<?> ctx) {
        CriteriaBuilder cb = ctx.criteriaBuilder();
        Path<?> fieldPath = resolvePath(ctx.root(), fieldDef.entityPath());

        // Handle COLLECTION (varchar[] or converted strings)
        if (fieldDef.type() == FieldType.COLLECTION) {
            Expression<String> pathAsString = fieldPath.as(String.class);

            return cb.or(
                    cb.like(pathAsString, "%{" + value + "}%"),
                    cb.like(pathAsString, "%," + value + ",%"),
                    cb.like(pathAsString, "%," + value + "}%"),
                    cb.like(pathAsString, "%{" + value + ",%")
            );
        }   // If required, add an else to handle true arrays that Hibernate can parse into a collection.

        // Handle standard Scalar ENUM or STRING
        // Compare the DB column (as a string) to our string value.
        return cb.equal(fieldPath, value);
    }
}

