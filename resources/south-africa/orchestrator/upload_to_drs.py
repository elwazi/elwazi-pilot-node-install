import requests
import datetime
import json
import hashlib
import glob
import os
import uuid

drs_map = {}
timestamp = datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")
aws_region = "UCT-ICTS-DC"
aws_bucket = ""

def post_drs_object_to_server(drs_server=None, drs_server_admin_port=None, object_id=None, description=None, name=None,
    version=None, aliases=[], is_bundle=None, size=None, mime_type=None,
    checksum_md5=None, checksum_sha1=None, checksum_sha256=None,
    drs_object_parent_ids=[], file_key=None):

    """Submit DRS Object to the local Starter Kit DRS server"""

    if is_bundle == None or not is_bundle:
        drs_map[object_id] = checksum_md5
        
    if checksum_md5 != None:
        object_id = checksum_md5
        
    url = "http://" + drs_server + ":" + drs_server_admin_port + "/admin/ga4gh/drs/v1/objects"
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
                "path": file_key
            }
        ]

    response = requests.post(url, json=drs_object_json)
    if response.status_code == 500:
        response_json = response.json()
        message = ( "WARNING: Unsuccessful object creation for DRS object with ID: '{}'. " \
        + "Status Code: {}. Error Message: {}").format(object_id, response_json["status"], response_json["error"])
        print(message)
    elif response.status_code != 200:
        response_json = response.json()
        print(response_json)
        print('--------------')
        message = ( "WARNING: Unsuccessful object creation for DRS object with ID: '{}'. " \
        + "Status Code: {}. Error Message: {}").format(object_id, response_json["status_code"], response_json["msg"])
        print(message)
        

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

def files_metadata_test(run_id, file, file_ext):
    metadata = []
    d = "run_id: %s, file: %s" % (run_id, file_ext) #should be changed
    # Overriding a few things here because we need to mock up values if running WES remotely and orchistrator is local
    # We know where the file is but cannot compute any stats or checksums on it. So the mockup.
    #file_stats = os.stat(file)
    #fsize = file_stats.st_size
    fsize = 1024
    dummy = str(uuid.uuid4())
    md5 = bundle_id_from_name(dummy, "md5")
    sh1 = bundle_id_from_name(dummy, "sh1")
    sh256 = bundle_id_from_name(dummy, "sh256")
    metadata.append([run_id, d, file, md5, sh1, sh256, fsize])
    return metadata

def add_file_to_server(metadata, type, drs_server, drs_server_admin_port):
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
        size = mdata[6]
        post_drs_object_to_server(
            drs_server = drs_server,
            drs_server_admin_port = drs_server_admin_port,
            object_id = object_id,
            is_bundle = is_bundle,
            mime_type = mime_type,
            checksum_md5 = checksum_md5,
            file_key = file_path,
            name = name,
            description = description,
            checksum_sha1 = checksum_sha1,
            checksum_sha256 = checksum_sha256,
            size = size
        )