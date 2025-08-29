package org.uksrc.archive.datalink;

import java.util.HashMap;
import java.util.Map;

public class DataLinkFields {
    private static final Map<String, FieldDetails> defs = new HashMap<>();

    static {
        defs.put("id", new FieldDetails("ID", "meta.id;meta.main", "char", "*", null));
        defs.put("accessUrl", new FieldDetails("access_url", "meta.ref.url", "char", "*", null));
        defs.put("serviceDef", new FieldDetails("service_def", "meta.ref", "char", "*", null));
        defs.put("errorMessage", new FieldDetails("error_message", "meta.code.error", "char", "*", null));
        defs.put("description", new FieldDetails("description", "meta.note", "char", "*", null));
        defs.put("semantics", new FieldDetails("semantics", "meta.code", "char", "*", null));
        defs.put("contentType", new FieldDetails("content_type", "meta.code.mime", "char", "*", null));
        defs.put("contentLength", new FieldDetails("content_length", "phys.size;meta.file", "long", null, "byte"));

        // Optional fields
        defs.put("contentQualifier", new FieldDetails("content_qualifier", "meta.code.class", "char", "*", null));
        defs.put("localSemantics", new FieldDetails("local_semantics", "meta.code", "char", "*", null));
        defs.put("linkAuth", new FieldDetails("link_auth", "meta.code", "char", "*", null));
        defs.put("linkAuthorized", new FieldDetails("link_authorized", "meta.code", "boolean", null, null));

        // Custom fields for the Archive Service
        defs.put("planeId", new FieldDetails("plane_id", "meta.id;meta.id.assoc", "char", "*", null));
    }

    public static FieldDetails get(String key) {
        return defs.get(key);
    }
}
