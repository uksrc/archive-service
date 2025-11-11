package org.uksrc.archive.datalink;

import java.util.*;
import java.util.stream.Stream;

/**
 * VOTable Row that defines the properties of datalink table row for exposing a resource.
 */
public class DataLinkRow extends VOTableRow {

    protected String id;                       // Required
    protected String accessUrl;                // ┐
    protected String serviceDef;               // ├ One of these three must be set
    protected String errorMessage;             // ┘
    protected String description;              // Field required, value not required
    protected String semantics;                // Required
    protected String contentType;              // Field required, value not required
    protected Long contentLength;              // Field required, value not required

    //Additional Optional fields
    protected String contentQualifier;
    protected String localSemantics;
    protected String linkAuth;
    protected String linkAuthorized;

    /**
     * Creates a DataLinkRow with mandatory properties, only one of accessUrl, serviceDef or errorMessage is required, and the other
     * two can be omitted.
     * @param id The ID of the data element represented by this row. MUST be supplied
     * @param semantics The semantics of the data element
     * @param accessUrl Resolvable location of the resource
     * @param serviceDef Name of the service
     * @param errorMessage Details of the error, such as resource not found
     */
    public DataLinkRow(String id, String semantics, String accessUrl, String serviceDef, String errorMessage) {
        this.id = id;
        this.accessUrl = accessUrl;
        this.serviceDef = serviceDef;
        this.errorMessage = errorMessage;
        this.semantics = semantics;

        //Enforce 'one of' for the properties listed
        long count = Stream.of(accessUrl, serviceDef, errorMessage)
                .filter(Objects::nonNull)
                .count();

        if (count != 1) {
            throw new IllegalStateException(
                    "Exactly one of accessUrl, serviceDef, or errorMessage must be provided");
        }
    }

    public String getId() { return id; }
    public String getAccessUrl() { return accessUrl; }
    public String getServiceDef() { return serviceDef; }
    public String getErrorMessage() { return errorMessage; }
    public String getDescription() { return description; }
    public String getSemantics() { return semantics; }
    public String getContentType() { return contentType; }
    public Long getContentLength() { return contentLength; }

    public String getContentQualifier() { return contentQualifier; }
    public String getLocalSemantics() { return localSemantics; }
    public String getLinkAuth() { return linkAuth; }
    public String getLinkAuthorized() { return linkAuthorized; }

    // Setters for non-mandatory values
    public void setDescription(String description) { this.description = description; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setContentLength(Long contentLength) { this.contentLength = contentLength; }

    public void setContentQualifier(String content_qualifier) { this.contentQualifier = content_qualifier; }
    public void setLocalSemantics(String localSemantics) { this.localSemantics = localSemantics; }
    public void setLinkAuth(String linkAuth) { this.linkAuth = linkAuth; }
    public void setLinkAuthorized(String linkAuthorized) { this.linkAuthorized = linkAuthorized; }
}

