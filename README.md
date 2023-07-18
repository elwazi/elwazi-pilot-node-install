# Instructions for setting up GA4GH standard implementations to demonstrate a eLwazi pilot use case

## Background

The aim is to set up three servers that would mimic (and finaly be) instances that runs on the Ilifu infrastructure, Mali ACE infrastructure and Ugande ACE infrastructure.

The idea is to make use of three GA4GH standards, Data Connnect, DRS and WES. The plan is to later incorporate Passports in the design.

<p align="center">
<img src="https://github.com/elwazi/eLwazi-pilot-node-install/blob/main/elwazi-pilot.png" width="500" height="500">
</p>

The user case would be. We will make use of the 1000 Genome data. We've selected the ACE2 region from the full genome CRAMs, indexed those to create a more working version of the data. We divided the data into three batches and the DRS and Data Connect servers will therefor just host a specific batch at the corresponding instances. A user would query the Data Connect servers, select the DRS CRAM objects based on the query and submit those to a WES endpoint. Queries on Data Connect can be done using sample id, population group, super population group and sex. The WES endpoint will process some stats on the CRAM files and generate a combined MultiQC report.

## Instructions on individual steps

### Data Connect setup

#### Part where we prepare DRS CRAM and CRAI ids to be added to database

```
gerrit@ga4gh-starter-kit:/share/elwazi$ for i in `cut -d ' ' -f 1 1000GP_Phase3.sample.ilifu`;do \
cram=`ls /share/elwazi/crams/$i/$i*.cram`; crai=`ls /share/elwazi/crams/$i/$i*.cram.crai`; \
crammd5sum=`echo -n $crai | md5sum | cut -f 1 -d ' '`; craimd5sum=`echo -n $crai | md5sum | cut -f 1 -d ' '`; \
echo -e "$i\tdrs://ga4gh-starter-kit.ilifu.ac.za:5000/$crammd5sum\tdrs://ga4gh-starter-kit.ilifu.ac.za:6000/$craimd5sum"; \
done > 1000GP_Phase3.sample.ilifu.drs
```

#### Create the Data Connect Microservices 

```
We intended create three server instance for the data connect, one in Mali, Uganda and the last one in ilifu server.

**** Ilifu Server set up setup


```

### DRS setup

Data Repository Service (DRS) provides minimal access to genomic file data by creating a DRS object for use in workflows as entry points to the files (![read more about DRS here](https://github.com/ga4gh/ga4gh-starter-kit-drs)). 

The [docker-compose.yml](./docker-compose.yml) file has been configured to launch two docker services, the first service, relies on the [ga4gh-starter-kit-util](https://github.com/ga4gh/ga4gh-starter-kit-utils) docker image, creates a relational sqlite database to store DRS objects with a unique identifier, a URL to access the file associated with the object identifier and other additional information in different SQL tables (see [create-tables.sql](resources/drs/db-scripts/create-tables.sql) file). The second service configures the DRS server using the ga4gh-starter-kit-drs docker image and defines two HTTP access ports, one for API consumers and one for server administrator tasks. The server settings are defined in the YAML [configuration file](./resources/drs/config/config.yml).

Once everything is in place, run the following command line to launch the services and start the DRS server:

```
docker-compose up -d
```

Verify that a Docker DRS container is created and is listening for HTTP requests on the ports designed in the docker-compose.yml file:

```
docker ps
```

To stop the server, the following command stops and removes the DRS container. Also run the bash refresh.sh script to remove the sqlite database, in case of a fresh install.

```
docker-compose down
./refresh.sh
```

The Python notebook, populate-db.ipynb, populates the sqlite database with test data. It uses the sample ID in the first column of the text file, `xx.ilifu`, to construct the full path to the CRAM and CRAI files. The hashlib md5 function is used to create the checksum for each file using its full path and use it as the identifier for the DRS object. The DRS object ID, file path, and other information is uploaded to the server database using an HTTP POST request.

### Example
- Link to Python Notebook demonstrating the process

## To Do
- Setup on Mali and Uganda instances
- Setup a workflow that can select CRAMs from a Data Connect query, do joint genotyping, calculate allele frequencies and compare with other queries e.g. from different populations
- Be able to retrieve the output reports through the API. Currently we need to login to the server
- Include a Passport broker
- Look into production versions of Data Connect, DRS, WES and Passport broker

