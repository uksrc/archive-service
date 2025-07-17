# archive-service

1. [Example resources](#examples)
2. [Submission and retrieval endpoints (REST APIs)](#endpoints)
3. [Tap service](#tapservice)
4. [Authentication](#authentication)


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
<a id="endpoints"></a>
### REST API details
Details of the functionality of the archive-service endpoints.

#### Retrieving observations

<details>
 <summary><code>GET</code> <code><b>/observations</b></code> <code>(Returns either all of the observations OR a paginated subset if optional page and size parameters supplied)</code></summary>

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
>  curl -X 'GET' -H 'accept: application/xml' 'http://localhost:8080/observations'
> ```

</details>

<details>
 <summary><code>GET</code> <code><b>/observations/{observationId}</b></code> <code>(Returns an Observation with the supplied ID, if found)</code></summary>

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
>  curl -X 'GET' 'http://localhost:8080/observations/23456' -H 'accept: application/xml'
> ```

</details>



------------------------------------------------------------------------------------------

#### Adding new Observations

<details>
 <summary><code>POST</code> <code><b>/observations</b></code> <code>(Add a new observation)</code></summary>

##### Responses

> | http code     | content-type      | response                                                        |
> |---------------|-------------------|-----------------------------------------------------------------|
> | `201`         | `application/xml` | `Observation added successfully, body contains new Observation` |
> | `400`         | `text/plain`      | `{"code":"400","message":"Bad Request"}`                        |

##### Example XML cURL

> ```
>  curl -X 'POST' -H 'Content-Type: application/xml' -H 'accept: application/xml' --data "@observation2.json" http://localhost:8080/observations
> ```

##### Example JSON cURL
with JSON response also
> ```
>  curl -X 'POST' -H 'Content-Type: application/json' -H 'accept: application/json' --data "@observation2.json" http://localhost:8080/observations
> ```
</details>

------------------------------------------------------------------------------------------

#### Updating observations

<details>
 <summary><code>UPDATE</code> <code><b>/observations/{observationId}</b></code> <code>(Updates an observation (Simple or Derived) with the same observationId)</code></summary>

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
>  curl -X 'PUT' -H 'Content-Type: application/xml' -H 'Accept: application/xml' -T observation2.xml http://localhost:8080/observations/988
> ```

</details>

------------------------------------------------------------------------------------------

#### Deleting Observations

<details>
 <summary><code>DELETE</code> <code><b>/observations/{observationId}</b></code> <code>(Delete an Observation with the supplied ID, if found)</code></summary>

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
>  curl -X 'DELETE' 'http://localhost:8080/observations/delete/123' -H 'accept: */*'
> ```

</details>  

------------------------------------------------------------------------------------------

#### Retrieving collections

<details>
 <summary><code>GET</code> <code><b>/collections</b></code> <code>(Returns the names of all the collections as a TSV (Tab Separated List))</code></summary>

##### Responses

> | http code | content-type | response                                 |
> |-----------|--------------|------------------------------------------|
> | `200`     | `text/plain` | `Returned successfully`                  |
> | `400`     | `text/plain` | `{"code":"400","message":"Bad Request"}` |


##### Example cURL

> ```
>  curl -X 'GET' -H 'accept: application/xml' 'http://localhost:8080/collections'
> ```

</details>

------------------------------------------------------------------------------------------
<a id="tapservice"></a>
## Tap Service

### TAP SCHEMA setup

The TAP schema is currently added to the database with the import.sql file which runs automatically on startup and adds the required tables to the database.
This is followed by a bean (TapSchemaPopulator) that will read the database and add each type (Observation etc.) to the generated TAP schema entries.

TODO: Generate a VO-DML/XSD model definition so that the TAP schema entries can be auto-added in the same way as the CAOM library.

### TAP service usage

Navigate to the <host>/tap endpoint (http://localhost:8080/tap for example), the host is the root of the archive-service.

This displays the default [Vollt](http://cdsportal.u-strasbg.fr/taptuto/gettingstarted_file.html#firststart) user interface and displays links to the standard utilities:

- async
- tables 
- capabilities
- examples
- availability
- sync

along with a textbox to run experimental queries.

### Deployment Settings
Update ``resources/templates/tap.properties.template`` as required. Any properties can be 'imported' from application.properties
if required using ${value}

One setting that may need changing is ``file_root_path`` it should resolve to a local folder (Windows requires full path too)

``file_root_path = /some/linux/path``

### Testing
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
```xml
quarkus.oidc.auth-server-url=https://your-oidc-service.url
quarkus.oidc.client-id=${OIDC_CLIENT_ID}
quarkus.oidc.credentials.secret=${OIDC_CLIENT_SECRET}
```

**Warning:** OIDC_CLIENT_SECRET & OIDC_CLIENT_ID are expected to set as environment variables so they don't have to appear in the application itself.

Restrict the APIs with the desired group(s)

Change the test group ``prototyping-groups/mini-src`` with the group your users need to be a member of.
```java
@RolesAllowed("prototyping-groups/mini-src")
```

#### Getting a token
1. Retrieve an authentication code
    ```shell
    curl "https://ska-iam.stfc.ac.uk/authorize?response_type=code&client_id=${OIDC_CLIENT_ID}&redirect_uri=http://localhost:8080/auth-callback&audience=authn-api&scope=openid+profile+offline_access&state=yQRL_ZdyAgTLv1H2sXI6w-THqDcqvlM3ulAlyfhB"
    ```

   - The ``redirect_uri`` has to be a service that receives two strings (code & state)
   - The ``state`` value is a string that represents this task and can be used to validate in the ``redirect_uri`` method.
   - Will redirect to the OIDC login screen of your provider via a web browser (unlikely to work when running curl from the command line)


2. Handle the response

    Create a method that follows this signature in the language that you are using.
    ```java
    // Running in http://localhost:8080/auth-callback for the above curl request
    @GET
    public Response handleOAuthCallback(@QueryParam("code") String code, @QueryParam("state") String state) {
        // Handle auth code and state as required
    }
    ```
  
 
3. Generate a bearer token

    ```shell
    curl -X POST https://ska-iam.stfc.ac.uk/token -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=authorization_code" -d "code=<YOUR_AUTH_CODE>" -d  "client_id=${OIDC_CLIENT_ID}" -d "client_secret=${OIDC_CLIENT_SECRET}" -d  "redirect_uri=http://localhost:8080/auth-callback"
    ```
   
    This should then return a JSON object that contains various values including the required ``access_token``. The access token can then be used as the bearer token when trying to access the Archive Service's APIs.


4. Use the bearer token to make a request

    ```shell
    curl.exe "http://localhost:8080/observations" -H "Authorization: Bearer <INSERT BEARER TOKEN>"
    ```
   
#### Test Cases
Location of CADC's test cases.

https://github.com/opencadc/caom2tools/tree/CAOM25/caom2/caom2/tests/data

<a id="authentication"></a>
## Authentication

A demonstration login page is supplied that will step through the OIDC approval steps at <host>/archive which is the root of the application.
This is disabled for production by default but can be enabled by disabling the *IfBuildProfile("dev")* settings in both LoginResource.java and AuthenticationResource.java 

#### application.properties values

Build-time settings only.

- *security.roles.enabled*: Security can be disabled (with a rebuild, intended for dev builds)


- *quarkus.oidc.auth-server-url*: the URI of the OIDC service
- *authentication.callback*: *redirectURI* to recieve the bearer token(s)
- *quarkus.oidc.client-id*: The client ID of the client registered at *quarkus.oidc.auth-server-url*
- *quarkus.oidc.credentials.secret*: The client secret of the client registered at *quarkus.oidc.auth-server-url*


- *resource.roles.view*: If *security.roles.enabled enabled* then these are the *quarkus.oidc.auth-server-url* groups that the user has to be a member of to **view/read** data endpoints.
- *resource.roles.edit*: If *security.roles.enabled enabled* then these are the *quarkus.oidc.auth-server-url* groups that the user has to be a member of to **view/edit** data endpoints.
