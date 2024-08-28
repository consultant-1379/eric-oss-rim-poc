ERICSSON © 2022

[Running the rApp on locally](#running-the-rapp-on-locally)  
[Starting and running the rApp](#starting-and-running-the-rapp)  
[Running the rApp on locally pointing to an EIAP cluster](#running-the-rapp-on-locally-pointing-to-an-eiap-cluster)  
[Running the rApp on EIAP Cluster](#running-the-rapp-on-eiap-cluster)  
[Observing the results](#observing-the-results)  
[Contributing the rApp](#contributing-the-rapp)  

# Running the rApp on locally

For loading the network information data, [opencsv library](https://opencsv.sourceforge.net/) has been used.  
&nbsp;

**MINIO And Connection To Object Store:**  

  RIM reads PM Counter ROP & Baseline data and CM and Weather data from locations in BDR / Object Store. To feed the rApp with this data, there should be a local minio running to connect to Object Store. To setup your minio, here are the steps:
  - Download the minio "server" to your local computer from [minio](https://min.io/download#) and launch with following commands: &nbsp;

   ```
   minio server [FLAGS] DIR1 [DIR2..]  

   ```
   where DIR points to a directory on a filesystem, that can be used by minio for (internal) file storage. &nbsp;

```
   Example: For Windows:  .\minio.exe server C:\minio --console-address :9090 &nbsp;

   Example: For Linux  :  ./minio server ~/minio --console-address :9090  
```
  - The minio server prints its output to the system console.
       - *The minio server process prints its output to the system console.*
         *Please connect to minIO through http://localhost:9000 and log in using the credentials* **minioadmin / minioadmin**  &nbsp;

  - CREATE BUCKET:  
        - Open Browser
        -  http://127.0.0.1:9000
             -  Login:  
             -  RootUser: minioadmin  
             -  RootPass: minioadmin

        - Create a new bucket using the "Create Bucket" option on the top right.  
        - Create a bucket named "rim" and hit "Create Bucket".    


  - UPLOAD REQUIRED DATA:
        - Once the bucket is created, hit "Upload" in the top right and then "Upload Folder".    
        - Then copy the related folders to minio bucket under **'rim'** folder. Sample format of object store necessary documents:  
            - [pm baseline](eric-oss-rim-poc-app/src/test/resources/pm/baseline/) csv file
            - [rop based pm](eric-oss-rim-poc-app/src/test/resources/pm/rop/) csv files
            - [rop based ducting](eric-oss-rim-poc-app/src/test/resources/geospatial/ducting/) geotiff files
       - Then make sub a directory under **'rim'** folder, called 'setup_files' and copy following files to it.
            - [cell allowlist](eric-oss-rim-poc-app/src/test/resources/setup_files/) list of cells the algorithm is allowed to mitigate (both victims and neighbors).
        - Then make sub directory under **'rim/setup_files'** folder, called 'cm' and copy following files to it.
            - [CM data files](eric-oss-rim-poc-app/src/test/resources/cm/) cm data files.

**UPDATE APPLICATION PARAMETERS:**   
  
  The following environment variables should be set up.

    - BDR_SERVICE=http://localhost:9000
    - BDR_ACCESS_KEY= < minio admin user >
    - BDR_SECRET_KEY= < minio password >
    - SPRING_DATASOURCE_EXPOSED=true
    - APP_DATA_CUSTOMERID=< customer ID, according to the PM data provided e.g. netsim01>
    - MITIGATION_CLOSED_LOOP_MODE=true
    - TROPODUCT_MAXLON=<adjust according to the geographical area> 
    - TROPODUCT_MAXLAT=adjust according to the geographical area> 
    - TROPODUCT_MINLON=<adjust according to the geographical area> 
    - TROPODUCT_MINLAT=<adjust according to the geographical area> 
    - CLUSTERING_MINIMUM_CONNECTED_EDGE_WEIGHT=<min connected edge weight value of customer>
    - SPRING_KAFKA_MODE_ENABLED=[true|false]
    - SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
    - SPRING_KAFKA_SCHEMA_REGISTRY=http://localhost:8081
    - APP_DATA_PM_CSV_USE_PRECALCULATED_AVG_DELTA_IPN=[true|false] #(Deprecated)
    - TROPODUCT_LOADFROMURL=[true|false]
    - TROPODUCT_BASEURLANDPATH=< url of the location of the weather data (.tif) if 'TROPODUCT_LOADFROMURL' is true, ignored if 'TROPODUCT_LOADFROMURL' is false, See section on 'Getting Weather Data'>
    - CELL_SELECTION_CONFIG_USE_WEATHER_DATA=true|false 
    

There is no need to change the [application.yaml](eric-oss-rim-poc-app/src/main/resources/application.yaml) file, spring will override the configuration from the environment according to the [relaxed binding](https://docs.spring.io/spring-boot/docs/2.0.x/reference/html/boot-features-external-config.html#boot-features-external-config-relaxed-binding) rules. However, it possible to achieve the same effect by altering the corresponding properties in [application.yaml](eric-oss-rim-poc-app/src/main/resources/application.yaml).

**Getting Weather Data (Predicted Ducting):** &nbsp;

There are three option to with respect to  weather data:

1) User can load data manually to 'RIM' bucket in object store under the 'geospatial/ducting/' directory.

2) Setup rim to pull the weather data directly from a specified URL.

3) User can opt 'not' to use weather data for RIM

For option 1, set the application parameter 'cell-selection-config.use-weather-data=true' and 'tropoduct.loadFromUrl=false'

For option 2, set the application parameter 'cell-selection-config.use-weather-data=true' and 'tropoduct.loadFromUrl=true' and 'tropoduct.baseUrlAndPath' equal to URL of the location of the weather data (.tif).

For option 3, set the application parameter 'cell-selection-config.use-weather-data=false'

Note: 'tropoduct.loadFromUrl' corresponds to the environment parameter 'TROPODUCT_LOADFROMURL'

Note: 'tropoduct.baseUrlAndPath' corresponds to the environment parameter 'TROPODUCT_BASEURLANDPATH'

Note: 'cell-selection-config.use-weather-data=true' corresponds to the environment parameter 'CELL_SELECTION_CONFIG_USE_WEATHER_DATA=true'

Note for options 1 && 2 the weather data corresponding to the ROPs being serviced by RIM must exist in the specified location. The weather data should be produced in a 3 hourly intervals of 00, 03, 06, 09, 12, 15 18, 21 hours for each day. 

For option 1, the file is stored in '.tif' files in the format of 'ww-<EPOCH timestamp in mS>.tif. So a ROP with an equivalent EPOCH of '1684800900000' will use weather data from the file named ww-1684800000000.tif.

For option 2, the file is stored in '.tif' files in the format of 'wwDD-HH.tif where DD == day and HH == hour. So a ROP with an equivalent EPOCH of '1684800900000' will use weather data with an EPOCH of '1684800000000' and so will fetch a file file named ww-23-00.tif

RIM will choose the weather data from the file that is CLOSEST (in EPOCH) to the current ROP. So a ROP produced at 04:15 am will choose weather data from 'wwDD-03.tif' and a ROP produced at 04:45 will choose the weather data from 'wwDD-06.tif'.
 

**RUNNING MODES:** &nbsp;

**Process ROPS From ENM via EIC using ADC PM Flow && Netsim.**  
The PM COUNTER ROP data is fed from ENM via EIC (ADC PM Counter Flow) and stored in an internal database in RIM. In  this mode, the RIM rAPP will:
- Read PM Counter ROP data from the output topic of the ADC 'eric-oss-3gpp-pm-xml-ran-parser' microservice, namely 'eric-oss-3gpp-pm-xml-ran-parser-nr', using a Kafka Consumer.
- The details of the bootstrap 'bootstrap server' are specified in application.yaml..
- Convert it to internal format suitable for processing based on the PM Counter Avro published schema.
- Post process the data to calculate the parameter: 'avgUeUlTp' (average UE Uplink throughput), based on the NRCELLDU counters [pmMacVolUlResUe](https://cpistore.internal.ericsson.com/elex?LI=EN/LZN7931071R27L&fn=65_15554-LZA7016014_33-V1Uen.BA.html) and [pmMacTimeUlResUe](https://cpistore.internal.ericsson.com/elex?LI=EN/LZN7931071R27L&fn=65_15554-LZA7016014_33-V1Uen.BA.html)
- Post process the data to calculate the parameter: 'avgDeltaIpN' (average delta Interference Plus Noise), based on the NRCELLDU Counters [pmRadioMaxDeltaIpNDistr or pmSWAvgSymbolDeltaIPN](https://cpistore.internal.ericsson.com/elex?LI=EN/LZN7931071R27L&fn=65_15554-LZA7016014_33-V1Uen.BA.html) &nbsp;

     - With the parameter 'APP_DATA_PM_CSV_USE_PRECALCULATED_AVG_DELTA_IPN=true', this calculation is has already been performed (pre-calculated) and stored in the 'avg_delta' column in the PM Counter ROP CSV File in BDR/Object Store. So this 'pre-calculated' value is used for 'avgDeltaIpN'. This is a 'test' mode.(deprecated)
     - With the parameter 'APP_DATA_PM_CSV_USE_PRECALCULATED_AVG_DELTA_IPN=false', this calculation is performed in the RIM rAPP based on the values in the 'pmRadioMaxDeltaIpNDistr' column in the PM Counter ROP CSV File in BDR/Object Store. **This is a 'production' mode**. &nbsp;

- Store the PM Counter ROP data in the internal database of the RIM rAPP, for use during Remote Interference detection and mitigation.



**Replay of CSV Files from Object Store with 'Kafka' enabled.** 

  The PM COUNTER ROP data is fed from a location in object store and stored in an internal database in RIM. In this mode, the RIM rAPP will:
- Read PM Counter ROP data from the 'csv' file in object store.
- Converted it to PM Counter Avro format based on the published schema.
- Placed it on the topic of the 'bootstrap server' specified in application.yaml using a Kafka Producer.
- Read it from the topic of the 'bootstrap server' specified in application.yaml using a Kafka Consumer.
- Convert it to internal format suitable for processing based on the PM Counter Avro published schema.
- Post process the data to calculate the parameter: 'avgUeUlTp' (average UE Uplink throughput), based on the NRCELLDU counters [pmMacVolUlResUe](https://cpistore.internal.ericsson.com/elex?LI=EN/LZN7931071R27L&fn=65_15554-LZA7016014_33-V1Uen.BA.html) and [pmMacTimeUlResUe](https://cpistore.internal.ericsson.com/elex?LI=EN/LZN7931071R27L&fn=65_15554-LZA7016014_33-V1Uen.BA.html)
- Post process the data to calculate the parameter: 'avgDeltaIpN' (average delta Interference Plus Noise), based on the NRCELLDU Counter [pmRadioMaxDeltaIpNDistr or pmSWAvgSymbolDeltaIPN](https://cpistore.internal.ericsson.com/elex?LI=EN/LZN7931071R27L&fn=65_15554-LZA7016014_33-V1Uen.BA.html) &nbsp;

     - With the parameter 'APP_DATA_PM_CSV_USE_PRECALCULATED_AVG_DELTA_IPN=true', this calculation is has already been performed (pre-calculated) and stored in the 'avg_delta' column in the PM Counter ROP CSV File in BDR/Object Store. So this 'pre-calculated' value is used for 'avgDeltaIpN'. This is a 'test' mode.(this is deprecated))
     - With the parameter 'APP_DATA_PM_CSV_USE_PRECALCULATED_AVG_DELTA_IPN=false', this calculation is performed in the RIM rAPP based on the values in the 'pmRadioMaxDeltaIpNDistr' column in the PM Counter ROP CSV File in BDR/Object Store. **This is a 'production' mode**. &nbsp;

- Store the PM Counter ROP data in the internal database of the RIM rAPP, for use during Remote Interference detection and mitigation.

**Replay of CSV Files from Object Store with with 'Kafka' disabled** 

  The PM Counter ROP data is fed from a location in object store and stored in an internal database in RIM. In this mode, the RIM rAPP will:
- Read PM Counter ROP data from the 'csv' file in object store.
- Post process the data to calculate the parameter: 'avgUeUlTp' (average UE Uplink throughput), based on the NRCELLDU counters [pmMacVolUlResUe](https://cpistore.internal.ericsson.com/elex?LI=EN/LZN7931071R27L&fn=65_15554-LZA7016014_33-V1Uen.BA.html) and [pmMacTimeUlResUe](https://cpistore.internal.ericsson.com/elex?LI=EN/LZN7931071R27L&fn=65_15554-LZA7016014_33-V1Uen.BA.html)
- Post process the data to calculate the parameter: 'avgDeltaIpN' (average delta Interference Plus Noise), based on the NRCELLDU Counter [pmRadioMaxDeltaIpNDistr or pmSWAvgSymbolDeltaIPN](https://cpistore.internal.ericsson.com/elex?LI=EN/LZN7931071R27L&fn=65_15554-LZA7016014_33-V1Uen.BA.html) &nbsp;

     - With the parameter 'APP_DATA_PM_CSV_USE_PRECALCULATED_AVG_DELTA_IPN=true', this calculation is has already been performed (pre-calculated) and stored in the 'avg_delta' column in the PM Counter ROP CSV File in BDR/Object Store. So this 'pre-calculated' value is used for 'avgDeltaIpN'. This is a 'test' mode. (this is deprecated))
     - With the parameter 'APP_DATA_PM_CSV_USE_PRECALCULATED_AVG_DELTA_IPN=false', this calculation is performed in the RIM rAPP based on the values in the 'pmRadioMaxDeltaIpNDistr' column in the PM Counter ROP CSV File in BDR/Object Store. This is a 'test' mode. &nbsp;

- Store the PM Counter ROP data in the internal database of the RIM rAPP, for use during Remote Interference detection and mitigation. 



**There are 2 Modes of running the RIM r-APP &nbsp;**

**Replay of CSV Files from Object Store** 

- KAFKA' can be enabled or disabled.  
- If 'KAFKA' is enabled, RIM Kafka  will be reading messages from the 'test' output topic of a local Kafka broker. In this mode,  ROP processing will need to be triggered, and the ROPs need to exist in object store as defied by the 'app.data.pm.rop.path' config parameter. Also a local kafka broker will need to be setup as per the section '**Starting a kafka broker and schema registry locally**'


```
spring.kafka.mode.enabled=true
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.schema-registry.url=http://localhost:8081
spring.kafka.topics.input.name=eric-oss-3gpp-pm-xml-ran-parser-test-nr
app.data.pm.rop.path=pm/netsim_rop/
```
- If 'KAFKA' is disabled, RIM ROP processing will need to be triggered via postman (see below), and the ROPs need to exist in object store as defied by the 'app.data.pm.rop.path' config parameter.

```
spring.kafka.mode.enabled=false
app.data.pm.rop.path=pm/netsim_rop/
```
**Process ROPS From ENM via EIC using ADC PM Flow && Netsimm** 

-'KAFKA' enabled, with RIM Kafka reading messages from the output topic of the  ADC 'eric-oss-3gpp-pm-xml-ran-parser', namely 'eric-oss-3gpp-pm-xml-ran-parser-nr'. In this mode, no need to Trigger ROP, ROP processing controlled by the 'app.data.pm.rop.kafka.cron' config parameter.

```
spring.kafka.mode.enabled=true
spring.kafka.topics.input.name=eric-oss-3gpp-pm-xml-ran-parser-nr
spring.kafka.bootstrap-servers=eric-oss-dmm-kf-op-sz-kafka-bootstrap:9092
spring.kafka.schema-registry.url=http://eric-schema-registry-sr:8081
app.data.pm.rop.kafka.cron=0 10,25,40,55 * * * *
```
**CONFLUENT: Starting a kafka broker and schema registry locally** &nbsp;

In order to run with 'Replay of CSV Files from Object Store with Kafka enabled', the following services need to be running locally.
- zookeeper
- kafka
- schema registry.

For running locally, set the following environmental variables:
- SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
- SPRING_KAFKA_SCHEMA_REGISTRY=http://localhost:8081

Download Confluent &nbsp;

- For Linux:
    - Download [Confluent](https://docs.confluent.io/platform/current/platform-quickstart.html#step-1-download-and-start-cp)  
    - Follow STEP 1, sub-steps 1-5 of the 'Tar archive' tab.  
    - Start Confluent


```
    $ confluent local services status
    $ confluent local services zookeeper start
                Using CONFLUENT_CURRENT: /tmp/confluent.171689
                Starting ZooKeeper
                ZooKeeper is [UP]

    $ confluent local services kafka start
                Using CONFLUENT_CURRENT: /tmp/confluent.171689
                ZooKeeper is [UP]
                Starting Kafka
                Kafka is [UP]

    $ confluent local services schema-registry start
                Using CONFLUENT_CURRENT: /tmp/confluent.171689
                ZooKeeper is [UP]
                Kafka is [UP]
                Starting Schema Registry
                Schema Registry is [UP]
```

- For Windows with WSL
     - Prerequisites: docker running on windows (rancher desktop is the most suitable alternative, docker desktop is not allowed because of its license) Kafka binaries (for testing the client)
     - Prerequisites: docker compose (you can use Chocolatey to install)
     - If docker and docker compose are running ok , everything needed to get kafka, ZK, schema registry etc. up and running is following the instructions [here](https://docs.confluent.io/platform/current/platform-quickstart.html#step-1-download-and-start-cp)  with some small changes.
     - Using powershell, the command to download the docker compose file is only slightly different e.g.:


```
       curl https://raw.githubusercontent.com/confluentinc/cp-all-in-one/7.3.1-post/cp-all-in-one/docker-compose.yml -OutFile docker-compose.yml
```
- Logs.


```
    $ less /tmp/confluent.280442/connect/logs/connect.log
    $ confluent local services connect log

```

**Create kafka topic**


```
    KAFKA : List topics.
        $ confluent-7.3.1/bin/kafka-topics --bootstrap-server http://<bootserver ip>:<Port> --list
    KAFKA : Create topics, when topic of same name (eric-oss-3gpp-pm-xml-ran-parser-nr) does not exist:
        $ confluent-7.3.1/bin/kafka-topics --bootstrap-server http://<bootserver ip>:<Port> --create --topic eric-oss-3gpp-pm-xml-ran-parser-nr --replication-factor 1 --partitions 3 --config segment.ms=300000
    KAFKA : Describe topic
        $ confluent-7.3.1/bin/kafka-topics --bootstrap-server http://<bootserver ip>:<Port> --describe --topic eric-oss-3gpp-pm-xml-ran-parser-nr
           
    KAFKA : Update existing topic, when topic of same name (eric-oss-3gpp-pm-xml-ran-parser-nr) does exists:
        $ confluent-7.3.1/bin/kafka-topics --bootstrap-server http://<bootserver ip>:<Port> --alter --topic eric-oss-3gpp-pm-xml-ran-parser-nr --partitions 3
    KAFKA : Describe topic
        $ confluent-7.3.1/bin/kafka-topics --bootstrap-server http://<bootserver ip>:<Port> --describe --topic eric-oss-3gpp-pm-xml-ran-parser-nr

where <bootserver ip>:<Port> is the kafka broker IP and Port number.
If Running locally this will be 'localhost:9092'
If Running on Cluster, then please contact cluster administrator for details. Also Adjust the number of partitions as needed. The number of consumer setup in the 'spring.kafka.consumer.concurrency' parameter of application.yaml of RIM rAPP, should match number of partitions of the 'Input' topic. 

```




## Starting and running the rApp

- After completing the steps above, the rApp is ready to be started.
- To build the rApp use your IDE or following command:  

```
      cd /<path>/<to>/eric-oss-rim-poc 
      mvn clean install -DskipTests
```

- To run the rApp use your IDE or following command after changing the jar package version according to the main POM.xml file:

```
      java -jar  eric-oss-rim-poc-app/target/eric-oss-rim-poc-app-<version>-SNAPSHOT.jar com.ericsson.oss.apps
```

**TRIGGER A ROP (Replay of CSV Files from Object Store):** &nbsp;

Once the launch completed, trigger the ROP from Postman. This will trigger the rApp to process the ROP data that's in MinIO.
To trigger it, 
- Import in Postman [RIM.postman_collection.json](scripts/json_files/RIM.postman_collection.json) in Postman. 
- Select the RIM POST to 'http://localhost:8080/v1/trigger/pmrop'
- Select the 'JSON' format
- Change the < EPOC Time Stamp >, < customerId > and <rop count> values according to your request.  

```
    {"timeStamp" : 1659931200000, "customerId" : "customer001", "nRops" : 1}
```


The logs will show the rApp running and that the data has been processed.  

```
    2023-01-04 14:20:00.316  INFO 1 --- [   scheduling-1] c.e.o.a.d.collection.pmrop.PmRopLoader   : Processing ROP with CustomerId '<customerId>' and TimeStamp '<EPOC Time Stamp>' 
    ...
    2023-01-04 14:20:32.361  INFO 1 --- [   scheduling-1] c.e.o.a.d.c.features.FeatureCalculator   : Finished calculating features for ROP <EPOC Time Stamp> and customer ID <customerId>
```

**Stop the rAPP** &nbsp;

- Ctrl & C to stop the RIM rAPP
- Ctrl & C to stop minio
- Stop confluent services.


```
  $ confluent local services schema-registry stop; confluent local services kafka stop; confluent local services zookeeper stop

```


# Running the rApp on locally pointing to an EIAP cluster

It is possible to run the rApp locally, feeding it from a local minio installation or an EIAP minio, as long as it's tunneled on the local machine.

Minio Options:
- From a local minio installation, as detailed above in [Running the rApp on locally](#running-the-rapp-on-locally)
- From EIAP minio, tunnel on the local machine. To do this run the following command:
    - ```kubectl port-forward <Pod name> 9000:9000 -n <namespace>```

The steps to set up PM data and baselines are the same as [Running the rApp on locally](#running-the-rapp-on-locally), however there is no need to set up the [CM data files](eric-oss-rim-poc-app/src/test/resources/cm/) (including cell geolocation information), since they would be sourced from CTS and NCMP, assuming the connected ENM is set up correctly, and geolocation data is available in CTS. 

It is usually *not* the case that geolocation data is available in CTS when connected to a NetSim simulated network. It is possible to check if geolocation data is present in CTS by means of the CTS REST API. 

First authenticate - example:

```
  curl -k --location --request POST 'https://th.599462422296.eu-west-1.ac.ericsson.se:443/auth/v1/login' --header 'X-Login: cts-user' --header 'X-password: password' --header 'X-tenant: master'
```
If the authentication is successful, a JSESSIONID (e.g. 11c9ccf7-993d-458e-8332-3ace48a2c9ec) is returned that can be included in the request for geolocation data. 

```
  curl -k --location --request GET 'https://th.599462422296.eu-west-1.ac.ericsson.se/oss-core-ws/rest/ctw/nrcell?fs.geographicSite=attrs&fs.geographicSite.locatedAt=attrs&fs.nrSectorCarriers=attrs' --header 'Cookie: tenantName=master; userName=cts-user; JSESSIONID=11c9ccf7-993d-458e-8332-3ace48a2c9ec'
```
The request will return all the nrcells and the geolocation data (if present).

Geolocation data can can be injected in CTS by means of [loadGeoData.py](scripts/loadGeoData.py)). An example:  

```
  loadGeoData.py -i geodata.csv -n th.599462422296.eu-west-1.ac.ericsson.se -u cts-user -p "password" -ds "/oss-core-ws/rest/osl-adv/datasync/process"
```

After that the following environment variables should be set up. 

- BDR_ACCESS_KEY= < minio admin user >
- BDR_SECRET_KEY= < minio password >
- SPRING_DATASOURCE_EXPOSED=false
- CTS_USERNAME=cts-user
- CTS_PASSWORD= < cts password >
- NCMP_USERNAME=cps-user;
- NCMP_PASSWORD=< ncmp password >
- APP_DATA_CUSTOMERID=< customer ID, according to the PM data provided e.g. netsim01>
- CLIENT_NCMP_BASE_PATH=< base path of ncmp endpoint >
- CLIENT_CTS_BASE_PATH=< base path of CTS endpoint >
- MITIGATION_CLOSED_LOOP_MODE=true
- TROPODUCT_MAXLON=< adjust accoding to the geographical area> 
- TROPODUCT_MAXLAT=< adjust accoding to the geographical area> 
- TROPODUCT_MINLON=< adjust accoding to the geographical area> 
- TROPODUCT_MINLAT=< adjust accoding to the geographical area> 
- CLUSTERING_MINIMUM_CONNECTED_EDGE_WEIGHT=<min connected edge weight value of customer>
- SPRING_KAFKA_MODE_ENABLED=[true|false]
- SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
- SPRING_KAFKA_SCHEMA_REGISTRY=http://localhost:8081
- APP_DATA_PM_CSV_USE_PRECALCULATED_AVG_DELTA_IPN=[true|false]
- APP_DATA_NETSIM=[true|false]
- APP_DATA_PM_ROP_PATH=pm/netsim_rop|pm/rop|pm_<some_other_path>
- TROPODUCT_LOADFROMURL=[true|false]
- TROPODUCT_BASEURLANDPATH=<url of the location of the weather data (.tif) if 'TROPODUCT_LOADFROMURL' is true, ignored if 'TROPODUCT_LOADFROMURL' is false, See section on 'Getting Weather Data'>
- CELL_SELECTION_CONFIG_USE_WEATHER_DATA=true|false


There is no need to change the [application.yaml](eric-oss-rim-poc-app/src/main/resources/application.yaml) file, spring will override the configuration from the environment according to the [relaxed binding](https://docs.spring.io/spring-boot/docs/2.0.x/reference/html/boot-features-external-config.html#boot-features-external-config-relaxed-binding) rules. However, it possible to achieve the same effect by altering the corresponding properties in [application.yaml](eri. c-oss-rim-poc-app/src/main/resources/application.yaml) as described in [Running the rApp on locally](#running-the-rapp-on-locally).

- APPLICATION Running modes:  
  Choose the 'Replay of CSV Files from Object Store (with kafka enabled)' mode of operation as detailed in the '**RUNNING MODES:**' section above.  
    - Start Confluent as detailed in the section **CONFLUENT: Starting a kafka broker and schema registry locally** as described in [Starting and running the rApp](#starting-and-running-the-rapp)
    - Setup kafka topic as per section '**Create kafka topic**' as described in [Starting and running the rApp](#starting-and-running-the-rapp). Note: Setup the Kafka to use 'localhost:9092' and schema registry to use 'http://localhost:8081'

- WEATHER DATA PARAMETERS:
    - See section on '**Getting Weather Data**'
    
Now the rApp can be started as described in [Starting and running the rApp](#starting-and-running-the-rapp).



# Running the rApp on EIAP Cluster
**Preparing the Environment**  
  - SETUP ACCESS TO BDR/Object Store on EIAP  
  
    To access Object Store on EIAP locally from the laptop  
    - Download and install locally the minio-client and give it rights to 'execute'  
        - LINUX : [minio-client](https://dl.min.io/client/mc/release/linux-amd64/mc)
        - WINDOWS : [minio client tool](https://min.io/docs/minio/windows/index.html)
    - Obtain the BDR/Object Store credentials from the cluster administrator.
        - CONTAINER_NAME:<pod Name>
        - MINIO_ACCESS_KEY:<access key>
        - MINIO_SECRET_KEY:<secret key>
    - Expose the PORT in BDR/ Object Store POD to allow local access. EITHER: expose temporarily the object store via [Lens desktop](https://k8slens.dev/desktop.html) OR: Use port forwarding, [kubectl port-forwarding](https://kubernetes.io/docs/tasks/access-application-cluster/port-forward-access-application-cluster/)

    ```
      kubectl : kubectl port-forward <POD> [options] <LOCAL_PORT>:<REMOTE_PORT>  
      LENS : PODS -> 'Click on' <POD>  Ports -> Forward to '<REMOTE_PORT>'
    ```
    - Adding the host to minio client use following command on terminal or PowerShell:  

    ```
      mc config host add objstore http://localhost:{{exposed-port-number}} {{access-key}} {{secret-key}}
    ```  
       In the above command the minio client connection is called 'objstore'
      
    - Create the bucket in the remote object store:  

    ```
      mc mb objstore/rim
    ```
   - UPLOAD REQUIRED DATA to BDR/Object:  
     Store PM, CM and Weather data required by RIM in Object Store. Sample format of Object Store Necessary Documents:
     - [pm baseline](eric-oss-rim-poc-app/src/test/resources/pm/baseline/) csv file, stored in object store under 'rim/pm/baseline/'.
     - [cell allowlist](eric-oss-rim-poc-app/src/test/resources/setup_files/) list of cells the algorithm is allowed to mitigate (both victims and neighbors), stored in object store under 'rim/setup_files/)'.
     
     Weather Data enabled (See '**Getting Weather Data**' Option 1)
     - [rop based ducting](eric-oss-rim-poc-app/src/test/resources/geospatial/ducting/) tiff files, stored in object store under 'rim/geospatial/ducting/'.
     
     Replay of CSV Files from Object Store 'mode' only
     - [rop based pm](eric-oss-rim-poc-app/src/test/resources/pm/rop/) csv files, stored in object store under 'rim/pm/rop/'.
     
     - [CM data files](eric-oss-rim-poc-app/src/test/resources/cm/) cm data files (only required if CM data not available in EIC), stored in object store under 'rim/setup_files/cm/)'.  

     Copying the required data to remote 'rim' folder in the object store:  


```
      mc cp --recursive <path/to/location/of/local/stored/data> objstore/rim/
      
      Example
      mc cp --recursive ./geospatial objstore/rim/
      mc cp --recursive ./pm objstore/rim/
      mc cp --recursive ./pm_baseline objstore/rim/
      mc cp --recursive ./setup_files/cm objstore/rim/
      mc cp --recursive ./setup_files/allowed-NrCellDu-<customerid>.csv.gz  objstore/rim/
```
      
   - OBJECT STORE LIMITED BUCKET SIZE:  
     The data files required by RIM rAPP, and reports generated (on a ROP by ROP basis) by RIM rAPP, are stored in the 'rim bucket' in BDR/Object store. This bucket has a default limited storage provision of approximately 20 GB. To prevent this bucket from filling up (thus causing errors in running the RIM rApp), an automatic expiration of reports generated by RIM can be set, after which the older reports will be deleted. Also need to set expire time on polygon 'json' files created by RIM in 'geospatial/polygons'. To set the expiration time of the report and geospatial/polygons folders, use the [ObjectStoreAdmin.py](scripts/ObjectStoreAdmin.py).


```
  $ python ./ObjectStoreAdmin.py -a "accesskey" -s "secretkey" -n "hostname" set_expire_time_bucket -b "bucketname" -f "foldername" -dc "datecount"

```
- APPLICATION 'CLOSE_LOOP_MODE': To allow the network parameters to be updated the 'CLOSE_LOOP_MODE' parameter of the RIM Application should be set to true. This can be done in either of the following ways.
     - In [application.yaml](eric-oss-rim-poc-app/src/main/resources/application.yaml) file via the parameter CLOSE_LOOP_MODE(default=true)
     - By the environment variable (MITIGATION_CLOSED_LOOP_MODE) variable should be set as true.  
     If it sets as false, it will start to work open loop mode, will disable change implementation and marks them as failed.   &nbsp;

- APPLICATION Running modes:
  Choose a desired mode of operation as detailed in the '**RUNNING MODES:**' section above.
    - **Process ROPS From ENM via EIC using ADC PM Flow && Netsimm** 
      - Contact cluster administrator for details of the kafka broker IP and port and schema registry IP and port. Ensure to setup the 'spring.kafka.bootstrap-servers' and 'spring.kafka.schema-registry.url' parameters in 'application.yaml' either directly or by use of the associated environment variables. 
    - **Replay of CSV Files from Object Store** 
        - Setup kafka topic on the broker as per section '**Create kafka topic**', if required (depends on running mode) 
        - Ensure to set up a unique topic name (i.e. don't use the name 'eric-oss-3gpp-pm-xml-ran-parser-nr') so as not to clash with the 'eric-oss-3gpp-pm-xml-ran-parser' service and set number of partitions to '3'


- WEATHER DATA PARAMETERS:
    - See section on '**Getting Weather Data**'

**Building and Deploy the CSAR**  
1) Firstly, [bob](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/SDP/Install+bob) tool must be downloaded and installed in the local system.  
2) Add the 'common_ruleset2.0.yaml' in the local RIM Project ('ci' folder) with one containing the fast-rollout rules and csar build machinery, located here  [common_ruleset2.0.yaml](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/download/attachments/547663859/common_ruleset2.0.yaml?version=3&modificationDate=1685548969000&api=v2)
If bob tries to use java 11 image, please update the common_ruleset2.0.yaml and make sure :"adp-maven-builder: armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-java17mvnbuilder:${env.MVN_BUILDER_TAG}"  
3) Add the 'common-properties.yaml' in the local RIM Project ( root folder) with this one, [common-properties.yaml](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/download/attachments/547663859/common-properties.yaml?version=2&modificationDate=1685548990000&api=v2)  
4) To build the CSAR package, execute the following command:  

```
     cd /<path>/<to>/eric-oss-rim-poc
     mvn clean install -DskipTests
     bob -r ./ci/common_ruleset2.0.yaml fast-rollout
```

5) Then, replace the Tosca.meta generated by the tool with the Tosca.meta that was originally provided (.bob/csar/Metadata/Tosca.meta) [to fix the CSAR package](https://docs.vmware.com/en/VMware-Telco-Cloud-Automation/1.9/com.vmware.tca.userguide/GUID-0831D39B-25AF-4CFB-A43C-9E7AAD25EA53.html)

```
     mkdir /tmp/csar
     cd /tmp/csar
     unzip ~/repos/prototypes/eric-oss-rim-poc/.bob/csar-output/eric-oss-rim-poc.csar
     cp ~/repos/prototypes/eric-oss-rim-poc/.bob/csar/Metadata/Tosca.meta Metadata/Tosca.meta
     zip -r eric-oss-rim-poc.csar *
```

6) After fixing the CSAR package it can be [Onboard-ed, Enabled, Instantiated](https://developer.intelligentautomationplatform.ericsson.net/#build-a-simple-app/hello-world-onboard-and-instantiate), as shown in the following steps.  
7) Get JSESSIONID value for <Generated JSESSION ID> in the commands below from the response of the request  

```
      curl -k --location --request POST 'https://<EIAP APP MANAGER URL>/auth/v1/login' --header 'X-Login: <Username>' --header 'X-password: <Password>'
```
   - Get returned <Generated JSESSION ID> and use it in the commands (of the steps below) for <Generated JSESSION ID> variable  

8) Then, upload the fixed CSAR package to kubernetes, by running the following command. Note, change  the JSESSIONID field with the value obtained above.

```
     curl -k --location --request POST 'https://<EIAP APP MANAGER URL>/app-manager/onboarding/v1/apps' --header 'accept: application/json' --form 'file=@"/tmp/csar/eric-oss-rim-poc.csar"' --header 'Cookie: JSESSIONID=<Generated JSESSION ID>'
```
   - From the [response](scripts/json_files_csar_upload_response.json) of the above command, get APP ID value for <Onboarding ID> variable and use it in the commands (of the steps below).
   - GET APP INSTANCE STATUS and Wait until the **status":"ONBOARDED","mode":"DISABLED** is shown in the [response](scripts/json_files/get_status_onboarded_disabled.json). The APP Instance Status can be checked with the following command:

```
      curl -k --location --request GET 'https://<EIAP APP MANAGER URL>/app-manager/onboarding/v1/apps/<Onboarding ID>' --header 'accept: application/json' --header 'Cookie: JSESSIONID=<Generated JSESSION ID>'
```
9) To enable the application, use following command after changing <Onboarding ID> and <Generated JSESSION ID> variables to the values obtained above:

```
     curl -k --location --request PUT 'https://<EIAP APP MANAGER URL>/app-manager/onboarding/v1/apps/<Onboarding ID>' -H 'Content-Type: application/json' -d '{"mode": "ENABLED"}' --header 'Cookie: JSESSIONID=<Generated JSESSION ID>'
```
   - Wait until the get **"status":"ONBOARDED","mode":"ENABLED"** in the [response](scripts/json_files/get_status_onboarded_enabled.json), please check with the following command:

```
      curl -k --location --request GET 'https://<EIAP APP MANAGER URL>/app-manager/onboarding/v1/apps/<Onboarding ID>' --header 'accept: application/json' --header 'Cookie: JSESSIONID=<Generated JSESSION ID>'
```

10) Then instantiate the application via following command after changing <Onboarding ID> and <Generated JSESSION ID> variables to the values obtained above

```
      curl -k --location --request POST 'https://<EIAP APP MANAGER URL>/app-manager/lcm/app-lcm/v1/app-instances' \
         -H 'accept: application/json' \
         -H 'Content-Type: application/json' \
         -d '{"appId": <Onboarding ID>}' \
         --header 'Cookie: JSESSIONID=<Generated JSESSION ID>'
```

   - Note the 'INSTANTIATION APP ID' from the reponse, and use it in the command for <Instantiating ID>' variable ( of the steps below). Example of expected [response](scripts/json_files/instantiate_response.json)
   - Wait until the get **"healthStatus":"INSTANTIATED","targetStatus":"INSTANTIATED"** in the [response](scripts/json_files/get_status_instantiated_instantiated.json), please check with the following command:

```
      curl -k --location --request GET 'https://<EIAP APP MANAGER URL>/app-manager/lcm/app-lcm/v1/app-instances/<Instantiating ID>' -H 'accept: application/json' -H 'Content-Type: application/json' --header 'Cookie: JSESSIONID=<Generated JSESSION ID>'

```
11) For detailed information, please check the [confluence page](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/pages/viewpage.action?pageId=547663859&src=contextnavpagetreemode)  

12) Setup the config map as similar to one of the examples as detailed in the 'Update the ConfigMap' section of this [confluence page](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/pages/viewpage.action?pageId=547663859#UbuntuviaOracleVMRunningRIMonEIAPCluster'ecosystem01'-Updatethe'ConfigMap)

13) Once the launch completed, trigger the ROP from Postman. This will trigger the rApp to process the ROP data that's in MinIO.
To trigger it, 
- Import in Postman [RIM.postman_collection.json](scripts/json_files/RIM.postman_collection.json) in Postman. 
- Select the RIM POST to 'http://localhost:8080/v1/trigger/pmrop'
- Select the 'JSON' format
- Change the <EPOC Time Stamp>, <customerId> and <rop count> values according to your request.
- Click 'SEND'  


```
    Do Once    
        sys_login
        create_route
    Then
        LoginToEIAP
        TriggerRIM -Body = {"timeStamp" : <EPOC Time Stamp>, "customerId" : "<customerId>", "nRops" : <rop count>}
```

# Observing the results
  - Currently, a [jupyter notebook](https://gitlab.rnd.gic.ericsson.se/dsi-a/remote-interference-management/-/blob/atlanta_demo/notebooks/atlanta_demo_draft_workspace.ipynb) can be used to observe the result.
  - Clone or copy the jupyter notebook in your local computer and change the minioClient access_key and secret_key variable according to the  processed data set. On the other hand, roptime and selected_roptimes variables must be changed according to the request.
  - After the variable change, the notebook is ready to use. Navigate *Cells --> Run All* button to run whole cells in the notebook.
  - The two types of outputs will be generated automatically. One of them are graphs and pilots which can be observable on jupyter notebook. And the other one is kepler.gl files which contains the outputs about RIM detections and actions on detailed maps visualisations.

# Contributing the rApp
- First set up your IDE and please make sure you have Java version 11.
- Then set up [gerrit](https://gerrit.ericsson.se/#/settings/ssh-keys) settings. The public key of local computer must be added to gerrit. To do it, navigate on [gerrit](https://gerrit.ericsson.se/#/settings/ssh-keys), *under your username on the right -> Settings - SSH Public Keys -> Add key*.  
  1) From the Terminal or Git Bash, run  'ssh-keygen'  
  2) Confirm the default path *.ssh/id_rsa*  
  3) Enter a passphrase (recommended) or leave it blank. Remember this passphrase, as you will need it to unlock the key whenever you use it.   
  4) Open *~/.ssh/id_rsa.pub and copy* & paste the contents into the box below, then click on "**Add**".  
  5) Note that *id_rsa.pub* is your public key and can be shared, while *id_rsa* is your private key and should be kept secret.Create your ssh key in your local computer.  

- After finishing the ssh settings, in [gerrit](https://gerrit.ericsson.se/) navigate to *Projects -> List*, then search **Fremen/eric-oss-rim-poc** in *Project Name* field.
- Copy the "clone with commit-msg hook" command and clone it your local computer 


  ```
  git clone ssh://efitleo@gerrit.ericsson.se:29418/OSS/com.ericsson.oss.apps/eric-oss-rim-poc && scp -p -P 29418 efitleo@gerrit.ericsson.se:hooks/commit-msg eric-oss-rim-poc/.git/hooks/
  git checkout master
  git pull
  git checkout -b <my_branch_name>
  ```
- After the changes done, commit the changes and push them to the master branch.


  ```
  git status
  git add --all
 
  COMMIT:
   FIRST COMMIT:
    $ git commit -m"JIRA Number: < message> " &nbsp;

   SUBSEQUENT COMMITS (in the same branch, not merged to repo)
    $ git commit --amend --no-edit 
     
  git push origin HEAD:refs/for/master
  ```
- Then, add **rAppPrototypeTeam** as reviewers.

