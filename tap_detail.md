## Details of the TAP Service
Any information regarding the current TAP service in use.

### TAP Properties
The Vollt service requires a file called tap.properties to be available at runtime, this supplies properties such as the URL of the database.

There is a template located at ``/src/main/resource/templates/tapProperties.txt`` which contains the required properties including some variabled ones. The variabled properties will be injected at build-time with values from the ``application.properties`` file. Once updated the generated ``tap.properties`` file will be deployed to ``/src/main/resources`` where it can be accessed by the Vollt service at run-time. 

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
reason that it's -1 that I'm unaware of.

