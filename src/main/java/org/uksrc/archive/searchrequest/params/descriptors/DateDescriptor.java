package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.*;
import org.uksrc.archive.searchrequest.query.QueryContext;
import static org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldDefinition;

import java.time.*;
import java.time.format.DateTimeParseException;

public class DateDescriptor implements PredicateDescriptor {

    private final FieldDefinition fieldDef;
    private final String value; // Raw ISO string

    private static final Instant MJD_EPOCH =
            LocalDate.of(1858, 11, 17)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();

    public DateDescriptor(FieldDefinition fieldDef, String value) {
        this.fieldDef = fieldDef;
        this.value = value;
    }

    @Override
    public Predicate toPredicate(QueryContext<?> context) {
        CriteriaBuilder cb = context.criteriaBuilder();

        // 1. Resolve to the object containing the bounds (usually 'time')
        From<?, ?> parent = resolveParentPath(context.root(), fieldDef);

        // 2. Resolve to the 'bounds' path
        String boundsPropertyName = getLastPathSegment(fieldDef.entityPath());
        Path<?> boundsPath = parent.get(boundsPropertyName);

        // 3. Parse the user input to MJD Seconds
        double userTimeMjd = parseToMjd(value);

        // 4. THE LOGIC: Observation End (upper) >= User Start Time
        // This ensures we get observations that started after the date
        // OR were already in progress.
        return cb.greaterThanOrEqualTo(
                boundsPath.get("upper").as(Double.class),
                userTimeMjd
        );
    }

    private double parseToMjd(String val) {
        try {
            // Full ISO 8601
            return toMjdSeconds(OffsetDateTime.parse(val).toInstant());
        } catch (DateTimeParseException e) {
            // Date only (YYYY-MM-DD) -> Start of day
            return toMjdSeconds(LocalDate.parse(val).atStartOfDay(ZoneOffset.UTC).toInstant());
        }
    }

    private From<?, ?> resolveParentPath(From<?, ?> root, FieldDefinition def) {
        String[] parts = def.entityPath().split("\\.");
        From<?, ?> current = root;
        // Join up to the part before 'bounds' (e.g., planes -> time)
        for (int i = 0; i < parts.length - 1; i++) {
            current = current.join(parts[i]);
        }
        return current;
    }

    private String getLastPathSegment(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }

    private double toMjdSeconds(Instant instant) {
        return Duration.between(MJD_EPOCH, instant).toNanos() / 1_000_000_000.0;
    }
}
