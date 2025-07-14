package org.uksrc.archive.utils.tools.tap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.ivoa.dm.caom2.Caom2Model;
import org.ivoa.dm.tapschema.ColNameKeys;
import org.ivoa.dm.tapschema.Schema;
import org.ivoa.dm.tapschema.TapschemaModel;
import org.jboss.logging.Logger;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.*;

/*
Adds any existing entities in the database to the TAP_SCHEMA (tables and columns)
 */
@Startup
@ApplicationScoped
public class TapSchemaPopulator {

    @Inject
    EntityManager entityManager;
    @Inject
    TapSchemaRepository tapSchemaRepository;

    private static final Logger LOG = Logger.getLogger(TapSchemaPopulator.class);

    static final String CHECK_TABLE_EXISTS_SQL = "SELECT 1 FROM \"TAP_SCHEMA\".\"tables\" WHERE table_name = ?";
    static final String CHECK_TAP_DEPLOYED_SQL = "SELECT schema_name FROM \"TAP_SCHEMA\".schemas;";
    static final String CHECK_SCHEMA_ADDED_SQL = "SELECT schema_name FROM \"TAP_SCHEMA\".schemas WHERE schema_name = ?";
    static final String GET_TABLES_FOR_SCHEMA =  "SELECT table_name FROM information_schema.tables WHERE table_schema = ?";
    static final String GET_COLUMNS_FOR_TABLE = "SELECT column_name, data_type, udt_name, character_maximum_length FROM information_schema.columns WHERE table_name = ?";
    static final String SCHEMA_NAME = "TAP_SCHEMA";

    @SuppressWarnings("unused")
    @PostConstruct
    public void initialize() {
        try {
            // Add the self-describing information of the TAP SCHEMA to the database.
            // As the tap_schema tables need to contain information about itself as well as the objects that are to be exposed.
            int num = 0;
            try {
                num = entityManager.createNativeQuery(CHECK_TAP_DEPLOYED_SQL).getResultList().size();
            }
            catch (Exception e) {
                String msg = "tap_schema.schemas not found";
                Log.error(msg, e);
                System.out.println(msg);
            }
            if (num == 0){
                addTapSchema();
            }

            addCAOMSchema();

        }catch (Exception e) {
            LOG.error("Populating TAP Schema", e);
        }
    }

    /**
     * Add the TAP Schema to the database.
     */
    private void addTapSchema() {
        try {
            InputStream is = TapschemaModel.TAPSchema();
            addModel(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add the CAOM Schema to the database.
     */
    private void addCAOMSchema() {
        try {
            InputStream is = Caom2Model.TAPSchema();
            addModel(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds the supplied model to the database.
     * @param is An input stream that contains the model
     * @throws jakarta.xml.bind.JAXBException for errors caused whilst parsing the model
     */
    private void addModel( InputStream is) throws jakarta.xml.bind.JAXBException {
        if (is != null) {
            TapschemaModel model = new TapschemaModel();
            JAXBContext jc = model.management().contextFactory();
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            JAXBElement<TapschemaModel> el = unmarshaller.unmarshal(new StreamSource(is), TapschemaModel.class);
            TapschemaModel model_in = el.getValue();
            if (model_in != null) {
                ColNameKeys.normalize(model_in);
                List<Schema> schemas = model_in.getContent(Schema.class);
                if (!schemas.isEmpty()) {
                    Schema schema = schemas.get(0);
                    tapSchemaRepository.insertSchema(schema);
              }
            }
        }
    }
}
