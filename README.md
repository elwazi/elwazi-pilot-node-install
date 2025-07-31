# Instructions for setting up GA4GH standard implementations to demonstrate a eLwazi pilot use case

## Background

The aim is to set up three servers that would mimic (and finaly be) instances that runs on the South-African infrastructure, Mali ACE infrastructure and Ugande ACE infrastructure.

The idea is to make use of three GA4GH standards, Data Connnect, DRS and WES. The plan is to later incorporate Passports in the design but for now we use firewall rules to allow only access between nodes.

<p align="center">
<img src="https://github.com/elwazi/eLwazi-pilot-node-install/blob/main/elwazi-pilot.png" width="500" height="500">
</p>

The user case would be. We will make use of the 1000 Genome data. We've selected the ACE2 region from the full genome CRAMs, indexed those to create a more working version of the data. We divided the data into three batches and the DRS and servers will host a specific batch at the corresponding instances. The Data Connect server will contain CRAM/CRAI access details (DRS ids) from all the DRS servers. A user would query the Data Connect servers, select the DRS CRAM objects based on the query and submit those to a WES endpoint. Queries on Data Connect can be done using sample id, population group, super population group and sex. The WES endpoint will process some stats on the CRAM files and generate a combined MultiQC report.

## Instructions on individual steps

### Data Connect setup

#### Data Connect Microservices Setup on Ilifu Server

The setup was followed from [here](https://github.com/mcupak/elwazi-data-connect-scripts)

Additionally a database was created for our purposes and populated

1. Create the following table:
```
CREATE TABLE genome_ilifu (
    sample_id VARCHAR(36) PRIMARY KEY,
    population_id VARCHAR(36) NOT NULL,
    super_population_id VARCHAR(36) NOT NULL,
    sex VARCHAR(36) NOT NULL,
    cram_drs_id VARCHAR(10485760),
    crai_drs_id VARCHAR(10485760)
);
```
2. Grant all rights to our user 'dataconnecttrino':
```
GRANT ALL PRIVILEGES on TABLE genome_ilifu to dataconnecttrino
```
3. Then Run the script [`genome_ilifu.sql`](https://github.com/elwazi/elwazi-pilot-node-install/blob/main/resources/south-africa/data-connect/db/genome_ilifu.sql) or copy and run it against Postgresql

The `cram_drs_id` and `crai_drs_id` was calcualted based on the md5sum string version of the full file path. This was also how it was added as the DRS id in the DRS database. 


### DRS setup

Did the setup [here](https://github.com/ga4gh/ga4gh-starter-kit-drs) and ran from source.

Dbs, configs and scripts for each node are in the [`resources`](https://github.com/elwazi/elwazi-pilot-node-install/tree/main/resources) folder.

The Python notebook, [`populate-db.ipynb`](https://github.com/elwazi/elwazi-pilot-node-install/blob/main/resources/south-africa/drs/scripts/populate-drs.ipynb), populates the sqlite database with test data. As previously mentioned he hashlib md5 function is used to create the checksum for each file using its full path and use it as the identifier for the DRS object. The DRS object ID, file path, and other information is uploaded to the server database using an HTTP POST request.

### WES setup

Did the setup [here](https://github.com/ga4gh/ga4gh-starter-kit-wes) and ran from source.

Dbs, configs and scripts for each node are in the [`resources`](https://github.com/elwazi/elwazi-pilot-node-install/tree/main/resources) folder.

### Orchestrator

Jupyter notebook orchestrator implementing use two cases are [here](https://github.com/elwazi/elwazi-pilot-node-install/blob/main/resources/south-africa/orchestrator/elwazi-pilot-node-tests.ipynb)

### Loading Sample Files for Data Connect

The `load-all-sites.sh` script automates the loading of 1000 Genomes sample data from all sites into the PostgreSQL database for Data Connect.

#### Prerequisites

1. Ensure PostgreSQL connection environment variables are set:
   ```bash
   export PGHOST=elwazi-catalog.postgres.database.azure.com
   export PGUSER=catalog_admin
   export PGPASSWORD=your_password
   export PGDATABASE=your_database_name  # Must not be 'postgres'
   ```

2. If you don't have a dedicated database, create one using the provided script:
   ```bash
   ./scripts/create-database-user.sh your_database_name
   ```

#### Running the Script

To load all sample files from all sites:
```bash
cd scripts
./load-all-sites.sh
```

The script will:
1. Scan the `resources/sample-files` directory for all 1000GP sample files
2. Extract the site information from the filename (e.g., `mali.ace` becomes facility `mali-ace`)
3. Create a table for each site (e.g., `mali_ace`, `uganda_uvri`)
4. Load the sample data with the following columns:
   - `sample_id`: Sample identifier
   - `population_code`: Population code (e.g., ACB, YRI)
   - `superpopulation_code`: Super-population code (e.g., AFR, EUR)
   - `sex`: Sample sex (male/female)
   - `facility`: Site/facility name (e.g., mali-ace, uganda-uvri)

#### Individual Site Loading

To load a single site's data manually:
```bash
./scripts/load-table.sh table_name facility_name /path/to/sample/file
```

Example:
```bash
./scripts/load-table.sh mali_ace mali-ace resources/sample-files/1000GP_Phase3.sample.mali.ace
```
