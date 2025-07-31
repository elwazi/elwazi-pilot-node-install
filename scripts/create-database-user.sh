#!/bin/bash

# Set default values for PostgreSQL connection if not already defined
: ${PGHOST:=elwazi-catalog.postgres.database.azure.com}
: ${PGUSER:=catalog_admin}
: ${PGPORT:=5432}
: ${PGDATABASE:=postgres}

# Export the variables
export PGHOST PGUSER PGPORT PGDATABASE

# Check if required arguments are provided
if [ $# -ne 1 ]; then
    echo "Usage: $0 <database_name>"
    echo "Example: $0 myapp_db"
    echo ""
    echo "Note: Set PGPASSWORD environment variable before running this script"
    echo "This will create a database and user with the same name"
    exit 1
fi

DB_NAME=$1
USER_NAME=$1

# Prompt for password
echo -n "Enter password for user '$USER_NAME': "
read -s USER_PASSWORD
echo ""

echo "Creating database '$DB_NAME' and user '$USER_NAME'..."

# Create the user (role) first
psql -c "CREATE ROLE $USER_NAME WITH LOGIN PASSWORD '$USER_PASSWORD';"

if [ $? -ne 0 ]; then
    echo "Warning: User $USER_NAME might already exist or creation failed"
fi

# Create the database with the user as owner
psql -c "CREATE DATABASE $DB_NAME OWNER $USER_NAME;"

if [ $? -ne 0 ]; then
    echo "Error: Database creation failed"
    exit 1
fi

# Connect to the new database and set up permissions
psql -d $DB_NAME -c "
-- Grant all privileges on the database to the user
GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $USER_NAME;

-- Grant all privileges on the public schema
GRANT ALL ON SCHEMA public TO $USER_NAME;

-- Grant all privileges on all tables, sequences, and functions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $USER_NAME;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $USER_NAME;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO $USER_NAME;

-- Set default privileges for future tables, sequences, and functions
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $USER_NAME;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $USER_NAME;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO $USER_NAME;
"

if [ $? -eq 0 ]; then
    echo "Successfully created database '$DB_NAME' and user '$USER_NAME' with all privileges"
    echo ""
    echo "To connect as the new user:"
    echo "PGUSER=$USER_NAME PGDATABASE=$DB_NAME psql"
else
    echo "Error setting up permissions"
    exit 1
fi