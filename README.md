# Instructions for setting up GA4GH standard implementations to demonstrate a eLwazi pilot use case

## Background

The aim is to set up three servers that would mimic (and finaly be) instances that runs on the Ilifu infrastructure, Mali ACE infrastructure and Ugande ACE infrastructure.

The idea is to make use of three GA4GH standards, Data Connnect, DRS and WES. The plan is to later incorporate Passports in the design.


![eLwazi pilot setup](http://https://github.com/elwazi/eLwazi-pilot-node-install/elwazi-pilot.png)

The user case would be. We will make use of the 1000 Genome data. We've selected the ACE2 region from the full genome CRAMs, indexed those to create a more working version of the data. We divided the data into three batches and the DRS and Data Connect servers will therefor just host a specific batch at the corresponding instances. A user would query the Data Connect servers, select the DRS CRAM objects based on the query and submit those to a WES endpoint. Queries on Data Connect can be done using sample id, population group, super population group and sex. The WES endpoint will process some stats on the CRAM files and generate a combined MultiQC report.

## Instructions on individual steps

### Data Connect setup

### DRS setup

### Example
- Link to Python Notebook demonstrating the process


## To DO
- Setup on Mali and Uganda instances
- Setup a workflow that can select CRAMs from a Data Connect query, do joint genotyping, calculate allele frequencies and compare with other queries e.g. from different populations
- Be able to retrieve the output reports through the API. Currently we need to login to the server
- Include a Passport broker
- Look into production versions of Data Connect, DRS, WES and Passport broker

