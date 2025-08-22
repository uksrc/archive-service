package org.uksrc.archive.datalink;

import java.util.Arrays;
import java.util.List;

public class ArchiveTableRow extends DataLinkRow {

    private final String planeId;

    private static final List<String> FIELD_ORDER = Arrays.asList(
            "id", "accessUrl", "serviceDef", "errorMessage",
            "description", "semantics", "contentType", "contentLength",
            "planeId"
    );

    public ArchiveTableRow(String id, String semantics, String accessUrl, String serviceDef, String errorMessage, String planeId) {
        super(id, semantics, accessUrl, serviceDef, errorMessage);
        this.planeId = planeId;
    }

    public String getPlaneId() { return planeId; }

    @Override
    public List<String> getFieldOrder(){
        return FIELD_ORDER;
    }
}

