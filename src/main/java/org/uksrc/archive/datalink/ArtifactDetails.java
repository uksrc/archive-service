package org.uksrc.archive.datalink;

import org.ivoa.dm.caom2.Artifact;

/**
 * Details of an Artifact for usage in a DataLink row.
 *  @param artifact The Artifact itself
 * @param planeId The containing Plane
 * @param description The description of the resource
 */
public record ArtifactDetails(Artifact artifact, String planeId, String description) {
}
