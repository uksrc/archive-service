package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.*;
import org.uksrc.archive.searchrequest.params.parser.DescriptorFactory;
import org.uksrc.archive.searchrequest.query.QueryContext;

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

    private final DescriptorFactory.FieldDefinition fieldDef;
    private final String value;

    public CollectionDescriptor(DescriptorFactory.FieldDefinition fieldDef, String value) {
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
    public Predicate toPredicate(QueryContext ctx) {
        CriteriaBuilder cb = ctx.criteriaBuilder();
        From<?, ?> parent = resolveParentPath(ctx.root(), fieldDef);
        String lastPart = getLastPathSegment(fieldDef.entityPath());

        // Handle COLLECTION (varchar[] or converted strings)
        if (fieldDef.type() == DescriptorFactory.FieldType.COLLECTION) {
            Expression<String> pathAsString = parent.get(lastPart).as(String.class);

            return cb.or(
                    cb.like(pathAsString, "%{" + value + "}%"),
                    cb.like(pathAsString, "%," + value + ",%"),
                    cb.like(pathAsString, "%," + value + "}%"),
                    cb.like(pathAsString, "%{" + value + ",%")
            );
        }   // If required, add an else to handle true arrays that Hibernate can parse into a collection.

        // Handle standard Scalar ENUM or STRING
        // Compare the DB column (as a string) to our string value.
        return cb.equal(parent.get(lastPart).as(String.class), value);
    }

    /**
     * Resolves the parent path of an entity by traversing the entity path defined in the given field definition
     * and joining the intermediate parts of the path starting from the root.
     *
     * @param root The starting point (root) of the entity graph.
     * @param def The field definition containing the entity path to be resolved.
     * @return The last accessed path of the parent in the entity graph.
     */
    private From<?, ?> resolveParentPath(From<?, ?> root, DescriptorFactory.FieldDefinition def) {
        String[] parts = def.entityPath().split("\\.");
        From<?, ?> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            current = current.join(parts[i]);
        }
        return current;
    }

    /**
     * Returns the property path's final value that represents the column in the database.
     * @param path The full path to the property, such as plane.id.
     * @return plane.id would return "id".
     */
    private String getLastPathSegment(String path) {
        String[] parts = path.split("\\.");
        return parts[parts.length - 1];
    }
}

