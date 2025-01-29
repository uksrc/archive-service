## Details of the TAP Service
Any information regarding the current TAP service in use.

### Running locally on minikube
1. Start minikube
2. Make sure the images created are added to minikube's docker repository and not to the host machine:
``` 
eval $(minikube docker-env)
Or on windows 
& minikube -p minikube docker-env | Invoke-Expression``
```
3. check the tap.properties template for suitable values. See [*TAP Properties*](#tap_properties)
4. clean and build the archive-service
```
gradle clean
gradle build -x test
```
5. Expose the archive service on the minikube cluster and the displayed localhost:<port> should resolve the service (just add /tap)
```
minikube service archive-service --url
```

### <a id="tap_properties"></a>TAP Properties
The Vollt service requires a file called tap.properties to be available at runtime, this supplies properties such as the URL of the database.

There is a template located at ``/src/main/resource/templates/tapProperties.txt`` which contains the required properties including some variabled ones. The variabled properties will be injected at build-time with values from the ``application.properties`` file. Once updated the generated ``tap.properties`` file will be deployed to ``/src/main/resources`` where it can be accessed by the Vollt service at run-time. 

Depedendent on the operating system that is being deployed on (mainly testing), the following property will need to be changed to the correct folder path:
``file_root_path = /some/linux/path`` This is where user data will be stored (such as generated VOTable output files). 

### Vollt Dependencies
The three libraries required to the TAP service are custom versions of Vollt and can be found here - https://github.com/slloyd-src/vollt. The individual JARs (adql, tap & uws) are built are embedded within the archive-service application under the ``lib`` folder.

### TAP_SCHEMA Population
``utils/tools/tap/TapSchemaPopulator.java`` Reads the database upon startup and adds any entries under ``public`` to the ``TAP_SCHEMA``. These entries will now be available to the TAP interface at run-time.
#### Notes
Some slight conversion of datatypes is required for columns, current list of converted types:
- ```"character varying"``` to ```"VARCHAR"```
- ```"timestamp"``` to ```"TIMESTAMP"```
- ```"bool"``` to ```"SMALLINT"```        Vollt TAP 1_0 doesn't support bool, publisher suggested using 0,1 SMALLINT until TAP1_1
- ```"double precision"``` to ```"DOUBLE"```

Any ARRAYs are added as ``size = -1`` NOT using ```arraysize``` until Vollt is updated to TAP1.1 and several bug fixes have been addressed.

### Vollt Tap Issues when running tap lint

#### 1 Section UPL: Make queries with table uploads
E-UPL-TMCX-1 Upload result column xtype mismatch timestamp != null
E-UPL-TMCX-2 Upload result column xtype mismatch timestamp != null

Vollt is TAP 1.0 (1.1 in development according to publisher) so column xtype shouldn't be evaluated here (1.0 uses column datatype instead).

#### 2 Section LOC: Test implementation of ObsLocTAP Data Model
F-LOC-NOTP-1 No table with name ivoa.obsplan

Obsplan not explicitly required for TAP 1.0 as it's for planning (could be added if required, would mean finding the definition somewhere to add to the TAP_SCHEMA though)

#### 3 Section CAP: Check TAP and TAPRegExt content of capabilities document
W-CAP-SVR0-1 No HTTP Server header

Will be OK once there's a reverse proxy etc. in front of it.

#### 4 Section QAS: Make ADQL queries in async mode
W-QAS-OLNG-1 Apparent success from bad query (unknown query language)

making a rest query with LANG=ADQL is the only supported query language in Vollt, the param has to be there but is always treated as ADQL regardless of the parameter value.
*SHOULD fail with something such as LANG=MADEUP according to TapLint
See src/tap/TapJob.java PARAM_LANGUAGE & LANG_ADQL

#### 5 Section UWS: Test asynchronous UWS/TAP behaviour
W-UWS-TFMT-1 Not recommended UWS V1.1 ISO-8601 form or empty string (missing trailing Z) "-1" from http://localhost:8080/tap/async/1736170285329/quote

-1 is the default string hard-coded into Vollt, I believe this is wrong and against the specification, I have emailed Gregory (Vollt publisher) to see if there's another
reason that it's -1 that I'm unaware of. Update: Gregory says it was fixed in 2018 - however the '-1' is still there in my testing. This might be worth ignoring until we decide
whether to continue using Vollt.


