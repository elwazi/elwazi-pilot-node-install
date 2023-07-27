#!/usr/bin/env python
# coding: utf-8

import requests
import datetime
import json
import hashlib
import glob
import os

drs_map = {}
timestamp = datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")
aws_region = "UCT-ICTS-DC"
aws_bucket = "oss.ilifu.ac.za:6780/a2d39f437ddd48dfad9f068a0d5a4ee2:elwazi-pilot-node-install"

def post_drs_object_to_server(object_id=None, description=None, name=None,
    version=None, aliases=[], is_bundle=None, size=None, mime_type=None,
    checksum_md5=None, checksum_sha1=None, checksum_sha256=None,
    drs_object_parent_ids=[], aws_file_key=None):

    """Submit DRS Object to the local Starter Kit DRS server"""

    if is_bundle == None or not is_bundle:
        drs_map[object_id] = checksum_md5
        
    if checksum_md5 != None:
        object_id = checksum_md5
        
    url = "http://localhost:5001/admin/ga4gh/drs/v1/objects"
    drs_object_json = {
        "id": object_id,
        "description": description,
        "created_time": timestamp,
        "name": name,
        "updated_time": timestamp,
        "version": version,
        "aliases": aliases,
        "is_bundle": is_bundle,
    }

    if len(drs_object_parent_ids) > 0:
        drs_object_json["drs_object_parents"] = [{"id": i} for i in drs_object_parent_ids]

    if not is_bundle:
        # add size, checksums, drs object parents
        drs_object_json["size"] = size
        drs_object_json["mime_type"] = mime_type
        drs_object_json["checksums"] = []
        drs_object_json["checksums"].append({"type": "md5", "checksum": checksum_md5})
        drs_object_json["checksums"].append({"type": "sha1", "checksum": checksum_sha1})
        drs_object_json["checksums"].append({"type": "sha256", "checksum": checksum_sha256})
        
        drs_object_json["file_access_objects"] = [
            {
                "path": file_path
            }
        ]
        
        #drs_object_json["aws_s3_access_objects"] = [
        #    {
        #        "region": aws_region,
        #        "bucket": aws_bucket,
        #        "key": aws_file_key
        #    }
        #]

    response = requests.post(url, json=drs_object_json)
    if response.status_code == 500:
        response_json = response.json()
        message = ( "WARNING: Unsuccessful object creation for DRS object with ID: '{}'. " \
        + "Status Code: {}. Error Message: {}").format(object_id, response_json["status"], response_json["error"])
        print(message)
    elif response.status_code != 200:
        response_json = response.json()
        message = ( "WARNING: Unsuccessful object creation for DRS object with ID: '{}'. " \
        + "Status Code: {}. Error Message: {}").format(object_id, response_json["status_code"], response_json["msg"])
        print(message)


# In[17]:


def bundle_id_from_name(bundle_name, hash="md5"):
    if hash == "md5":
        bundle_hash = hashlib.md5(bundle_name.encode())
    elif hash == "sh1":
        bundle_hash = hashlib.sha1(bundle_name.encode())
    elif hash == "sh256":
        bundle_hash = hashlib.sha256(bundle_name.encode())
    bundle_id = bundle_hash.hexdigest()
    #drs_map[bundle_name] = bundle_id
    return bundle_id


# In[18]:


def files_metadata(path, ext):
    metadata = []
    with open(os.path.join(path, "1000GP_Phase3.sample.ilifu")) as f:
        for line in f.readlines():
            s = line.split(' ')[0]   
            o = line.split(' ')
            d = "Patient: %s, Country: %s, Region: %s, Sex: %s" % (o[0], o[1], o[2], o[3])
            cr_path = path + "/crams/{}/*.{}".format(s,ext)
            files = glob.glob(cr_path)
            for file in files:
                f_path = "/crams/{}/{}".format(s,os.path.basename(file))
                file_stats = os.stat(file)
                fsize = file_stats.st_size
                md5 = bundle_id_from_name(file, "md5")
                sh1 = bundle_id_from_name(file, "sh1")
                sh256 = bundle_id_from_name(file, "sh256")
                metadata.append([s, d, f_path, md5, sh1, sh256])
    return metadata


# In[19]:


def add_file_to_server(metadata, type):
    for mdata in metadata:
        object_id = "%s.%s" % (mdata[0], type)
        file_path = mdata[2]
        name = os.path.splitext(file_path.split('/')[-1])[0]
        description = mdata[1]
        is_bundle = False
        mime_type = "application/%s" % (type)
        checksum_md5 = mdata[3]
        checksum_sha1 = mdata[4]
        checksum_sha256 = mdata[5]
        post_drs_object_to_server(
            object_id = object_id,
            is_bundle = is_bundle,
            mime_type = mime_type,
            checksum_md5 = checksum_md5,
            aws_file_key = file_path,
            name = name,
            description = description,
            checksum_sha1 = checksum_sha1,
            checksum_sha256 = checksum_sha256
        )


# In[20]:


headings = ["sample_id", "cram_md5"]
path = "/share/elwazi"
metadata_crams = files_metadata(path, "cram")
metadata_crais = files_metadata(path, "crai")


# In[21]:


add_file_to_server(metadata_crams, "cram")
add_file_to_server(metadata_crais, "crai")


# In[ ]:




