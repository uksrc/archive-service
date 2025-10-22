package org.uksrc.archive.datalink;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Details all the fields expected in the DataLink output, all the standard fields,
 * all the optional fields and any fields required for the Archive Service.
 * NOTE: Field names match the getter/setter names of DataLinkRow
 * @see DataLinkRow
 */
public class FieldOrder {

    //The list of properties that SHOULD be displayed and added in the order that matches the table header.
    public static final List<String> FIELD_ORDER = Arrays.asList(
            "id", "accessUrl", "serviceDef", "errorMessage",
            "description", "semantics", "contentType", "contentLength"
    );

    //The list of properties that MAY be displayed if they are required and added in the order that matches the table header.
    public static final List<String> OPTIONAL_FIELD_ORDER = Arrays.asList(
            "contentQualifier", "localSemantics", "linkAuth", "linkAuthorized"
    );

    //Custom fields required by the Archive Service
    public static final List<String> CUSTOM_FIELD_ORDER = List.of(
            "planeId"
    );

    /**
     * Convenience method that returns all fields in their expected order for
     * the Archive Service.
     * @return List<String> of ALL the fields in order.
     */
    public static List<String> getAllFieldsOrder() {
        ArrayList<String> fieldOrder = new ArrayList<>(FIELD_ORDER);
        fieldOrder.addAll(OPTIONAL_FIELD_ORDER);
        fieldOrder.addAll(CUSTOM_FIELD_ORDER);
        return fieldOrder;
    }
}
