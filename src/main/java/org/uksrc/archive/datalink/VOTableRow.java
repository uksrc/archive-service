package org.uksrc.archive.datalink;

import java.util.Objects;

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

    }

    public static class Builder {
        private String id;
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
    }
}
