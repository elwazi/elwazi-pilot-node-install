#!/bin/bash

# Set default values for PostgreSQL connection if not already defined
: ${PGHOST:=elwazi-catalog.postgres.database.azure.com}
: ${PGUSER:=catalog_admin}
: ${PGPORT:=5432}

# Check if PGDATABASE is set
if [ -z "$PGDATABASE" ]; then
    echo "Error: PGDATABASE environment variable is not set"
    echo "Please set it to your target database:"
    echo "  export PGDATABASE=your_database_name"
    exit 1
fi

# Check if PGDATABASE is set to postgres
if [ "$PGDATABASE" = "postgres" ]; then
    echo "Error: PGDATABASE is set to 'postgres' system database"
    echo "Please create a dedicated database using create-database-user.sh:"
    echo "  ./create-database-user.sh your_database_name"
    echo "Then set PGDATABASE to your new database name"
    exit 1
fi

# Export the variables
export PGHOST PGUSER PGPORT PGDATABASE

# Check if required arguments are provided
if [ $# -ne 3 ]; then
    echo "Usage: $0 <table_name> <facility> <data_file_path>"
    echo "Example: $0 samples uganda-ace /path/to/1000GP_Phase3.sample.mali.ace"
    echo ""
    echo "Note: Set PGPASSWORD environment variable before running this script"
    exit 1
fi

TABLE_NAME=$1
FACILITY=$2
DATA_FILE=$3

# Check if data file exists
if [ ! -f "$DATA_FILE" ]; then
    echo "Error: Data file not found: $DATA_FILE"
    exit 1
fi

echo "Loading data from $DATA_FILE into table $TABLE_NAME..."

# Create the table if it doesn't exist
# Set columns that match the 1000 Genomes sample file structure
psql -c "CREATE TABLE IF NOT EXISTS $TABLE_NAME (
    sample_id VARCHAR(50),
    population_code VARCHAR(10),
    superpopulation_code VARCHAR(10),
    sex VARCHAR(10),
    facility VARCHAR(100)
);"

# Create a temporary file with the facility column added
TEMP_FILE=$(mktemp)
awk -v facility="$FACILITY" '{print $0, facility}' "$DATA_FILE" > "$TEMP_FILE"

# Load the space-separated file into the table
psql -c "\COPY $TABLE_NAME(sample_id, population_code, superpopulation_code, sex, facility) FROM '$TEMP_FILE' WITH (FORMAT text, DELIMITER ' ');"

# Clean up temp file
rm -f "$TEMP_FILE"

if [ $? -eq 0 ]; then
    echo "Data loaded successfully into $TABLE_NAME"
else
    echo "Error loading data into $TABLE_NAME"
    exit 1
fi