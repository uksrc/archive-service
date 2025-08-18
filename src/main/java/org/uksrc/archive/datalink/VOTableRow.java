package org.uksrc.archive.datalink;

import java.util.Objects;
import java.util.stream.Stream;

public class VOTableRow {
    private String id;                       // Required
    private String accessUrl;                // ┐
    private String serviceDef;               // ├ One of these three must be set
    private String errorMessage;             // ┘
    private String description;              // Field required, value not required
    private String semantics;                // Required
    private String contentType;              // Field required, value not required
    private Long contentLength;              // Field required, value not required

    //Optional fields
    private String content_qualifier;
    private String local_semantics;
    private String link_auth;
    private String link_authorized;


    private VOTableRow(Builder builder) {
        this.id = builder.id;
        this.accessUrl = builder.accessUrl;
        this.serviceDef = builder.serviceDef;
        this.errorMessage = builder.errorMessage;
        this.description = builder.description;
        this.semantics = builder.semantics;
        this.contentType = builder.contentType;
        this.contentLength = builder.contentLength;

        this.content_qualifier = builder.content_qualifier;
        this.local_semantics = builder.local_semantics;
        this.link_auth = builder.link_auth;
        this.link_authorized = builder.link_authorized;

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
    public String getContent_qualifier() { return content_qualifier; }
    public String getLocal_semantics() { return local_semantics; }
    public String getLink_auth() { return link_auth; }
    public String getLink_authorized() { return link_authorized; }

    public static class Builder {
        private final String id;
        private String accessUrl;
        private String serviceDef;
        private String errorMessage;
        private String description;
        private String semantics;
        private String contentType;
        private Long contentLength;

        private String content_qualifier;
        private String local_semantics;
        private String link_auth;
        private String link_authorized;

        public Builder(String id, String semantics) {
            this.id = Objects.requireNonNull(id, "id is required");
            this.semantics = Objects.requireNonNull(semantics, "semantics is required");
        }

        public Builder accessUrl(String url) { this.accessUrl = url; return this; }
        public Builder serviceDef(String def) { this.serviceDef = def; return this; }
        public Builder errorMessage(String msg) { this.errorMessage = msg; return this; }

        public Builder description(String d) { this.description = d; return this; }
        public Builder semantics(String s) { this.semantics = s; return this; }
        public Builder contentType(String ct) { this.contentType = ct; return this; }
        public Builder contentLength(Long len) { this.contentLength = len; return this; }

        public Builder content_qualifier(String s) { this.content_qualifier = s; return this; }
        public Builder local_semantics(String s) { this.local_semantics = s; return this; }
        public Builder link_auth(String s) { this.link_auth = s; return this; }
        public Builder link_authorized(String s) { this.link_authorized = s; return this; }

        public VOTableRow build() { return new VOTableRow(this); }
    }
}
