package org.uksrc.archive.datalink;

import java.util.ArrayList;
import java.util.List;

/**
 * Specialisation of the DataLinkRow (VOTable row) that adds in custom properties for use
 * in the Archive Service.
 */
public class ArtifactTableRow extends DataLinkRow {

    private final String planeId;           // Which plane this resource belongs to

    /**
     * Creates a ArtifactTableRow with mandatory properties, only one of accessUrl, serviceDef or errorMessage is required, and the other
     * two can be omitted.
     * @param id The ID of the data element represented by this row. MUST be supplied
     * @param semantics The semantics of the data element
     * @param accessUrl Resolvable location of the resource
     * @param serviceDef Name of the service
     * @param errorMessage Details of the error, such as resource not found
     * @param planeId The Plane that this Artifact belongs to.
     */
    public ArtifactTableRow(String id, String semantics, String accessUrl, String serviceDef, String errorMessage, String planeId) {
        super(id, semantics, accessUrl, serviceDef, errorMessage);
        this.planeId = planeId;
    }

    public String getPlaneId() { return planeId; }
}

