package org.uksrc.archive.datalink;

import org.ivoa.dm.caom2.Artifact;

/**
 * Details of an Artifact for usage in a DataLink row.
 * @param planeId The containing Plane
 * @param artifact The Artifact itself
 */
public record ArtifactDetails(String planeId, Artifact artifact, String description) {
}
