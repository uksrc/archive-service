package org.uksrc.archive.datalink;

/**
 * Details of an Artifact for usage in a DataLink row.
 * @param name The name of the field
 * @param ucd The Unified Content Descriptor of the field (type of quantity such as 'phys.temperature').
 * @param dataType Primitive data type that this field represents, 'char', 'int', 'long' as examples.
 * @param arraySize The size of the array, '*' is valid for undefined.
 * @param unit The unit of measurement, such as 'deg' or 'arcseconds'.
 */
public record FieldDetails(String name, String ucd, String dataType, String arraySize, String unit) {
}
