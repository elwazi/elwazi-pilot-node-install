import os
import sys
from azure.storage.blob import BlobServiceClient
from azure.identity import DefaultAzureCredential

def upload_blob_to_azure(account, container, blob, file):
    az_url = f"https://{account}.blob.core.windows.net"
    print(f"Attempting to upload '{file}' to Azure Blob Storage...")
    print(f"  Account: {account}")
    print(f"  Container: {container}")
    print(f"  Blob Name: {blob}")
    try:
        credential = DefaultAzureCredential()
        blob_service_cl = BlobServiceClient(account_url=az_url, credential=credential)
        container_cl = blob_service_cl.get_container_client(container)
        try:
            container_cl.get_container_properties()
            print(f"Container '{container}' exists.")
        except Exception as e:
            print(f"Error: Container '{container}' does not exist or access denied: {e}", file=sys.stderr)
            return
        blob_cl = container_cl.get_blob_client(blob)
        # upload the file
        with open(file, "rb") as data:
            blob_cl.upload_blob(data, overwrite=True)
        
        print(f"\nSuccessfully uploaded '{file}' to '{blob}' in container '{container}'.")
        
    except Exception as ex:
        print(f"\nAn error occurred during blob upload: {ex}", file=sys.stderr)

account_name = "elwazitestaccount"
container_name = "wes-upload"
blob_name = "test.html"
file_path = "/home/hocine/test.html"

if not os.path.exists(file_path):
    print(f"Error: Local file '{file_path}' not found.", file=sys.stderr)
    sys.exit(1)
    
upload_blob_to_azure(account_name, container_name, blob_name, file_path)

