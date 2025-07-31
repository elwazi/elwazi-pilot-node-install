#!/bin/bash

set -e

script_dir=$(dirname "$0")
sample_file_dir=$(dirname "$0")/../resources/sample-files

for file in "$sample_file_dir"/1000GP*; do
  file_name_site=$(echo -n $file | grep -oE '[a-z]+\.[a-z]+$')
  table_name=$(echo -n $file_name_site | tr '.' '_')
  facility_name=$(echo -n $file_name_site | tr '.' '-')
  "$script_dir"/load-table.sh "$table_name" "$facility_name" "$file"
done