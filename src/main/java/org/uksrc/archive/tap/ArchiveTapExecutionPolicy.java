package org.uksrc.archive.tap;

import org.javastro.ivoacore.tap.TapExecutionPolicy;
import org.javastro.ivoacore.uws.environment.DefaultExecutionPolicy;

/**
 * Execution policy for TAP jobs in the archive service.
 * Extends the default execution policy with TAP-specific constraints.
 */
public class ArchiveTapExecutionPolicy extends DefaultExecutionPolicy implements TapExecutionPolicy {

    private final long maxRecords;

    /**
     * Creates a new ArchiveTapExecutionPolicy with default maximum records limit.
     */
    public ArchiveTapExecutionPolicy() {
        this(10000L); // Default max records
    }

    /**
     * Creates a new ArchiveTapExecutionPolicy with specified maximum records limit.
     *
     * @param maxRecords the maximum number of records that can be returned by a query
     */
    public ArchiveTapExecutionPolicy(long maxRecords) {
        super();
        this.maxRecords = maxRecords;
    }

    @Override
    public long maxRec() {
        return maxRecords;
    }
}
