/*
 Example TAP_SCHEMA taken from the Vollt examples.
 TODO - Check if this is correct for either ALL use cases (or just ours)
 */
CREATE SCHEMA "TAP_SCHEMA";

CREATE TABLE "TAP_SCHEMA"."schemas" ("schema_name" VARCHAR, "description" VARCHAR, "utype" VARCHAR, "dbname" VARCHAR, PRIMARY KEY("schema_name"));
CREATE TABLE "TAP_SCHEMA"."tables" ("schema_name" VARCHAR, "table_name" VARCHAR, "table_type" VARCHAR, "description" VARCHAR, "utype" VARCHAR, "dbname" VARCHAR, PRIMARY KEY("table_name"));
CREATE TABLE "TAP_SCHEMA"."columns" ("table_name" VARCHAR, "column_name" VARCHAR, "description" VARCHAR, "unit" VARCHAR, "ucd" VARCHAR, "utype" VARCHAR, "datatype" VARCHAR, "size" INTEGER, "principal" SMALLINT, "indexed" SMALLINT, "std" SMALLINT, "dbname" VARCHAR, PRIMARY KEY("table_name","column_name"));
CREATE TABLE "TAP_SCHEMA"."keys" ("key_id" VARCHAR, "from_table" VARCHAR, "target_table" VARCHAR, "description" VARCHAR, "utype" VARCHAR, PRIMARY KEY("key_id"));
CREATE TABLE "TAP_SCHEMA"."key_columns" ("key_id" VARCHAR, "from_column" VARCHAR, "target_column" VARCHAR, PRIMARY KEY("key_id"));

INSERT INTO "TAP_SCHEMA"."schemas" VALUES ('TAP_SCHEMA', 'Set of tables listing and describing the schemas, tables and columns published in this TAP service.', NULL, NULL);

INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.schemas', 'table', 'List of schemas published in this TAP service.', NULL, NULL);
INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.tables', 'table', 'List of tables published in this TAP service.', NULL, NULL);
INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.columns', 'table', 'List of columns of all tables listed in TAP_SCHEMA.TABLES and published in this TAP service.', NULL, NULL);
INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.keys', 'table', 'List all foreign keys but provides just the tables linked by the foreign key. To know which columns of these tables are linked, see in TAP_SCHEMA.key_columns using the key_id.', NULL, NULL);
INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.key_columns', 'table', 'List all foreign keys but provides just the columns linked by the foreign key. To know the table of these columns, see in TAP_SCHEMA.keys using the key_id.', NULL, NULL);

INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.schemas', 'schema_name', 'schema name, possibly qualified', NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.schemas', 'description', 'brief description of schema', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.schemas', 'utype', 'UTYPE if schema corresponds to a data model', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'schema_name', 'the schema name from TAP_SCHEMA.schemas', NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'table_name', 'table name as it should be used in queries', NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'table_type', 'one of: table, view', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'description', 'brief description of table', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'utype', 'UTYPE if table corresponds to a data model', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'table_name', 'table name from TAP_SCHEMA.tables', NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'column_name', 'column name', NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'description', 'brief description of column', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'unit', 'unit in VO standard format', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'ucd', 'UCD of column if any', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'utype', 'UTYPE of column if any', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'datatype', 'ADQL datatype as in section 2.5', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', '"size"', 'length of variable length datatypes', NULL, NULL, NULL, 'INTEGER', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'principal', 'a principal column; 1 means true, 0 means false', NULL, NULL, NULL, 'INTEGER', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'indexed', 'an indexed column; 1 means true, 0 means false', NULL, NULL, NULL, 'INTEGER', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'std', 'a standard column; 1 means true, 0 means false', NULL, NULL, NULL, 'INTEGER', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'key_id', 'unique key identifier', NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'from_table', 'fully qualified table name', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'target_table', 'fully qualified table name', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'description', 'description of this key', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'utype', 'utype of this key', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.key_columns', 'key_id', 'unique key identifier', NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.key_columns', 'from_column', 'key column name in the from_table', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.key_columns', 'target_column', 'key column name in the target_table', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1, NULL);

/* ************************************* */
/* ADDITION OF ALL THE OBJECT TO PUBLISH */
/* ************************************* */
INSERT INTO "TAP_SCHEMA"."schemas"(schema_name) VALUES ('public');


/*
 ObsPlan - NOTE: looks like it requires more tables ObsTAP??
 */

/*
 NOTE: s_region  should be s_region (requires pgSphere installing though)
 */
/*CREATE TABLE "public"."obsplan" (t_planning double precision NOT NULL, target_name character varying,
                              obs_id            character varying NOT NULL,
                              obs_collection    character varying NOT NULL,
                              s_ra              double precision,
                              s_dec             double precision,
                              s_fov             double precision,
                              s_region          character varying NOT NULL,
                              s_resolution      double precision,
                              t_min             double precision NOT NULL,
                              t_max             double precision NOT NULL,
                              t_exptime         double precision,
                              t_resolution      double precision,
                              em_min            double precision,
                              em_max            double precision,
                              em_res_power      double precision,
                              o_ucd             character varying NOT NULL,
                              pol_states        character varying,
                              pol_xel           integer,
                              facility_name     character varying NOT NULL,
                              instrument_name   character varying NOT NULL,
                              obs_release_date  timestamp,
                              t_plan_exptime    double precision NOT NULL,
                              category          character varying,
                              priority          integer,
                              execution_status  character varying NOT NULL
);


INSERT INTO "TAP_SCHEMA".tables (schema_name,table_name,table_type,description,utype,dbname) VALUES ('public','ivoa.obsplan','table','ObsLocTAP compatible table','','');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','t_planning','something here','d','','','DOUBLE',0,0,0,0,'t_planning');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','target_name','something here','','','','VARCHAR',32,0,0,0,'target_name');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','obs_id','something here','','','','VARCHAR',32,0,0,0,'obs_id');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','obs_collection','something here','','','','VARCHAR',32,0,0,0,'obs_collection');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','s_ra','something here','deg','','','DOUBLE',0,0,0,0,'s_ra');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','s_dec','something here','deg','','','DOUBLE',0,0,0,0,'s_dec');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','s_fov','something here','deg','','','DOUBLE',0,0,0,0,'s_fov');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','s_resolution','something here','arcsec','','','DOUBLE',0,0,0,0,'s_resolution');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','t_min','something here','d','','','DOUBLE',0,0,0,0,'t_min');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','t_max','something here','d','','','DOUBLE',0,0,0,0,'t_max');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','t_exptime','something here','s','','','DOUBLE',0,0,0,0,'t_exptime');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','t_resolution','something here','s','','','DOUBLE',0,0,0,0,'t_resolution');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','em_min','something here','m','','','DOUBLE',0,0,0,0,'em_min');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','em_max','something here','m','','','DOUBLE',0,0,0,0,'em_max');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','em_res_power','something here','','','','DOUBLE',0,0,0,0,'em_res_power');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','o_ucd','something here','','','','VARCHAR',32,0,0,0,'o_ucd');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','pol_states','something here','','','','VARCHAR',32,0,0,0,'pol_states');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','pol_xel','something here','','','','BIGINT',0,0,0,0,'pol_xel');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','facility_name','something here','','','','VARCHAR',32,0,0,0,'facility_name');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','instrument_name','something here','','','','VARCHAR',32,0,0,0,'instrument_name');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','obs_release_date','date','','','','TIMESTAMP',0,0,0,0,'obs_release_date');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','t_plan_exptime','s','','','','DOUBLE',0,0,0,0,'t_plan_exptime');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','category','something here','','','','VARCHAR',32,0,0,0,'category');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','priority','something here','','','','INTEGER',0,0,0,0,'priority');
INSERT INTO "TAP_SCHEMA".columns (table_name,column_name,description,unit,ucd,utype,datatype,"size",principal,indexed,std,dbname) VALUES ( 'ivoa.obsplan','s_region','something here','','','','VARCHAR',32,0,0,0,'s_region');
*/




  /* Attempt at reading resources from the database & adding them to the TAP_SCHEMA.
   Works from a console (as a script) but can't get the converted function to work here so moving to
   java as an initial/alternate solution.
 */

/*

 DO $$
    DECLARE
        table_record RECORD;
        c RECORD;
    BEGIN
        -- Iterate over the result of the obs_tables query
        FOR table_record IN
            (SELECT table_name
             FROM information_schema.tables
             WHERE table_name IN ('Observation', 'SimpleObservation', 'DerivedObservation'))    -- Limited to Observation(s) for testing only
            LOOP
                -- Insert the table name and row count into the fixed table
                INSERT INTO "TAP_SCHEMA"."tables"(schema_name,table_name,description) VALUES ('public', table_record.table_name, 'Details of a(n) ' || table_record.table_name);

                -- Insert each column from the current table
                FOR c IN
                    EXECUTE 'SELECT column_name, data_type, character_maximum_length
                     FROM information_schema.columns
                     WHERE table_name = ' || quote_literal(table_record.table_name)
                    LOOP
                        -- Insert the column metadata into column_metadata table
                        INSERT INTO "TAP_SCHEMA"."columns"(table_name,column_name,description,datatype,"size",unit,ucd,principal,indexed) VALUES (table_record.table_name, c.column_name, NULL, c.data_type, NULL, NULL, NULL, 0, 0);
                    END LOOP;
            END LOOP;
    END $$;

 */