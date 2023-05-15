#!/bin/bash -e

# This script automates the deployment to Azure Container Apps
# See /docs/deploying-to-azure-containerapps.md for details

# Display help
help() {
  echo "This script deploys the superheroes to Azure Container Apps."
  echo "It assumes you have the the following utilities installed on your machine:"
  echo "  1) The Azure CLI (https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)"
  echo "  2) jq (https://stedolan.github.io/jq)"
  echo "  3) curl (https://curl.se)"
  echo
  echo "Additionally, it assumes you are already logged into your Azure environment via the Azure CLI."
  echo
  echo "Syntax: deploy-to-azure-containerapps.sh [options]"
  echo "options:"
  echo "  -g <resource_group_name>     Name of the Azure resource group to use to deploy resources. Default is 'super-heroes'."
  echo "  -h                           Prints this help message"
  echo "  -l <location>                The location (region) to deploy resources into. Default: 'eastus2'".
  echo "  -p <postgres_server_tier>    Compute tier of the PostgreSQL servers. Accepted values: Burstable, GeneralPurpose, MemoryOptimized. Default: 'Burstable'."
  echo "  -r                           If present, create an Azure Container Registry instance (see https://azure.microsoft.com/en-us/services/container-registry). This is optional. No container images are pushed here by this script."
  echo "  -s <postgres_server_sku>     The SKU to use for the PostgreSQL servers (see https://azure.microsoft.com/en-us/pricing/details/postgresql/flexible-server). Default: 'B1ms'."
  echo "  -t <tag>                     The tag for the images to deploy. Accepted values: 'java17-latest' or 'native-latest'. Default: 'native-latest'."
  echo "  -u <unique_identifier>       A unique identifier to append to some resources. Some Azure services require unique names within a region (across users). Default is to use the output of the 'whoami' command."
}

exit_abnormal() {
  echo
  help
  exit 1
}

cleanup() {
  echo "Removing temp directory $TEMP_DIR"
  rm -rf $TMP_DIR
}

create_container_registry() {
  echo "Creating the $CONTAINER_REGISTRY_NAME container registry"
  az acr create \
    --resource-group "$RESOURCE_GROUP" \
    --location "$LOCATION" \
    --name "$CONTAINER_REGISTRY_NAME" \
    --sku Standard \
    --tags system="$TAG_SYSTEM"

  # Allow anonymous pull access
  echo "Allowing anonymous pull access to the $CONTAINER_REGISTRY_NAME container registry"
  az acr update \
    --name "$CONTAINER_REGISTRY_NAME" \
    --anonymous-pull-enabled

  echo "Getting the URL for the $CONTAINER_REGISTRY_NAME container registry"
  CONTAINER_REGISTRY_URL=$(az acr show \
    --resource-group "$RESOURCE_GROUP" \
    --name "$CONTAINER_REGISTRY_NAME" \
    --output json | jq -r .loginServer)
}

create_postgres_db() {
  local APP_NAME=$1
  local DB_NAME=$2
  local DB_SCHEMA=$3
  local DB_SQL=$4
  local DOWNLOADED_SQL_FILE="$TEMP_DIR/${APP_NAME}.sql"

  echo "Creating the $DB_NAME postgres database"
  az postgres flexible-server create \
    --resource-group "$RESOURCE_GROUP" \
    --location "$LOCATION" \
    --tags system="$TAG_SYSTEM" application="$APP_NAME" \
    --name "$DB_NAME" \
    --admin-user "$POSTGRES_DB_ADMIN" \
    --admin-password "$POSTGRES_DB_PWD" \
    --public all \
    --sku-name "Standard_$POSTGRES_SKU" \
    --tier "$POSTGRES_TIER" \
    --storage-size 256 \
    --version "$POSTGRES_DB_VERSION"
  echo

  echo "Creating the $DB_SCHEMA schema on the $DB_NAME database"
  az postgres flexible-server db create \
    --resource-group "$RESOURCE_GROUP" \
    --server-name "$DB_NAME" \
    --database-name "$DB_SCHEMA"
  echo

  echo "Downloading $APP_NAME data from $DB_SQL to $DOWNLOADED_SQL_FILE"
  curl "$DB_SQL" --output "$DOWNLOADED_SQL_FILE"
  echo

  echo "Adding data to the $DB_SCHEMA schema in the $DB_NAME database"
  az postgres flexible-server execute \
    --name "$DB_NAME" \
    --admin-user "$POSTGRES_DB_ADMIN" \
    --admin-password "$POSTGRES_DB_PWD" \
    --database-name "$DB_SCHEMA" \
    --file-path "$DOWNLOADED_SQL_FILE"
  echo
}

create_mongo_db() {
  local DB_NAME=$1
  local DB_SCHEMA=$2

  echo "Creating the $DB_NAME MongoDB database"
  az cosmosdb create \
    --resource "$RESOURCE_GROUP" \
    --locations regionName="$LOCATION" failoverPriority=0 \
    --tags system="$TAG_SYSTEM" application="$FIGHTS_APP" \
    --name "$DB_NAME" \
    --kind MongoDB \
    --server-version "$MONGO_DB_VERSION"
  echo

  echo "Creating the $DB_SCHEMA schema on the $DB_NAME MongoDB"
  az cosmosdb mongodb database create \
    --resource-group "$RESOURCE_GROUP" \
    --account-name "$DB_NAME" \
    --name "$DB_SCHEMA"
  echo
}

get_mongo_connect_string() {
  local DB_NAME=$1
  echo $(az cosmosdb keys list \
      --resource-group "$RESOURCE_GROUP" \
      --name "$DB_NAME" \
      --type connection-strings \
      --query "connectionStrings[?description=='Primary MongoDB Connection String'].connectionString" \
      --output tsv)
}

create_eventhub() {
  echo "Creating the EventHub namespace $KAFKA_NAMESPACE"
  az eventhubs namespace create \
    --resource-group "$RESOURCE_GROUP" \
    --location "$LOCATION" \
    --tags system="$TAG_SYSTEM" application="$FIGHTS_APP" \
    --name "$KAFKA_NAMESPACE"

  echo "Creating the Kafka topic $KAFKA_TOPIC in the EventHub namespace $KAFKA_NAMESPACE"
  az eventhubs eventhub create \
    --resource-group "$RESOURCE_GROUP" \
    --name "$KAFKA_TOPIC" \
    --namespace-name "$KAFKA_NAMESPACE"
}

get_eventhub_connect_string() {
  echo $(az eventhubs namespace authorization-rule keys list \
      --resource-group "$RESOURCE_GROUP" \
      --namespace-name "$KAFKA_NAMESPACE" \
      --name RootManageSharedAccessKey \
      --output json | jq -r .primaryConnectionString)
}

create_apicurio() {
  echo "Creating Apicurio Container App"
  az containerapp create \
    --resource-group "$RESOURCE_GROUP" \
    --tags system="$TAG_SYSTEM" application="$FIGHTS_APP" \
    --image "$APICURIO_IMAGE" \
    --name "$APICURIO_APP" \
    --environment "$CONTAINERAPPS_ENVIRONMENT" \
    --ingress external \
    --target-port 8080 \
    --min-replicas 1 \
    --env-vars REGISTRY_AUTH_ANONYMOUS_READ_ACCESS_ENABLED=true

  echo "Getting Apicurio URL"
  APICURIO_URL="https://$(az containerapp ingress show \
      --resource-group "$RESOURCE_GROUP" \
      --name "$APICURIO_APP" \
      --output json | jq -r .fqdn)"

  echo "Setting https URLs for Apicurio"
  az containerapp update \
    --resource-group "$RESOURCE_GROUP" \
    --name "$APICURIO_APP" \
    --set-env-vars REGISTRY_UI_CONFIG_APIURL="${APICURIO_URL}/apis/registry" \
                   REGISTRY_UI_CONFIG_UIURL="${APICURIO_URL}/ui"
}

create_heroes_app() {
  echo "Creating Heroes Container App"
  az containerapp create \
    --resource-group "$RESOURCE_GROUP" \
    --tags system="$TAG_SYSTEM" application="$HEROES_APP" \
    --image "$HEROES_IMAGE" \
    --name "$HEROES_APP" \
    --environment "$CONTAINERAPPS_ENVIRONMENT" \
    --ingress external \
    --target-port 8083 \
    --min-replicas 1 \
    --env-vars QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION=validate \
               QUARKUS_HIBERNATE_ORM_SQL_LOAD_SCRIPT=no-file \
               QUARKUS_DATASOURCE_USERNAME="$POSTGRES_DB_ADMIN" \
               QUARKUS_DATASOURCE_PASSWORD="$POSTGRES_DB_PWD" \
               QUARKUS_DATASOURCE_REACTIVE_URL="$HEROES_DB_CONNECT_STRING"

  echo "Getting URL to heroes app"
  HEROES_URL="https://$(az containerapp ingress show \
      --resource-group $RESOURCE_GROUP \
      --name $HEROES_APP \
      --output json | jq -r .fqdn)"
}

create_villains_app() {
  echo "Creating Villains Container App"
  az containerapp create \
    --resource-group "$RESOURCE_GROUP" \
    --tags system="$TAG_SYSTEM" application="$VILLAINS_APP" \
    --image "$VILLAINS_IMAGE" \
    --name "$VILLAINS_APP" \
    --environment "$CONTAINERAPPS_ENVIRONMENT" \
    --ingress external \
    --target-port 8084 \
    --min-replicas 1 \
    --env-vars QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION=validate \
               QUARKUS_HIBERNATE_ORM_SQL_LOAD_SCRIPT=no-file \
               QUARKUS_DATASOURCE_USERNAME="$POSTGRES_DB_ADMIN" \
               QUARKUS_DATASOURCE_PASSWORD="$POSTGRES_DB_PWD" \
               QUARKUS_DATASOURCE_JDBC_URL="$VILLAINS_DB_CONNECT_STRING"

  echo "Getting URL to villains app"
  VILLAINS_URL="https://$(az containerapp ingress show \
      --resource-group $RESOURCE_GROUP \
      --name $VILLAINS_APP \
      --output json | jq -r .fqdn)"
}

create_statistics_app() {
  echo "Creating Event Statistics Container App"
  az containerapp create \
    --resource-group "$RESOURCE_GROUP" \
    --tags system="$TAG_SYSTEM" application="$STATISTICS_APP" \
    --image "$STATISTICS_IMAGE" \
    --name "$STATISTICS_APP" \
    --environment "$CONTAINERAPPS_ENVIRONMENT" \
    --ingress external \
    --target-port 8085 \
    --min-replicas 1 \
    --env-vars KAFKA_BOOTSTRAP_SERVERS="$KAFKA_BOOTSTRAP_SERVERS" \
               KAFKA_SECURITY_PROTOCOL=SASL_SSL \
               KAFKA_SASL_MECHANISM=PLAIN \
               KAFKA_SASL_JAAS_CONFIG="$KAFKA_JAAS_CONFIG" \
               MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_APICURIO_REGISTRY_URL="${APICURIO_URL}/apis/registry/v2"

  echo "Getting URL to event statistics app"
  STATISTICS_URL="https://$(az containerapp ingress show \
      --resource-group $RESOURCE_GROUP \
      --name $STATISTICS_APP \
      --output json | jq -r .fqdn)"
}

create_fights_app() {
  echo "Creating Fights Container App"
  az containerapp create \
    --resource-group "$RESOURCE_GROUP" \
    --tags system="$TAG_SYSTEM" application="$FIGHTS_APP" \
    --image "$FIGHTS_IMAGE" \
    --name "$FIGHTS_APP" \
    --environment "$CONTAINERAPPS_ENVIRONMENT" \
    --ingress external \
    --target-port 8082 \
    --min-replicas 1 \
    --env-vars KAFKA_BOOTSTRAP_SERVERS="$KAFKA_BOOTSTRAP_SERVERS" \
               KAFKA_SECURITY_PROTOCOL=SASL_SSL \
               KAFKA_SASL_MECHANISM=PLAIN \
               KAFKA_SASL_JAAS_CONFIG="$KAFKA_JAAS_CONFIG" \
               MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_APICURIO_REGISTRY_URL="${APICURIO_URL}/apis/registry/v2" \
               QUARKUS_LIQUIBASE_MONGODB_MIGRATE_AT_START=false \
               QUARKUS_MONGODB_CONNECTION_STRING="$MONGO_COLLECTION_CONNECT_STRING" \
               QUARKUS_REST_CLIENT_HERO_CLIENT_URL="$HEROES_URL" \
               FIGHT_VILLAIN_CLIENT_BASE_URL="$VILLAINS_URL"

  echo "Getting URL to event fights app"
  FIGHTS_URL="https://$(az containerapp ingress show \
      --resource-group $RESOURCE_GROUP \
      --name $FIGHTS_APP \
      --output json | jq -r .fqdn)"
}

create_ui_app() {
  echo "Creating Super Heroes UI Container App"
  az containerapp create \
    --resource-group "$RESOURCE_GROUP" \
    --tags system="$TAG_SYSTEM" application="$UI_APP" \
    --image "$UI_IMAGE" \
    --name "$UI_APP" \
    --environment "$CONTAINERAPPS_ENVIRONMENT" \
    --ingress external \
    --target-port 8080 \
    --env-vars API_BASE_URL="$FIGHTS_URL"

  echo "Getting URL to Super Heroes UI app"
  UI_URL="https://$(az containerapp ingress show \
      --resource-group $RESOURCE_GROUP \
      --name $UI_APP \
      --output json | jq -r .fqdn)"
}

# Define defaults
RESOURCE_GROUP="super-heroes"
LOCATION="eastus2"
IMAGES_TAG="native-latest"
UNIQUE_IDENTIFIER=$(whoami)
POSTGRES_SKU="B1ms"
POSTGRES_TIER="Burstable"
CREATE_CONTAINER_REGISTRY=false

# Process the input options
while getopts "g:hl:p:rs:t:u:" option; do
  case $option in
    g) RESOURCE_GROUP=$OPTARG
       ;;

    h) help
       exit
       ;;

    l) LOCATION=$OPTARG
       ;;

    p) POSTGRES_TIER=$OPTARG
       ;;

    r) CREATE_CONTAINER_REGISTRY=true
       ;;

    s) POSTGRES_SKU=$OPTARG
       ;;

    t) IMAGES_TAG=$OPTARG
       ;;

    u) UNIQUE_IDENTIFIER=$OPTARG
       ;;

    *) exit_abnormal
       ;;
  esac
done

# Define variables
# Other
TEMP_DIR=$(mktemp -d)
GITHUB_RAW_BASE_URL="https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/main"
SUPERHEROES_IMAGES_BASE="quay.io/quarkus-super-heroes"
TAG_SYSTEM=quarkus-super-heroes

# Container registry
CONTAINER_REGISTRY_NAME="superheroes${UNIQUE_IDENTIFIER}"

# Container Apps
LOG_ANALYTICS_WORKSPACE="super-heroes-logs"
CONTAINERAPPS_ENVIRONMENT="super-heroes-env"

# Postgres
POSTGRES_DB_ADMIN="superheroesadmin"
POSTGRES_DB_PWD="p@ssw0rd-12046"
POSTGRES_DB_VERSION=14

# MongoDB
MONGO_DB="fights-db-$UNIQUE_IDENTIFIER"
MONGO_DB_VERSION="4.2"

# Kafka
KAFKA_NAMESPACE="fights-kafka-$UNIQUE_IDENTIFIER"
KAFKA_TOPIC="fights"
KAFKA_BOOTSTRAP_SERVERS="$KAFKA_NAMESPACE.servicebus.windows.net:9093"

# Apicurio
APICURIO_APP="apicurio"
APICURIO_IMAGE="apicurio/apicurio-registry-mem:2.4.2.Final"

# Heroes
HEROES_APP="rest-heroes"
HEROES_DB="heroes-db-$UNIQUE_IDENTIFIER"
HEROES_IMAGE="${SUPERHEROES_IMAGES_BASE}/${HEROES_APP}:${IMAGES_TAG}"
HEROES_DB_SCHEMA="heroes"
HEROES_DB_SQL="$GITHUB_RAW_BASE_URL/$HEROES_APP/deploy/db-init/initialize-tables.sql"
HEROES_DB_CONNECT_STRING="postgresql://${HEROES_DB}.postgres.database.azure.com:5432/${HEROES_DB_SCHEMA}?ssl=true&sslmode=require"

# Villains
VILLAINS_APP="rest-villains"
VILLAINS_DB="villains-db-$UNIQUE_IDENTIFIER"
VILLAINS_IMAGE="${SUPERHEROES_IMAGES_BASE}/${VILLAINS_APP}:${IMAGES_TAG}"
VILLAINS_DB_SCHEMA="villains"
VILLAINS_DB_SQL="$GITHUB_RAW_BASE_URL/$VILLAINS_APP/deploy/db-init/initialize-tables.sql"
VILLAINS_DB_CONNECT_STRING="jdbc:postgresql://${VILLAINS_DB}.postgres.database.azure.com:5432/${VILLAINS_DB_SCHEMA}?ssl=true&sslmode=require"

# Fights
FIGHTS_APP="rest-fights"
FIGHTS_DB_SCHEMA="fights"
FIGHTS_IMAGE="${SUPERHEROES_IMAGES_BASE}/${FIGHTS_APP}:${IMAGES_TAG}"

# Statistics
STATISTICS_APP="event-statistics"
STATISTICS_IMAGE="${SUPERHEROES_IMAGES_BASE}/${STATISTICS_APP}:${IMAGES_TAG}"

# UI
UI_APP="ui-super-heroes"
UI_IMAGE="${SUPERHEROES_IMAGES_BASE}/${UI_APP}:latest"

# Now run the script
echo "Deploying Quarkus Superheroes to Azure Container Apps with the following configuration:"
echo "  Resource Group: $RESOURCE_GROUP"
echo "  Location: $LOCATION"
echo "  Container image tag: $IMAGES_TAG"
echo "  Unique identifier: $UNIQUE_IDENTIFIER"
echo "  PostgreSQL SKU: $POSTGRES_SKU"
echo "  PostgreSQL Tier: $POSTGRES_TIER"
echo
echo "Please be patient. This may take a while."
echo

# Install the required extensions
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Installing required Azure CLI extensions"
echo "-----------------------------------------"
az extension add --name containerapp
az extension add --name rdbms-connect
az extension add --name log-analytics
echo

# Register the Microsoft.App namespace
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Registering the Microsoft.App namespace"
echo "-----------------------------------------"
az provider register --namespace Microsoft.App --wait
echo

# Register the Microsoft.OperationalInsights provider
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Registering the Microsoft.OperationalInsights provider"
echo "-----------------------------------------"
az provider register --namespace Microsoft.OperationalInsights --wait
echo

# Create resource group
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $RESOURCE_GROUP resource group in the $LOCATION location"
echo "-----------------------------------------"
az group create \
  --name "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --tags system="$TAG_SYSTEM"
echo

# Create Container registry (if needed)
if "$CREATE_CONTAINER_REGISTRY"; then
  echo "-----------------------------------------"
  echo "[$(date +"%m/%d/%Y %T")]: Creating the $CONTAINER_REGISTRY_NAME container registry"
  echo "-----------------------------------------"
  create_container_registry
fi

# Create the Heroes Postgres db
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $HEROES_DB PostgreSQL database"
echo "-----------------------------------------"
create_postgres_db "$HEROES_APP" "$HEROES_DB" "$HEROES_DB_SCHEMA" "$HEROES_DB_SQL"
echo

# Create the Villains Postgres db
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $VILLAINS_DB PostgreSQL database"
echo "-----------------------------------------"
create_postgres_db "$VILLAINS_APP" "$VILLAINS_DB" "$VILLAINS_DB_SCHEMA" "$VILLAINS_DB_SQL"
echo

# Create the Fights MongoDB db
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $MONGO_DB MongoDB database"
echo "-----------------------------------------"
create_mongo_db "$MONGO_DB" "$FIGHTS_DB_SCHEMA"
MONGO_COLLECTION_CONNECT_STRING=$(get_mongo_connect_string "$MONGO_DB")
echo

# Create the EventHub
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $KAFKA_NAMESPACE EventHub and the $KAFKA_TOPIC topic"
echo "-----------------------------------------"
create_eventhub
KAFKA_CONNECTION_STRING=$(get_eventhub_connect_string)
JAAS_CONFIG='org.apache.kafka.common.security.plain.PlainLoginModule required username="$ConnectionString" password="'
KAFKA_JAAS_CONFIG="${JAAS_CONFIG}${KAFKA_CONNECTION_STRING}\";"
echo

# Create a Log Analytics workspace
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $LOG_ANALYTICS_WORKSPACE Log Analytics workspace"
echo "-----------------------------------------"
az monitor log-analytics workspace create \
  --resource-group "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --tags system="$TAG_SYSTEM" \
  --workspace-name "$LOG_ANALYTICS_WORKSPACE"
echo

# Retrieve the Log Analytics Client ID & Secret
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Retrieving the Log Analytics Client ID and secret"
echo "-----------------------------------------"
LOG_ANALYTICS_WORKSPACE_CLIENT_ID=$(az monitor log-analytics workspace show  \
  --resource-group "$RESOURCE_GROUP" \
  --workspace-name "$LOG_ANALYTICS_WORKSPACE" \
  --query customerId  \
  --output tsv | tr -d '[:space:]')

LOG_ANALYTICS_WORKSPACE_CLIENT_SECRET=$(az monitor log-analytics workspace get-shared-keys \
  --resource-group "$RESOURCE_GROUP" \
  --workspace-name "$LOG_ANALYTICS_WORKSPACE" \
  --query primarySharedKey \
  --output tsv | tr -d '[:space:]')
echo

# Create the Container Apps environment
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $CONTAINERAPPS_ENVIRONMENT Container Apps environment"
echo "-----------------------------------------"
az containerapp env create \
  --resource-group "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --tags system="$TAG_SYSTEM" \
  --name "$CONTAINERAPPS_ENVIRONMENT" \
  --logs-workspace-id "$LOG_ANALYTICS_WORKSPACE_CLIENT_ID" \
  --logs-workspace-key "$LOG_ANALYTICS_WORKSPACE_CLIENT_SECRET"
echo

# Create Apicurio
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $APICURIO_APP Container App"
echo "-----------------------------------------"
create_apicurio
echo

# Create heroes
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $HEROES_APP Container App"
echo "-----------------------------------------"
create_heroes_app
echo

# Create villains
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $VILLAINS_APP Container App"
echo "-----------------------------------------"
create_villains_app
echo

# Create statistics
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $STATISTICS_APP Container App"
echo "-----------------------------------------"
create_statistics_app
echo

# Create fights
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $FIGHTS_APP Container App"
echo "-----------------------------------------"
create_fights_app
echo

# Create UI
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $UI_APP Container App"
echo "-----------------------------------------"
create_ui_app
echo

cleanup

echo
echo "Deployment took $SECONDS seconds to complete."

echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: All services have been deployed"
echo "-----------------------------------------"
echo "  Super Heroes UI: $UI_URL"
echo "  Event stats: $STATISTICS_URL"
echo "  Fights URL: $FIGHTS_URL"
echo "  Heroes URL: $HEROES_URL"
echo "  Villains URL: $VILLAINS_URL"
echo "  Apicurio Schema Registry: $APICURIO_URL"

if "$CREATE_CONTAINER_REGISTRY"; then
  echo "  Container Registry URL: $CONTAINER_REGISTRY_URL"
fi

echo
