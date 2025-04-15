#!/bin/sh
set -e

TEMPLATE_FILE="/tapProperties.txt"
OUTPUT_FILE="/config/tap.properties"

# ... (check if template file exists) ...

echo "Custom entrypoint: Processing template file $TEMPLATE_FILE with envsubst..."

# Define which variables envsubst should process.
# The leading '$' tells envsubst these are variable names.
# Single quotes prevent the *current* shell from expanding them here.
VARS_TO_SUBST='$DB_URL $DB_USERNAME $DB_PASSWORD'

# Run envsubst. It will look for DB_URL, API_KEY etc. in its environment
# (which it inherits from this script, which inherits from docker run).
mkdir -p "$(dirname "$OUTPUT_FILE")"
envsubst "$VARS_TO_SUBST" < "$TEMPLATE_FILE" > "$OUTPUT_FILE"

exec java -jar quarkus-run.jar
