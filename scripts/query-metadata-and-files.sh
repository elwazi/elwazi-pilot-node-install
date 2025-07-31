#!/bin/bash

set -e

if [ $# -ne 1 ]; then
    echo "Usage: $0 <slug>"
    echo "Example: $0 sa-ilifu"
    exit 1
fi

# check that dnastack command is installed
if ! command -v omics &> /dev/null; then
    echo "omics command not found. Please install it first." >&2
    echo "https://docs.omics.ai/products/command-line-interface/installation" >&2
    exit 1
fi

slug=$1
schema_name="$(echo -n "$slug" | tr '-' '_')"
metadata_table_name="${schema_name}"

omics use elwazi.omics.ai
omics cs query -c "$slug" "
SELECT metadata.*, files.name, files.drs_url
FROM collections.${schema_name}.${metadata_table_name} metadata
JOIN collections.${schema_name}._files files
ON files.name LIKE '%' || metadata.sample_id || '%'" | jq