# archive-service

Details of the functionality of the archive-service endpoints.

------------------------------------------------------------------------------------------
Example resources suitable for **minimal** testing (Mandatory properties only).
#### Example Simple Observation
Namespace details must conform with the current vo-dml model used.
```xml
<SimpleObservation xmlns:caom2="http://ivoa.net/dm/models/vo-dml/experiment/caom2"  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="caom2:caom2.SimpleObservation">
    <id>988</id>
    <collection>e-merlin</collection>
    <uri>auri</uri>
    <intent>science</intent>
</SimpleObservation>
```

*@type* has to be set for JSON
```json
{
  "@type": "caom2:caom2.SimpleObservation",
  "id": "myData12345",
  "collection": "test",
  "uri": "auri",
  "intent": "science"
}
```
#### Example Derived Observation
```xml
<DerivedObservation xmlns:caom2="http://ivoa.net/dm/models/vo-dml/experiment/caom2"  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="caom2:caom2.DerivedObservation">
    <id>10</id>
    <collection>test</collection>
    <uri>auri</uri>
    <intent>science</intent>
    <members>jbo-simple1</members>
    <members>jbo-simple2</members>
</DerivedObservation>
```
------------------------------------------------------------------------------------------
### REST API details  
Endpoints available for interaction with the archive-service.

#### Retrieving observations

<details>
 <summary><code>GET</code> <code><b>/observations</b></code> <code>(Returns either all of the observations OR a paginated subset if optional page and size parameters supplied)</code></summary>

##### Parameters

> | name | type     | data type | description                                                                    |
> |------|----------|-----------|--------------------------------------------------------------------------------|
> | page | optional | integer   | The page index, zero-indexed                                                   |
> | size | optional | integer   | The number of observations to return for each page, must be greater than zero. |


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

<details>
 <summary><code>GET</code> <code><b>/observations/collection/{collectionId}</b></code> <code>(Returns all observations for the supplied collectionId, if found)</code></summary>

##### Parameters

> | name         | type     | data type | description                                                                    |
> |--------------|----------|-----------|--------------------------------------------------------------------------------|
> | collectionId | required | String    | The unique identifier of a specific collection                                 |
> | page         | optional | integer   | The page index, zero-indexed                                                   |
> | size         | optional | integer   | The number of observations to return for each page, must be greater than zero. |


##### Responses

> | http code | content-type      | response                                                                      |
> |-----------|-------------------|-------------------------------------------------------------------------------|
> | `201`     | `application/xml` | `List of Observation (Simple and/or Derived) found and returned successfully` |
> | `400`     | `text/plain`      | `{"code":"400","message":"Bad Request"}`                                      |

##### Example cURL

> ```
>  curl -X 'GET' 'http://localhost:8080/observations/23456' -H 'accept: application/xml'
> ```

</details>

------------------------------------------------------------------------------------------

#### Adding new Observations

<details>
 <summary><code>PUT</code> <code><b>/observations/add</b></code> <code>(Add a new observation)</code></summary>

##### Responses

> | http code     | content-type      | response                                                        |
> |---------------|-------------------|-----------------------------------------------------------------|
> | `201`         | `application/xml` | `Observation added successfully, body contains new Observation` |
> | `400`         | `text/plain`      | `{"code":"400","message":"Bad Request"}`                        |

##### Example XML cURL

> ```
>  curl -v --header "Content-Type: application/xml" -T observation1.xml http://localhost:8080/observations/add
> ```

##### Example JSON cURL
with JSON response also
> ```
>  curl -v --header "Content-Type: application/json" --header "Accept: application/json" -T observation1.xml http://localhost:8080/observations/add
> ```
</details>

<details>
 <summary><code>PUT</code> <code><b>/observations/derived/add</b></code> <code>(Add a new derived observation)</code></summary>

##### Responses

> | http code     | content-type      | response                                                               |
> |---------------|-------------------|------------------------------------------------------------------------|
> | `201`         | `application/xml` | `Observation added successfully, body contains new DerivedObservation` |
> | `400`         | `text/plain`      | `{"code":"400","message":"Bad Request"}`                               |

##### Example cURL

> ```
>  curl -v --header "Content-Type: application/xml" -T observation1.xml http://localhost:8080/observations/derived/add
> ```

</details>

------------------------------------------------------------------------------------------

#### Updating observations

<details>
 <summary><code>POST</code> <code><b>/observations/update/{observationId}</b></code> <code>(Updates an observation (Simple or Derived) with the same observationId)</code></summary>

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
>  curl -v --header "Content-Type: application/xml" -T observation123.xml http://localhost:8080/observations/update/123
> ```

</details>

------------------------------------------------------------------------------------------

#### Deleting Observations

<details>
 <summary><code>GET</code> <code><b>/observations/delete/{observationId}</b></code> <code>(Delete an Observation with the supplied ID, if found)</code></summary>

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
 <summary><code>GET</code> <code><b>/observations/collections</b></code> <code>(Returns the names of all the collections as a TSV (Tab Separated List))</code></summary>

##### Responses

> | http code | content-type | response                                 |
> |-----------|--------------|------------------------------------------|
> | `200`     | `text/plain` | `Returned successfully`                  |
> | `400`     | `text/plain` | `{"code":"400","message":"Bad Request"}` |


##### Example cURL

> ```
>  curl -X 'GET' -H 'accept: application/xml' 'http://localhost:8080/observations/collections'
> ```

</details>