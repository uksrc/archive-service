# archive-service

1. [Example resources](#examples)
2. [Submission and retrieval endpoints (REST APIs)](#rest-api-details)
3. [Tap service](#tap-service)
4. [Authentication](#authentication)
5. [DataLink](#datalink)
6. [Spherical Queries](#spherical-queries)
7. [Test Cases](#test-cases)


------------------------------------------------------------------------------------------
### Examples
<a id="examples"></a>
Example resources suitable for **minimal** testing (Mandatory properties only).

#### Example Simple Observation
Namespace details must conform with the current vo-dml model used.
```xml
<SimpleObservation xmlns:caom2="http://ivoa.net/dm/models/vo-dml/experiment/caom2"  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="caom2:SimpleObservation">
    <collection>e-merlin</collection>
    <uri>urn:obs:jbo:20170801:obs1</uri>    //Must be unique
    <intent>science</intent>
</SimpleObservation>
```

*@type* has to be set for JSON
```json
{
  "@type": "caom2:caom2.SimpleObservation",
  "collection": "test",
  "uri": "urn:obs:jbo:20170801:obs1",
  "intent": "science"
}
```
#### Example Derived Observation
```xml
<DerivedObservation xmlns:caom2="http://ivoa.net/dm/models/vo-dml/experiment/caom2"  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="caom2:DerivedObservation">
    <collection>test</collection>
    <uri>urn:obs:jbo:20170801:obs1</uri>
    <intent>science</intent>
    <members>jbo-simple1</members>
    <members>jbo-simple2</members>
</DerivedObservation>
```
------------------------------------------------------------------------------------------
### REST API details
Details of the functionality of the archive-service endpoints.

#### Retrieving observations

<details>
 <summary><code>GET</code> <code><b>/archive/observations</b></code> <code>(Returns either all of the observations OR a paginated subset if optional page and size parameters supplied)</code></summary>

##### Parameters

> | name         | type     | data type | description                                                                    |
> |--------------|----------|-----------|--------------------------------------------------------------------------------|
> | collectionId | optional | String    | Filter by collection Id if required (not supplying will return all).           |
> | page         | optional | integer   | The page index, zero-indexed                                                   |
> | size         | optional | integer   | The number of observations to return for each page, must be greater than zero. |


##### Responses

> | http code | content-type      | response                                 |
> |-----------|-------------------|------------------------------------------|
> | `200`     | `application/xml` | `Returned successfully`                  |
> | `400`     | `text/plain`      | `{"code":"400","message":"Bad Request"}` |


##### Example cURL

> ```
>  curl -X 'GET' -H 'accept: application/xml' 'http://localhost:8080/archive/observations'
> ```

</details>

<details>
 <summary><code>GET</code> <code><b>/archive/observations/{observationId}</b></code> <code>(Returns an Observation with the supplied ID, if found)</code></summary>

##### Parameters

> | name          |  type     | data type | description                                                         |
> |---------------|-----------|-----------|---------------------------------------------------------------------|
> | observationId |  required | String    | The unique identifier of a specific Observation (Simple or Derived) |


##### Responses

> | http code | content-type      | response                                      |
> |-----------|-------------------|-----------------------------------------------|
> | `201`     | `application/xml` | `Observation found and returned successfully` |
> | `400`     | `text/plain`      | `{"code":"400","message":"Bad Request"}`      |
> | `404`     | `text/plain`      | Observation not found                         |

##### Example cURL

> ```
>  curl -X 'GET' 'http://localhost:8080/archive/observations/23456' -H 'accept: application/xml'
> ```

</details>



------------------------------------------------------------------------------------------

#### Adding new Observations

<details>
 <summary><code>POST</code> <code><b>/archive/observations</b></code> <code>(Add a new observation)</code></summary>

##### Responses

> | http code     | content-type      | response                                                        |
> |---------------|-------------------|-----------------------------------------------------------------|
> | `201`         | `application/xml` | `Observation added successfully, body contains new Observation` |
> | `400`         | `text/plain`      | `{"code":"400","message":"Bad Request"}`                        |

##### Example XML cURL

> ```
>  curl -X 'POST' -H 'Content-Type: application/xml' -H 'accept: application/xml' --data "@observation2.json" http://localhost:8080/archive/observations
> ```

##### Example JSON cURL
with JSON response also
> ```
>  curl -X 'POST' -H 'Content-Type: application/json' -H 'accept: application/json' --data "@observation2.json" http://localhost:8080/archive/observations
> ```
</details>

------------------------------------------------------------------------------------------

#### Updating observations

<details>
 <summary><code>UPDATE</code> <code><b>/archive/observations/{observationId}</b></code> <code>(Updates an observation (Simple or Derived) with the same observationId)</code></summary>

##### Parameters

> | name          |  type     | data type | description                                               |
> |---------------|-----------|-----------|-----------------------------------------------------------|
> | observationId |  required | String    | The unique identifier of a specific observation to update |


##### Responses

> | http code | content-type      | response                                 |
> |-----------|-------------------|------------------------------------------|
> | `200`     | `application/xml` | `Observation updated successfully`       |
> | `400`     | `text/plain`      | `{"code":"400","message":"Bad Request"}` |
> | `404`     | `text/plain`      | Observation not found                    |

##### Example cURL

> ```
>  curl -X 'PUT' -H 'Content-Type: application/xml' -H 'Accept: application/xml' -T observation2.xml http://localhost:8080/archive/observations/988
> ```

</details>

------------------------------------------------------------------------------------------

#### Deleting Observations

<details>
 <summary><code>DELETE</code> <code><b>/archive/observations/{observationId}</b></code> <code>(Delete an Observation with the supplied ID, if found)</code></summary>

##### Parameters

> | name          |  type     | data type | description                                                         |
> |---------------|-----------|-----------|---------------------------------------------------------------------|
> | observationId |  required | String    | The unique identifier of a specific Observation (Simple or Derived) |


##### Responses

> | http code | content-type      | response                                 |
> |-----------|-------------------|------------------------------------------|
> | `204`     | `application/xml` | `Observation deleted`                    |
> | `400`     | `text/plain`      | `{"code":"400","message":"Bad Request"}` |
> | `404`     | `text/plain`      | Observation not found                    |

##### Example cURL

> ```
>  curl -X 'DELETE' 'http://localhost:8080/archive/observations/delete/123' -H 'accept: */*'
> ```

</details>  

------------------------------------------------------------------------------------------

#### Retrieving collections

<details>
 <summary><code>GET</code> <code><b>/archive/collections</b></code> <code>(Returns the names of all the collections as a TSV (Tab Separated List))</code></summary>

##### Responses

> | http code | content-type | response                                 |
> |-----------|--------------|------------------------------------------|
> | `200`     | `text/plain` | `Returned successfully`                  |
> | `400`     | `text/plain` | `{"code":"400","message":"Bad Request"}` |


##### Example cURL

> ```
>  curl -X 'GET' -H 'accept: application/xml' 'http://localhost:8080/archive/collections'
> ```

</details>

------------------------------------------------------------------------------------------

#### Retrieving dataLink

<details>
 <summary><code>GET</code> <code><b>/archive/datalink/links</b></code> <code>(Returns the DataLink object for the observation supplied)</code></summary>

##### Parameters

> | name |  type     | data type | description                                                         |
> |------|-----------|-----------|---------------------------------------------------------------------|
> | ID   |  required | String    | The unique identifier of a specific Observation (Simple or Derived) |
> 

##### Responses

> | http code | content-type | response                                                                 |
> |-----------|--------------|--------------------------------------------------------------------------|
> | `200`     | `text/plain` | `Returned successfully`                                                  |
> | `500`     | `text/plain` | `{"code":"500","message":"Error, could not construct DataLink VOTable"}` |
>

##### Example cURL

> ```
>  curl -X 'GET' -H 'accept: application/xml' 'http://localhost:8080/archive/datalink/links?ID=2cf99e88-90e1-4fe8-a502-e5cafdc6ffa1'
> ```

</details>

------------------------------------------------------------------------------------------

### Tap Service

#### TAP SCHEMA setup

The TAP schema is currently added to the database with the import.sql file which runs automatically on startup and adds the required tables to the database.
This is followed by a bean (TapSchemaPopulator) that will read the database and add each type (Observation etc.) to the generated TAP schema entries.

TODO: Generate a VO-DML/XSD model definition so that the TAP schema entries can be auto-added in the same way as the CAOM library.

#### TAP service usage

Navigate to the <host>/tap endpoint (http://localhost:8080/tap for example), the host is the root of the archive-service.

This displays the default [Vollt](http://cdsportal.u-strasbg.fr/taptuto/gettingstarted_file.html#firststart) user interface and displays links to the standard utilities:

- async
- tables 
- capabilities
- examples
- availability
- sync

along with a textbox to run experimental queries.

#### Deployment Settings
Update ``resources/templates/tap.properties.template`` as required. Any properties can be 'imported' from application.properties
if required using ${value}

One setting that may need changing is ``file_root_path`` it should resolve to a local folder (Windows requires full path too)

``file_root_path = /some/linux/path``

#### Testing
Using [Stilts TapLint utility](https://www.star.bris.ac.uk/mbt/stilts/sun256/taplint.html), any issues can be highlighted.
```
java -jar .\stilts.jar taplint interface=tap1.0 tapurl=http://localhost:8080/tap 
```
Can be focused on a subset if required using the ```report``` parameter, see [Usages](https://www.star.bris.ac.uk/mbt/stilts/sun256/taplint-usage.html) 

Caution: Some of the TapLint tests seem to assume TAP 1.1 compliance and Vollt is currently 1.0. So double-check any issues with the specifications:

- [TAP 1.0](https://www.ivoa.net/documents/TAP/20100327/REC-TAP-1.0.html)
- [TAP 1.1](https://www.ivoa.net/documents/TAP/20190927/REC-TAP-1.1.html)

### Authentication
Using a OIDC controller, APIs are restricted to a specific group.

Update the application.properties as required (example below uses environment variables for the client ID and secret).
The ID and secret are available from the client registered on your auth-server.
```shell
quarkus.oidc.auth-server-url=https://your-oidc-service.url
quarkus.oidc.client-id=${OIDC_CLIENT_ID}
quarkus.oidc.credentials.secret=${OIDC_CLIENT_SECRET}
```

**Warning:** OIDC_CLIENT_SECRET & OIDC_CLIENT_ID are expected to set as environment variables so they don't have to appear in the application itself.

Restrict the APIs with the desired group(s)

Change the test group ``prototyping-groups/mini-src`` with the group your users need to be a member of.
```shell
@RolesAllowed("prototyping-groups/mini-src")
```

#### Getting a token
1. **Retrieve an authentication code**  

   ```shell
    curl "https://ska-iam.stfc.ac.uk/authorize?response_type=code&client_id=${OIDC_CLIENT_ID}&redirect_uri=http://localhost:8080/auth-callback&audience=authn-api&scope=openid+profile+offline_access&state=yQRL_ZdyAgTLv1H2sXI6w-THqDcqvlM3ulAlyfhB"
    ```
   
   - The ``redirect_uri`` has to be a service that receives two strings (code & state)
   - The ``state`` value is a string that represents this task and can be used to validate in the ``redirect_uri`` method.
   - Will redirect to the OIDC login screen of your provider via a web browser (unlikely to work when running curl from the command line)

2. **Handle the response**
    
    Create a method that follows this signature in the language that you are using.
    ```java
    // Running in http://localhost:8080/auth-callback for the above curl request
    @GET
    public Response handleOAuthCallback(@QueryParam("code") String code, @QueryParam("state") String state) {
        // Handle auth code and state as required
    }
    ```
   
3. **Generate a bearer token**
   
    ```shell
    curl -X POST https://ska-iam.stfc.ac.uk/token -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=authorization_code" -d "code=<YOUR_AUTH_CODE>" -d  "client_id=${OIDC_CLIENT_ID}" -d "client_secret=${OIDC_CLIENT_SECRET}" -d  "redirect_uri=http://localhost:8080/auth-callback"
    ```
      
    This should then return a JSON object that contains various values including the required ``access_token``. The access token can then be used as the bearer token when trying to access the Archive Service's APIs.

4. **Use the bearer token to make a request**
   
    ```shell
    curl.exe "http://localhost:8080/archive/observations" -H "Authorization: Bearer <INSERT BEARER TOKEN>"
    ```
   
#### Login Page
A demonstration login page is supplied that will step through the OIDC approval steps at <host>/archive which is the root of the application.
This is disabled for production by default but can be enabled by disabling the *IfBuildProfile("dev")* settings in both LoginResource.java and AuthenticationResource.java

#### application.properties values

Build-time settings only.

- *security.roles.enabled*: Security can be disabled (with a rebuild, intended for dev builds)


- *quarkus.oidc.auth-server-url*: the URI of the OIDC service
- *authentication.callback*: *redirectURI* to receive the bearer token(s)
- *quarkus.oidc.client-id*: The client ID of the client registered at *quarkus.oidc.auth-server-url*
- *quarkus.oidc.credentials.secret*: The client secret of the client registered at *quarkus.oidc.auth-server-url*


- *resource.roles.view*: If *security.roles.enabled enabled* then these are the *quarkus.oidc.auth-server-url* groups that the user has to be a member of to **view/read** data endpoints.
- *resource.roles.edit*: If *security.roles.enabled enabled* then these are the *quarkus.oidc.auth-server-url* groups that the user has to be a member of to **view/edit** data endpoints.

#### Environment variables

The following env vars are required to allow the IAM process to succeed.

- *OIDC_SERVER_URL*: The URL of the IAM service
- *OIDC_CLIENT_ID*: OIDC client ID of your registered client. Required for DEV builds.
- *OIDC_CLIENT_SECRET*: OIDC client secret of your registered client. Required for DEV builds.

- *OIDC_AUTH_CALLBACK*: URI of the service that handles authentication callbacks.

### DataLink

Retrieve a DataLink object for a specific Artifact.

```shell
<HOST>/archive/datalink/links?ID={Artifact.id}
```

Returns a VOTable that lists the resources available.
```shell
<VOTABLE xmlns:stc="http://www.ivoa.net/xml/STC/v1.30" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.ivoa.net/xml/VOTable/v1.3" version="1.3">
  <RESOURCE type="results">
    <TABLE>
      <FIELD arraysize="*" datatype="char" name="ID" ucd="meta.id;meta.main"/>
      <FIELD arraysize="*" datatype="char" name="access_url" ucd="meta.ref.url"/>
      <FIELD arraysize="*" datatype="char" name="service_def" ucd="meta.ref"/>
      <FIELD arraysize="*" datatype="char" name="error_message" ucd="meta.code.error"/>
      <FIELD arraysize="*" datatype="char" name="description" ucd="meta.note"/>
      <FIELD arraysize="*" datatype="char" name="semantics" ucd="meta.code"/>
      <FIELD arraysize="*" datatype="char" name="content_type" ucd="meta.code.mime"/>
      <FIELD arraysize="" datatype="long" name="content_length" ucd="phys.size;meta.file"/>
      <FIELD arraysize="*" datatype="char" name="content_qualifier" ucd="meta.code.class"/>
      <FIELD arraysize="*" datatype="char" name="local_semantics" ucd="meta.code"/>
      <FIELD arraysize="*" datatype="char" name="link_auth" ucd="meta.code"/>
      <FIELD arraysize="" datatype="boolean" name="link_authorized" ucd="meta.code"/>
      <!-- Custom properties for this service -->
      <FIELD arraysize="*" datatype="char" name="plane_id" ucd="meta.id;meta.id.assoc"/>
      <DATA>
        <TABLEDATA>
          <TR>
            <TD>55e4160d-033f-4fae-9c47-93ec1f3b8643</TD>
            <TD>http://localhost:8080/archive/datalink/resource/55e4160d-033f-4fae-9c47-93ec1f3b8643</TD>
            <TD/>
            <TD/>
            <TD/>
            <TD>auxiliary</TD>
            <TD>image/png</TD>
            <TD>264960</TD>
            <TD/>
            <TD/>
            <TD>true</TD>                                       <!-- ALL resources require authentication -->
            <TD>false</TD>                                      <!-- Needs updating when the WebUI can send current users -->
            <TD>05974947-4872-4169-8240-c31f235232fd</TD>
          </TR>
        </TABLEDATA>
      </DATA>
    </TABLE>
  </RESOURCE>
</VOTABLE>
```

This gives the access_url to resolve the resource associated with the supplied Artifact.id value as shown by value http://localhost:8080/archive/datalink/resource/55e4160d-033f-4fae-9c47-93ec1f3b8643 above.

Note: The /archive/datalink/resource API determines the actual Artifact.uri via the supplied Artifact.id value. Currently expected to be a file/http url to a fixed location.

The link_authorized property value needs updating once there's a mechanism in place to send the current user's status.

### Spherical Queries

The postgres database that is currently used has the [pgSphere extension](https://github.com/postgrespro/pgsphere) installed and can be used with the following approach. Hibernate 6+

1. [Register a new function](#register-a-new-function-postgresqldialect)
2. [Nominate the function](#nominate-the-function)
3. [Use the function](#use-the-function)

#### Register a new function (PostgreSQLDialect)

    This exposes pgSphere functionality to Hibernate for use in HQL queries (& ultimately in JPQL queries).
    ```
    import org.hibernate.boot.model.FunctionContributions;
    import org.hibernate.boot.model.FunctionContributor;
    import org.hibernate.dialect.PostgreSQLDialect;
    import org.hibernate.query.sqm.function.SqmFunctionRegistry;
    import org.hibernate.type.StandardBasicTypes;
    
    /**
    * Class to register pgSphere helper functions for Hibernate queries.
    */
    public class PgSphereDialect extends PostgreSQLDialect implements FunctionContributor {

      @Override
      public void contributeFunctions(FunctionContributions functionContributions) {
        SqmFunctionRegistry registry = functionContributions.getFunctionRegistry();
      
        var typeConfig = functionContributions.getTypeConfiguration();
        var doubleType = typeConfig.getBasicTypeRegistry()
                .resolve(StandardBasicTypes.DOUBLE);
                
        // Distance function x,y <-> x,y
        registry.registerPattern(
              "pgsphere_distance",
              "(spoint(radians(?1), radians(?2))::spoint <-> spoint(radians(?3), radians(?4))::spoint)",
              doubleType
        );
      }
    }
    ```

    As can be seen the function needs three parameters. 
    1. the function name for use when being called by HQL queries
    2. The function definition itself
    3. The return type

#### Nominate the function

Place a new file in *src\main\resources\META-INF\services* called *org.hibernate.boot.model.FunctionContributor* which contains the name of the class containing the new function (full package path required).
```
org.uksrc.archive.utils.query.PgSphereDialect
```

This allows Java's service loader to discover the function at build-time.

#### Use the function

Create a JPQL query that makes use of the function.
```
@PersistenceContext
protected EntityManager em;

String CONE_SEARCH_QUERY =
           "SELECT obs FROM Observation obs JOIN obs.targetPosition tp JOIN tp.coordinates p" +
                   " WHERE FUNCTION('pgsphere_distance', p.cval1, p.cval2, :ra, :dec) <= radians(:radiusInDegrees)";

//Use as normal
TypedQuery<Observation> query = em.createQuery(CONE_SEARCH_QUERY, Observation.class);
query.setParameter("ra", ra);
query.setParameter("dec", dec);
query.setParameter("radiusInDegrees", radius);

List<Observation> observations = query.getResultList();
```



## Test Cases
Location of CADC's test cases.

https://github.com/opencadc/caom2tools/tree/CAOM25/caom2/caom2/tests/data
