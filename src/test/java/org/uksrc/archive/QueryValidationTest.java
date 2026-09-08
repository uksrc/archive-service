package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.ivoa.dm.caom2.DerivedObservation;
import org.ivoa.dm.caom2.Observation;
import org.ivoa.dm.caom2.types.Point;
import org.junit.jupiter.api.*;
import org.xml.sax.InputSource;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.ByteArrayDataSource;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOTableBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;

import static io.restassured.config.RestAssuredConfig.newConfig;
import static io.restassured.config.XmlConfig.xmlConfig;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.uksrc.archive.utils.Utilities.TEST_READER_ROLE;
import static org.uksrc.archive.utils.Utilities.TEST_WRITER_ROLE;
import static org.uksrc.archive.utils.Utilities.TEST_USER;

/**
 * Intended for the use of testing the TAP ADQL service with queries.
 * <p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryValidationTest {

    @Inject
    ObservationResource observationResource;

    @Inject
    EntityManager em;

    private static boolean dataLoaded = false;

    private static final String TAP_QUERY = "/tap/sync?LANG=ADQL&QUERY=";

    //TODO - make sure FORMAT is still accepted even though it's deprecated
    //TODO - RESPONSEFORMAT instead for CSV/TAP
    /**
     * Unfortunately, BeforeAll & BeforeEach cannot be used to add an Observation due to security and API startup timing.
     * Needs to be called before ALL tests, so testing an individual test would require a called to SetupData() first.
     */
    @Test
    @Order(1)
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    void setupData() {
        if (!dataLoaded) {
            try {
                String xml = Files.readString(Paths.get("testing/observation1.xml"));

                JAXBContext jaxbContext = JAXBContext.newInstance(DerivedObservation.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                Object result = unmarshaller.unmarshal(new StringReader(xml));

                Observation obs;
                if (result instanceof JAXBElement<?> jaxbElement) {
                    obs = (Observation) jaxbElement.getValue();
                } else {
                    obs = (Observation) result;
                }
                observationResource.addObservation(obs);

                dataLoaded = true;
            }catch(Exception e) {
                System.out.println(e.getMessage());
                fail();
            }
        }
    }

    @Test
    public void testSyncQuery() {
        given()
                .formParam("QUERY", "select * from TAP_SCHEMA.columns")
                .when().post("/tap/sync")
                .then()
                .log().body()
                .statusCode(200); //TODO validate the VOTable
    }

    @Test
    public void testSyncQuery2() {
        given()
                .formParam("QUERY", "select * from TAP_SCHEMA.columns")
                .when().get("/tap/sync")
                .then()
                .log().body()
                .statusCode(200); //TODO validate the VOTable
    }

    @AfterAll
    @Transactional
    public void clearDatabase() {
        // Clear the table(s)
        em.createQuery("DELETE FROM Artifact").executeUpdate();
        em.createQuery("DELETE FROM Plane").executeUpdate();
        em.createQuery("DELETE FROM Observation").executeUpdate();
    }

    @Test
    @DisplayName("Select the standard TAP_SCHEMA tables")
    public void testSchemas() {
        String request = TAP_QUERY + "SELECT * FROM TAP_SCHEMA.tables";
        Response res = queryRequest(request);
        res.then()
                .statusCode(OK.getStatusCode())
                .body("VOTABLE.RESOURCE.INFO.find {it.@name == 'QUERY'}.@value",
                        equalTo("SELECT * FROM TAP_SCHEMA.tables"));
    }

    @Test
    @DisplayName("Distance with individual values (No Points)")
    public void testDistance() {
        String request = TAP_QUERY + "SELECT * FROM \"Point\" as po WHERE DISTANCE(po.cval1, po.cval2, 150.0, 2.5) < 90.0;";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(2, table.getRowCount());
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("BOX with individual values and no geometric co-ord system defined")
    public void testBox() {
        String request = TAP_QUERY + "SELECT * FROM \"Point\" as po WHERE 1 = CONTAINS(POINT(cval1, cval2), BOX(190.0, 56.0, 10.0, 10.0));";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(1, table.getRowCount());

        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("CIRCLE with individual values and no geometric co-ord system defined")
    public void testCircle() {
        String request = TAP_QUERY + "SELECT * FROM \"Point\" as po WHERE CONTAINS(POINT(po.cval1, po.cval2), CIRCLE(195.0, 57.0, 5.0)) = 1;";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(2, table.getRowCount());

            List<String> columnNames = IntStream.range(0, table.getColumnCount())
                    .mapToObj(i -> table.getColumnInfo(i).getName().toLowerCase())
                    .collect(Collectors.toList());

            assertThat(columnNames, containsInAnyOrder("cval1", "cval2", "id", "polygon_id"));
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("POLYGON with individual values and no geometric co-ord system defined")
    public void testPolygon() {
        String request =  TAP_QUERY + "SELECT * FROM \"Point\" as po WHERE 1 = CONTAINS(POINT(po.cval1, po.cval2), POLYGON(180.0, 50.0, 200.0, 50.0, 200.0, 70.0, 180.0, 70.0));";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(2, table.getRowCount());

            List<String> columnNames = IntStream.range(0, table.getColumnCount())
                    .mapToObj(i -> table.getColumnInfo(i).getName().toLowerCase())
                    .collect(Collectors.toList());

            assertThat(columnNames, containsInAnyOrder("cval1", "cval2", "id", "polygon_id"));
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("POLYGON with individual values and no geometric co-ord system defined (no results)")
    public void testPolygonFailure() {
        String request = TAP_QUERY +  "SELECT * FROM \"Point\" as po WHERE 1 = CONTAINS(POINT(po.cval1, po.cval2), POLYGON(100.0, 50.0, 110.0, 50.0, 110.0, 70.0, 100.0, 70.0));";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(0, table.getRowCount());

            List<String> columnNames = IntStream.range(0, table.getColumnCount())
                    .mapToObj(i -> table.getColumnInfo(i).getName().toLowerCase())
                    .collect(Collectors.toList());

            assertThat(columnNames, containsInAnyOrder("cval1", "cval2", "id", "polygon_id"));
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("REGION with POINT")
    public void testPolygonWithPoint() {
        String request = TAP_QUERY + "SELECT * FROM \"Point\" as po WHERE 1 = CONTAINS(POINT(po.cval1, po.cval2), REGION('CIRCLE ICRS 190.0 50.0 7.0'));";
        double expectedCval1 = 193.109524583333;        //Must match the value from the test file (observation1.xml)
        double tolerance = 0.00001;

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(1, table.getRowCount());

            List<String> columnNames = IntStream.range(0, table.getColumnCount())
                    .mapToObj(i -> table.getColumnInfo(i).getName().toLowerCase())
                    .collect(Collectors.toList());

            assertThat(columnNames, containsInAnyOrder("cval1", "cval2", "id", "polygon_id"));
            assertThat(((Number) table.getCell(0, 2)).doubleValue(), closeTo(expectedCval1, tolerance));
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("REGION (with an incorrect geometric co-ord system defined) with POINT")
    public void testRegionWithIncorrectGeometricCoOrd() {
        String request = TAP_QUERY + "SELECT * FROM \"Point\" WHERE CONTAINS(POINT(180.0, 0.0), REGION('CIRCLE MADE_UP 180.0 0.0 5.0')) = 1;";

        Response res = queryRequest(request);
        assertThat(res.asString(), containsString("Unsupported region serialization"));
    }

    @Test
    @DisplayName("Cone search using Distance")
    public void testConeSearchWithDistance() {
        String request = TAP_QUERY + "SELECT * FROM \"Point\" as po WHERE DISTANCE(po.cval1, po.cval2, 180.0, 0.0) < 2.5";

        //Expected miss
        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(0, table.getRowCount());

            List<String> columnNames = IntStream.range(0, table.getColumnCount())
                    .mapToObj(i -> table.getColumnInfo(i).getName().toLowerCase())
                    .collect(Collectors.toList());

            assertThat(columnNames, containsInAnyOrder("cval1", "cval2", "id", "polygon_id"));
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Cone search using Distance 2")
    public void testConeSearchWithDistance2() {
        String request = TAP_QUERY + "SELECT * FROM \"Point\" as po WHERE DISTANCE(po.cval1, po.cval2, 195.5, 56.0) < 2.5";
        double expectedCval2 = 56.57208;        //Must match the value from the test file (observation1.xml)
        double tolerance = 0.00001;

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(2, table.getRowCount());

            List<String> columnNames = IntStream.range(0, table.getColumnCount())
                    .mapToObj(i -> table.getColumnInfo(i).getName().toLowerCase())
                    .collect(Collectors.toList());

            assertThat(columnNames, containsInAnyOrder("cval1", "cval2", "id", "polygon_id"));
            assertThat(((Number) table.getCell(0, 3)).doubleValue(), closeTo(expectedCval2, tolerance));
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Attempt at a crossmatch using Distance")
    public void testCrossMatchWithDistance() {
        String request = TAP_QUERY + "SELECT a.id AS src_id, b.id AS cat_id, DISTANCE(POINT(a.cval1, a.cval2), POINT(b.cval1, b.cval2)) AS dist_deg " +
                "FROM \"Point\" AS a JOIN \"Point\" AS b ON a.id != b.id AND DISTANCE(POINT(a.cval1, a.cval2), POINT(b.cval1, b.cval2)) < 10.0;";
        double expectedCval2 = 1.8791494589752562;
        double tolerance = 0.00001;

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(2, table.getRowCount());

            List<String> columnNames = IntStream.range(0, table.getColumnCount())
                    .mapToObj(i -> table.getColumnInfo(i).getName())
                    .collect(Collectors.toList());

            assertThat(columnNames, containsInAnyOrder("src_id", "cat_id", "dist_deg"));
            assertThat(((Number) table.getCell(0, 2)).doubleValue(), closeTo(expectedCval2, tolerance));
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    //IN_UNIT doesn't seem to accept 'arcsec', 'deg', etc. at the moment
  /*  @Test
    @DisplayName("Convert the unit returned")
    public void testConvertUnit() {
        String request = String.format(TAP_QUERY, "JSON") + "SELECT * FROM \"point\" WHERE IN_UNIT(DISTANCE(POINT(ra, dec), POINT(180.0, 0.0)), 'arcsec') < 10.0";

        Response res = queryRequest(request);
        res.then()
                .statusCode(OK.getStatusCode())
    }*/

    @Test
    @DisplayName("Return first non-null value")
    public void testCoalesce() {
        String request = TAP_QUERY + "SELECT COALESCE(collection, 'Unknown') FROM Observation;";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(1, table.getRowCount());

            Object cellValue = table.getCell(0, 0);
            assertEquals("EMERLIN", cellValue);
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Cast to a double value from ?")
    public void testDoubleCast() {
        String request = TAP_QUERY + "SELECT CAST(contentLength AS DOUBLE) AS doubleValue FROM Artifact;";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertTrue(table.getRowCount() > 0);

            Object cellValue = table.getCell(0, 0);
            assertInstanceOf(Number.class, cellValue);
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Cast to varchar value from a numeric value")
    public void testVarcharCast() {
        String request = TAP_QUERY + "SELECT CAST(contentLength AS VARCHAR) AS charValue FROM Artifact;";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertTrue(table.getRowCount() > 0);

            Object cellValue = table.getCell(0, 0);
            assertInstanceOf(String.class, cellValue);
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("UNION of both tables, removing duplicates")
    public void testUnionNoDuplicates() {
        String request = TAP_QUERY + "SELECT a.cval1 AS ra, a.cval2 AS dec FROM caom2.\"Point\" AS a" +
                " UNION SELECT b.cval1 AS ra, b.cval2 AS dec FROM \"Point\" AS b;";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(2, table.getRowCount());
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("INTERSECTion of both tables, retuning rows that only appear in both results")
    public void testIntersectingTables() {
        String request = TAP_QUERY + "SELECT ID FROM \"Shape\" INTERSECT SELECT ID FROM \"Point\";";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(2, table.getRowCount());
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("EXCEPTion of first table, retuning rows that only appear in first result")
    public void testExceptionTable() {
        String request = TAP_QUERY + "SELECT ID FROM Shape EXCEPT SELECT id FROM \"Point\";";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(0, table.getRowCount());
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Chaining of SET operators")
    public void testChaining() {
        String request = TAP_QUERY + "(SELECT id, cval1, cval2 FROM \"Point\" UNION SELECT id, dimension_naxis1, dimension_naxis2 FROM \"Position\") EXCEPT SELECT id, dimension_naxis1, dimension_naxis2 FROM \"Position\";";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(2, table.getRowCount());

            try (RowSequence rseq = table.getRowSequence()) {
                assertTrue(rseq.next());
                assertEquals(2L, rseq.getCell(0));
            }
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("ORDER_BY a specific qualified column")
    public void testOrderingByAQualifiedColumn() {
        String request = TAP_QUERY + "SELECT contentType, contentLength FROM artifact ORDER BY contentLength;";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            long rowCount = table.getRowCount();
            boolean isSorted = true;

            for (long i = 0; i < rowCount - 1; i++) {
                // Use Number to safely handle both Integer and Long VOTable datatypes
                Number lower = (Number) table.getCell(i, 1);
                Number upper = (Number) table.getCell(i + 1, 1);

                // Check ascending sort order (default)
                if (lower.longValue() > upper.longValue()) {
                    isSorted = false;
                    break;
                }
            }
            assertTrue(isSorted);
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("ORDER_BY with an expression")
    public void testOrderingWithAnExpression() {
        String request = TAP_QUERY + "SELECT contentType, contentLength, (contentLength / 10) AS sort_key FROM Artifact ORDER BY sort_key DESC;";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        //Check each value in the second column in the response is sorted (descending) and that
        //the expression has been performed.
        try (StarTable table = parseVOTable(res.asString())) {
            long rowCount = table.getRowCount();

            for (long i = 0; i < rowCount - 1; i++) {
                Integer currValue = (Integer) table.getCell(i, 1);
                Integer nextValue = (Integer) table.getCell(i + 1, 1);

                // Assert descending order (current >= next)
                assertTrue(currValue >= nextValue);
            }
        } catch (IOException e) {
            fail("Failed to parse VOTable: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Commments inside a query")
    public void testComments() {
        String request = TAP_QUERY + "-- A header comment\n" +
                "      SELECT * -- a sub-comment\n" +
                "      FROM Plane;";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(6, table.getRowCount());
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Simple use of a string literal including an enclosed quote")
    public void testStringLiteral() {
        String request = TAP_QUERY + "SELECT * FROM algorithm WHERE name = 'correlator';";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        //1 VOTable with 1 row and it's contents should be '1, correlator'
        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(1, table.getRowCount());

            try (RowSequence rseq = table.getRowSequence()) {
                assertTrue(rseq.next());
                assertEquals(1L, rseq.getCell(0));
                assertEquals("correlator", rseq.getCell(1));
                assertFalse(rseq.next());
            }
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Geometric functions MUST use string literals")
    public void testGeometricStringLiteral() {
        String request = TAP_QUERY + "SELECT 1 FROM \"Point\" as po WHERE CONTAINS(POINT('ICRS', po.cval1, po.cval2), CIRCLE('ICRS', 195.0, 56.0, 5.0)) = 1;";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        //1 VOTable with 1 row and it's initial column should be '1'
        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(2, table.getRowCount());

            try (RowSequence rseq = table.getRowSequence()) {
                assertTrue(rseq.next());
                assertEquals(1, rseq.getCell(0));
            }
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Accept NULL as a valid expression value")
    public void testNullExpressionValue() {
        String request = TAP_QUERY + "SELECT * FROM \"Point\" WHERE POLYGON_ID IS NULL;";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        //1 VOTable with 2 rows and one of each's entries in a null value
        //<TR><TD>1</TD><TD></TD><TD>193.109524583333</TD><TD>56.57208</TD></TR>
        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(2, table.getRowCount());

            try (RowSequence rseq = table.getRowSequence()) {
                assertTrue(rseq.next());
                assertEquals(1L, rseq.getCell(0));
                assertTrue(rseq.next());
                assertEquals(2L, rseq.getCell(0));
                assertNull(rseq.getCell(1));
            }
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Accept NULL as a valid expression value in COALESCE")
    public void testNullCoalesceExpressionValue() {
        String request = TAP_QUERY + "SELECT COALESCE(calibration_namespace, NULL, 'No namespace defined') AS namespace FROM \"Position\";";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(2, table.getRowCount());

            try (RowSequence rseq = table.getRowSequence()) {
                assertTrue(rseq.next());
                assertEquals("No namespace defined", rseq.getCell(0));
            }
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("COORDSYS usage as a deprecated method. (Remove when usage finally removed)")
    public void testDeprecatedMethod() {
        String request = TAP_QUERY + "SELECT POINT('ICRS', 25.0, -19.5) from \"Point\";";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        System.out.println(res.asString());
        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(2, table.getRowCount());

            try (RowSequence rseq = table.getRowSequence()) {
                assertTrue(rseq.next());
                assertEquals("(0.4363323129985824 , -0.34033920413889424)", rseq.getCell(0));
            }
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    /** Optional Features - Start */

    @Test
    @DisplayName("UPPER and LOWER for values")
    public void testCapitalisationMethods() {
        String request = TAP_QUERY + "SELECT UPPER(name) AS name_upper, LOWER(name) AS name_lower FROM Telescope;";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(1, table.getRowCount());

            Object upper = table.getCell(0, 0);
            assertEquals("E-MERLIN", upper);
            Object lower = table.getCell(0, 1);
            assertEquals("e-merlin", lower);
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("UPPER against a real value")
    public void testMatchingAnUpper() {
        String request =  TAP_QUERY + "SELECT * FROM algorithm WHERE UPPER(name) = 'CORRELATOR';";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(1, table.getRowCount());

            Object upper = table.getCell(0, 1);
            assertEquals("correlator", upper);
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("LOWER against a value that doesn't match")
    public void testMatchingANonExistentLower() {
        String request = TAP_QUERY + "SELECT * FROM algorithm WHERE LOWER(name) = 'SUBARU';";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(0, table.getRowCount());
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("ILIKE to perform a case-insentive match with wildcards/patterns")
    public void testWildcardsWithILike() {
        String request = TAP_QUERY + "SELECT * FROM Provenance WHERE name ILIKE 'eMERL%';";

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());
        System.out.println(res.asString());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(5, table.getRowCount());

            String actualValue = (String) table.getCell(0, 1);
            assertThat(actualValue, startsWith("eMERLIN"));
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    @Test
    @Disabled("The TAP service currently returns an error") // TODO
    @DisplayName("WITH to create a temporary value to use as a subquery")
    public void testTemporarySubquery() {
        String request = TAP_QUERY + "WITH \"Point\" AS (SELECT * FROM Artifact WHERE MOD(contentLength,10) = 0) SELECT cval1, cval2 FROM \"Point\" WHERE cval1 > 10 AND cval2 < 100";
        double expectedCval2 = 56.57208;        //Must match the value from the test file (observation1.xml)
        double tolerance = 0.00001;

        Response res = queryRequest(request);
        res.then().statusCode(OK.getStatusCode());

        try (StarTable table = parseVOTable(res.asString())) {
            assertEquals(2, table.getRowCount());

            List<String> columnNames = IntStream.range(0, table.getColumnCount())
                    .mapToObj(i -> table.getColumnInfo(i).getName().toLowerCase())
                    .collect(Collectors.toList());

            assertThat(columnNames, containsInAnyOrder("cval1", "cval2", "id", "polygon_id"));
            assertThat(((Number) table.getCell(0, 2)).doubleValue(), closeTo(expectedCval2, tolerance));
        } catch (IOException e) {
            fail("Failed to parse VOTable response: " + e.getMessage());
        }
    }

    /**
     * Perform an AQDL query.
     * All queries are performed against the localhost instance of the archive-service.
     * @param query The ADQL query (NOT URL encoded)
     * @return The raw response from the target
     */
    private Response queryRequest(String query) {
        return given()
              //  .contentType("application/xml")
                .when()
                .get(query)
                .andReturn();
    }

    private StarTable parseVOTable(String xml) throws IOException {
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {

            return new StarTableFactory()
                    .makeStarTable(in, new VOTableBuilder());
        }
    }
}
