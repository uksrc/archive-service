package org.uksrc.archive.datalink;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Specialisation of the DataLinkRow (VOTable row) that adds in custom properties for use
 * in the Archive Service.
 */
public class ArchiveTableRow extends DataLinkRow {

    private final String planeId;

    // Fields that get displayed
   /** private static final List<String> FIELD_ORDER = Arrays.asList(
            "id", "accessUrl", "serviceDef", "errorMessage",
            "description", "semantics", "contentType", "contentLength",
            "planeId"
    );*/

    public ArchiveTableRow(String id, String semantics, String accessUrl, String serviceDef, String errorMessage, String planeId) {
        super(id, semantics, accessUrl, serviceDef, errorMessage);
        this.planeId = planeId;
    }

    public String getPlaneId() { return planeId; }

    @Override
    public List<String> getFieldOrder() {
        List<String> fields = new ArrayList<>(FIELD_ORDER);
        fields.addAll(extraFields());
        return fields;
    }

    protected List<String> extraFields() {
        return List.of("planeId");
    }
}

