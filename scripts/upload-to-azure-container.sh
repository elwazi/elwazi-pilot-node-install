#!/bin/sh

# Upload CRAM files to Azure Blob Storage

SECONDS=0

for i in `cut -f 1 -d ' ' /share/elwazi/1000GP_Phase3.sample.uganda.uvri`; do 

    file=$(find /share/elwazi/crams-all/${i} -type f -name "*.cram") 
        sample_name=$(basename "$file")
        echo "Uploading $sample_name"
        echo "Uploading $file"
        az storage blob upload \
            --account-name elwazitestaccount \
            --container-name uganda-uvri\
            --name "${sample_name}" \
            --file "$file" \
            --auth-mode login
done
# Track elapsed time
seconds=$SECONDS
ELAPSED="Elapsed: $(($seconds / 3600))hrs $((($seconds / 60) % 60))min $(($seconds % 60))sec"
echo "$ELAPSED"
