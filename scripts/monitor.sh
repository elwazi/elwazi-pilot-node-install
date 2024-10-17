#!/bin/bash

# Define hosts and ports
declare -A machines
machines=( 
    ["ga4gh-starter-kit.ilifu.ac.za"]="4500 4501 5000 5001 6000 6001 8089"
    ["osdp.ace.ac.ug"]="5000 5001 6000 6001"
    ["elwazi-node.icermali.org"]="5000 5001 6000 6001"
    ["196.43.136.22"]="5000 5001 6000 6001"
)

# Function to check a port on a host
check_port() {
    nc -z -w2 $1 $2 &> /dev/null
    if [ $? -eq 0 ]; then
        echo "$1:$2 is open"
    else
        echo "$1:$2 is closed"
    fi
}

# Iterate over machines and check ports
for host in "${!machines[@]}"; do
    for port in ${machines[$host]}; do
        check_port $host $port
    done
done