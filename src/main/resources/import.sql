CREATE SCHEMA IF NOT EXISTS "TAP_SCHEMA";

/*
 Required structure for TAP 1.0
 From:
 http://cdsportal.u-strasbg.fr/taptuto/gettingstarted_file.html 2. Declare Metadata
 and
 https://www.ivoa.net/documents/TAP/20190927/REC-TAP-1.1.html 4. Metadata TAP_SCHEMA

 Details of xtype values found in the DALI specification
 https://www.ivoa.net/documents/DALI/20230712/WD-DALI-1.2-20230712.html#tth_sEc3 3  Data Types and Literal Values

 "dbName" is supposed to assist with logical names for database columns mapping, although a comment in
 tap.properties (full Vollt code) suggests this is now redundant and must manually be set in tap.properties
 (removing the ability to dynamically add as with the other properties unfortunately).
 */
CREATE TABLE IF NOT EXISTS "TAP_SCHEMA"."schemas" ("schema_name" VARCHAR, "description" VARCHAR, "utype" VARCHAR, "schema_index" INTEGER, "dbname" VARCHAR, PRIMARY KEY("schema_name"));
CREATE TABLE IF NOT EXISTS "TAP_SCHEMA"."tables" ("schema_name" VARCHAR, "table_name" VARCHAR, "table_type" VARCHAR, "description" VARCHAR, "utype" VARCHAR, "dbname" VARCHAR, "table_index" INTEGER, PRIMARY KEY("table_name"));
CREATE TABLE IF NOT EXISTS "TAP_SCHEMA"."columns" ("table_name" VARCHAR, "column_name" VARCHAR, "description" VARCHAR, "unit" VARCHAR, "ucd" VARCHAR, "utype" VARCHAR, "xtype" VARCHAR, "datatype" VARCHAR, "size" INTEGER, "arraysize" VARCHAR, "principal" SMALLINT, "indexed" SMALLINT, "std" SMALLINT, "column_index" INTEGER, "dbname" VARCHAR, PRIMARY KEY("table_name","column_name"));
CREATE TABLE IF NOT EXISTS "TAP_SCHEMA"."keys" ("key_id" VARCHAR, "from_table" VARCHAR, "target_table" VARCHAR, "description" VARCHAR, "utype" VARCHAR, PRIMARY KEY("key_id"));
CREATE TABLE IF NOT EXISTS "TAP_SCHEMA"."key_columns" ("key_id" VARCHAR, "from_column" VARCHAR, "target_column" VARCHAR, PRIMARY KEY("key_id"));

/**
  Add self-describing metadata for the TAP_SCHEMA and its contents.
 */
INSERT INTO "TAP_SCHEMA"."schemas" VALUES ('TAP_SCHEMA', 'Set of tables listing and describing the schemas, tables and columns published in this TAP service.', NULL, NULL) ON CONFLICT ("schema_name") DO NOTHING;

INSERT INTO "TAP_SCHEMA"."tables" (
  "schema_name", "table_name", "table_type", "description", "utype", "dbname"
) VALUES (
  'TAP_SCHEMA', 'TAP_SCHEMA.schemas', 'table', 'List of schemas published in this TAP service.', NULL, NULL
)
ON CONFLICT ("table_name") DO NOTHING;

INSERT INTO "TAP_SCHEMA"."tables" (
  "schema_name", "table_name", "table_type", "description", "utype", "dbname"
) VALUES (
  'TAP_SCHEMA', 'TAP_SCHEMA.tables', 'table', 'List of tables published in this TAP service.', NULL, NULL
)
ON CONFLICT ("table_name") DO NOTHING;

INSERT INTO "TAP_SCHEMA"."tables" (
  "schema_name", "table_name", "table_type", "description", "utype", "dbname"
) VALUES (
  'TAP_SCHEMA', 'TAP_SCHEMA.columns', 'table', 'List of columns of all tables listed in TAP_SCHEMA.TABLES and published in this TAP service.', NULL, NULL
)
ON CONFLICT ("table_name") DO NOTHING;

INSERT INTO "TAP_SCHEMA"."tables" (
  "schema_name", "table_name", "table_type", "description", "utype", "dbname"
) VALUES (
  'TAP_SCHEMA', 'TAP_SCHEMA.keys', 'table', 'List all foreign keys but provides just the tables linked by the foreign key. To know which columns of these tables are linked, see in TAP_SCHEMA.key_columns using the key_id.', NULL, NULL
)
ON CONFLICT ("table_name") DO NOTHING;

INSERT INTO "TAP_SCHEMA"."tables" (
  "schema_name", "table_name", "table_type", "description", "utype", "dbname"
) VALUES (
  'TAP_SCHEMA', 'TAP_SCHEMA.key_columns', 'table', 'List all foreign keys but provides just the columns linked by the foreign key. To know the table of these columns, see in TAP_SCHEMA.keys using the key_id.', NULL, NULL
)
ON CONFLICT ("table_name") DO NOTHING;

INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.schemas', 'schema_name', 'schema name, possibly qualified', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.schemas', 'description', 'brief description of schema', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.schemas', 'utype', 'UTYPE if schema corresponds to a data model', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.schemas', 'schema_index', 'To allow ordered display if required.', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'schema_name', 'the schema name from TAP_SCHEMA.schemas', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'table_name', 'table name as it should be used in queries', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'table_type', 'one of: table, view', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'description', 'brief description of table', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'utype', 'UTYPE if table corresponds to a data model', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'table_index', 'To allow ordered display if required.', NULL, NULL, NULL, NULL,'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'table_name', 'table name from TAP_SCHEMA.tables', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0,1, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'column_name', 'column name', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 1, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'description', 'brief description of column', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'unit', 'unit in VO standard format', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'ucd', 'UCD of column if any', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'utype', 'UTYPE of column if any', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'datatype', 'ADQL datatype as in section 2.5', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', '"size"', 'length of variable length datatypes', NULL, NULL, NULL, NULL, 'INTEGER', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'arraysize', 'arraysize, * for varying or NULL for none', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'xtype', 'custom type specification, consider utype instead', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'principal', 'a principal column; 1 means true, 0 means false', NULL, NULL, NULL, NULL, 'INTEGER', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'indexed', 'an indexed column; 1 means true, 0 means false', NULL, NULL, NULL, NULL, 'INTEGER', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'std', 'a standard column; 1 means true, 0 means false', NULL, NULL, NULL, NULL, 'INTEGER', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'column_index', 'To allow ordered display if required.', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'key_id', 'unique key identifier', NULL, NULL, NULL, NULL,'VARCHAR', -1, 1, 1, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'from_table', 'fully qualified table name', NULL, NULL, NULL, NULL,'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'target_table', 'fully qualified table name', NULL, NULL, NULL, NULL,'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'description', 'description of this key', NULL, NULL, NULL, NULL,'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'utype', 'utype of this key', NULL, NULL, NULL, NULL,'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.key_columns', 'key_id', 'unique key identifier', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.key_columns', 'from_column', 'key column name in the from_table', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.key_columns', 'target_column', 'key column name in the target_table', NULL, NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, 1) ON CONFLICT ("table_name", "column_name") DO NOTHING;

/* ************************************* */
/* ADDITION OF ALL THE OBJECT TO PUBLISH */
/* ************************************* */
INSERT INTO "TAP_SCHEMA"."schemas"(schema_name) VALUES ('public') ON CONFLICT (schema_name) DO NOTHING;