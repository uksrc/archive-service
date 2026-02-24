package org.uksrc.archive.searchrequest.params.transform;

@FunctionalInterface
public interface RangeTransformer {
    // Takes (min, max) from user and returns a specific bound for the DB
    Double transform(String min, String max, boolean isLowerBound);
}