# Table of Contents
- [Introduction](#introduction)
- [Getting ready with Azure](#getting-ready-with-azure)
- [Automated Installation](#automated-installation)
- [Manual Installation](#manual-installation)
    - [Setting up the Azure environment](#setting-up-the-azure-environment)
    - [Create a resource group](#create-a-resource-group)
    - [Create a Container Apps environment](#create-a-container-apps-environment)
    - [Create the managed Postgres databases](#create-the-managed-postgres-databases)
    - [Create the managed MongoDB database](#create-the-managed-mongodb-database)
    - [Create the managed Kafka](#create-the-managed-kafka)
    - [Create the Schema Registry](#create-the-schema-registry)
    - [Deploying the applications](#deploying-the-applications)
        - [Heroes microservice](#heroes-microservice)
        - [Villains microservice](#villains-microservice)
        - [Statistics Microservice](#statistics-microservice)
        - [Fights microservice](#fights-microservice)
        - [Super Hero UI](#super-hero-ui)
- [Miscellaneous](#miscellaneous)

# Introduction
[Azure Container Apps](https://docs.microsoft.com/en-us/azure/container-apps/) allows to run containerized applications without worrying about orchestration or infrastructure (i.e. we don't have to directly use K8s, it's used under the hood).
This guide goes through setting up the Azure environment, required backing services, and deploying the Super Hero microservices. Some of the services (i.e. databases/Kafka/etc) have been replaced with Azure services. This diagram shows the overall architecture:

![application-architecture-azure-containerapps](../images/application-architecture-azure-containerapps.png)

# Getting ready with Azure

First of all, you need an Azure subscription.
If you don't have one, go to https://signup.azure.com and register.
Also make sure you have [Azure CLI installed](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli) on your machine.

Once everything is installed, sign in to Azure from the CLI:

```shell
az login
```

# Automated Installation
The entire system can be deployed in an automated fashion by running the [`deploy-to-azure-containerapps.sh` script](../scripts/deploy-to-azure-containerapps.sh). This script can be downloaded and run outside of this repo as well.

Run `deploy-to-azure-containerapps.sh -h` for information and configuration options.

# Manual Installation
## Setting Up the Azure Environment

Install the Azure Container Apps extension for the Azure CLI:

```shell
az extension add --name containerapp
az extension add --name rdbms-connect
```

Register the Microsoft.App namespace

```shell
az provider register --namespace Microsoft.App --wait
```

The Azure Container Apps environment will be created using a set of Azure CLI commands.
Set the following environment variables:

```shell
RESOURCE_GROUP="super-heroes"
LOCATION="eastus2"
TAG="native-java17-latest"
SUPERHEROES_IMAGES_BASE="quay.io/quarkus-super-heroes"
# Need this because some Azure services need to have unique names in a region
UNIQUE_IDENTIFIER=$(whoami)
# Container Apps
LOG_ANALYTICS_WORKSPACE="super-heroes-logs"
CONTAINERAPPS_ENVIRONMENT="super-heroes-env"
# Postgres
POSTGRES_DB_ADMIN="superheroesadmin"
POSTGRES_DB_PWD="super-heroes-p#ssw0rd-12046"
POSTGRES_DB_VERSION="13"
# MongoDB
MONGO_DB="fights-db-$UNIQUE_IDENTIFIER"
MONGO_DB_VERSION="4.0"
# Kafka
KAFKA_NAMESPACE="fights-kafka-$UNIQUE_IDENTIFIER"
KAFKA_TOPIC="fights"
KAFKA_BOOTSTRAP_SERVERS="$KAFKA_NAMESPACE.servicebus.windows.net:9093"
# Apicurio
APICURIO_APP="apicurio"
APICURIO_IMAGE="apicurio/apicurio-registry-mem:2.2.0.Final"
# Heroes
HEROES_APP="rest-heroes"
HEROES_DB="heroes-db-$UNIQUE_IDENTIFIER"
HEROES_DB_SCHEMA="heroes"
HEROES_IMAGE="${SUPERHEROES_IMAGES_BASE}/${HEROES_APP}:${TAG}"
HEROES_DB_CONNECT_STRING="postgresql://${HEROES_DB}.postgres.database.azure.com:5432/${HEROES_DB_SCHEMA}?ssl=true&sslmode=require"
# Villains
VILLAINS_APP="rest-villains"
VILLAINS_DB="villains-db-$UNIQUE_IDENTIFIER"
VILLAINS_DB_SCHEMA="villains"
VILLAINS_IMAGE="${SUPERHEROES_IMAGES_BASE}/${VILLAINS_APP}:${TAG}"
VILLAINS_DB_CONNECT_STRING="jdbc:postgresql://${VILLAINS_DB}.postgres.database.azure.com:5432/${VILLAINS_DB_SCHEMA}?ssl=true&sslmode=require"
# Fights
FIGHTS_APP="rest-fights"
FIGHTS_DB_SCHEMA="fights"
FIGHTS_IMAGE="${SUPERHEROES_IMAGES_BASE}/${FIGHTS_APP}:${TAG}"
# Statistics
STATISTICS_APP="event-statistics"
STATISTICS_IMAGE="${SUPERHEROES_IMAGES_BASE}/${STATISTICS_APP}:${TAG}"
# UI
UI_APP="ui-super-heroes"
UI_IMAGE="${SUPERHEROES_IMAGES_BASE}/${UI_APP}:latest"
```

## Create a resource group

All the resources in Azure have to belong to a resource group.
Execute the following command to create a resource group:

```shell
az group create \
  --name $RESOURCE_GROUP \
  --location $LOCATION
```

## Create a Container Apps environment

Create a Log Analytics workspace:

```shell
az monitor log-analytics workspace create \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --workspace-name $LOG_ANALYTICS_WORKSPACE
```

Retrieve the Log Analytics Client ID and client secret:

```shell
LOG_ANALYTICS_WORKSPACE_CLIENT_ID=`az monitor log-analytics workspace show  \
  --resource-group $RESOURCE_GROUP \
  --workspace-name $LOG_ANALYTICS_WORKSPACE \
  --query customerId  \
  --output tsv | tr -d '[:space:]'`

echo $LOG_ANALYTICS_WORKSPACE_CLIENT_ID

LOG_ANALYTICS_WORKSPACE_CLIENT_SECRET=`az monitor log-analytics workspace get-shared-keys \
  --resource-group $RESOURCE_GROUP \
  --workspace-name $LOG_ANALYTICS_WORKSPACE \
  --query primarySharedKey \
  --output tsv | tr -d '[:space:]'`

echo $LOG_ANALYTICS_WORKSPACE_CLIENT_SECRET
```

Create the container apps environment:

````shell
az containerapp env create \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --name $CONTAINERAPPS_ENVIRONMENT \
  --logs-workspace-id $LOG_ANALYTICS_WORKSPACE_CLIENT_ID \
  --logs-workspace-key $LOG_ANALYTICS_WORKSPACE_CLIENT_SECRET
````

## Create the managed Postgres Databases

We need to create two PostgreSQL databases so the Heroes and Villains microservice can store data.
Because we also want to access these database from external SQL client, we make them available to the outside world thanks to the `-public all` parameter.
Create the two databases with the following commands:

```shell
az postgres flexible-server create \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --name $HEROES_DB \
  --admin-user $POSTGRES_DB_ADMIN \
  --admin-password $POSTGRES_DB_PWD \
  --public all \
  --sku-name Standard_D2s_v3 \
  --storage-size 4096 \
  --version $POSTGRES_DB_VERSION
```

```shell
az postgres flexible-server create \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --name $VILLAINS_DB \
  --admin-user $POSTGRES_DB_ADMIN \
  --admin-password $POSTGRES_DB_PWD \
  --public all \
  --sku-name Standard_D2s_v3 \
  --storage-size 4096 \
  --version $POSTGRES_DB_VERSION
```

Then, we create two database schemas, one for Heroes, another one for Villains

```shell
az postgres flexible-server db create \
    --resource-group $RESOURCE_GROUP \
    --server-name $HEROES_DB \
    --database-name $HEROES_DB_SCHEMA
```

```shell
az postgres flexible-server db create \
    --resource-group $RESOURCE_GROUP \
    --server-name $VILLAINS_DB \
    --database-name $VILLAINS_DB_SCHEMA
```

Add data to both databases using the following commands:

```shell
az postgres flexible-server execute \
    --name $HEROES_DB \
    --admin-user $POSTGRES_DB_ADMIN \
    --admin-password $POSTGRES_DB_PWD \
    --database-name $HEROES_DB_SCHEMA \
    --file-path rest-heroes/deploy/db-init/initialize-tables.sql
```

```shell
az postgres flexible-server execute \
    --name $VILLAINS_DB \
    --admin-user $POSTGRES_DB_ADMIN \
    --admin-password $POSTGRES_DB_PWD \
    --database-name $VILLAINS_DB_SCHEMA \
    --file-path rest-villains/deploy/db-init/initialize-tables.sql
```

You can check the content of the tables with the following commands:

```shell
az postgres flexible-server execute \
    --name $HEROES_DB \
    --admin-user $POSTGRES_DB_ADMIN \
    --admin-password $POSTGRES_DB_PWD \
    --database-name $HEROES_DB_SCHEMA \
    --querytext "select * from hero"
```

```shell
az postgres flexible-server execute \
    --name $VILLAINS_DB \
    --admin-user $POSTGRES_DB_ADMIN \
    --admin-password $POSTGRES_DB_PWD \
    --database-name $VILLAINS_DB_SCHEMA \
    --querytext "select * from villain"
```

If you'd like to see the connection strings to the databases, ise the following commands:

```shell
az postgres flexible-server show-connection-string \
  --database-name $HEROES_DB_SCHEMA \
  --server-name $HEROES_DB \
  --admin-user $POSTGRES_DB_ADMIN \
  --admin-password $POSTGRES_DB_PWD \
  --query connectionStrings.jdbc \
  --out tsv

az postgres flexible-server show-connection-string \
  --database-name $VILLAINS_DB_SCHEMA \
  --server-name $VILLAINS_DB \
  --admin-user $POSTGRES_DB_ADMIN \
  --admin-password $POSTGRES_DB_PWD \
  --query connectionStrings.jdbc \
  --out tsv
```

> **NOTE:** These aren't the actual connection strings used, especially in the heroes service, which does not use JDBC.
> 
> You also need to append `ssl=true&sslmode=require` to the end of each connect string to force the driver to use ssl.
> 
> These commands are just here for your own examination purposes.

## Create the managed MongoDB Database

We need to create a MongoDB so the Fight microservice can store data.
Create a database in the region where it's available:

```shell
az cosmosdb create \
  --resource-group $RESOURCE_GROUP \
  --locations regionName="$LOCATION" failoverPriority=0 \
  --name $MONGO_DB \
  --kind MongoDB \
  --server-version $MONGO_DB_VERSION
```

Create the Fight collection:

````shell
az cosmosdb mongodb database create \
  --resource-group $RESOURCE_GROUP \
  --account-name $MONGO_DB \
  --name $FIGHTS_DB_SCHEMA
````

To configure Kafka, first get the connection string of the event hub namespace.

```shell
MONGO_CONNECTION_STRING=$(az cosmosdb keys list \
  --resource-group $RESOURCE_GROUP \
  --name $MONGO_DB \
  --type connection-strings \
  --query "connectionStrings[?description=='Primary MongoDB Connection String'].connectionString" \
  --output tsv)

echo $MONGO_CONNECTION_STRING
```


## Create the Managed Kafka

The Fight microservice communicates with the Statistics microservice through Kafka.
We need to create an Azure event hub for that.

```shell
az eventhubs namespace create \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --name $KAFKA_NAMESPACE
```

```shell
az eventhubs eventhub create \
  --resource-group $RESOURCE_GROUP \
  --name $KAFKA_TOPIC \
  --namespace-name $KAFKA_NAMESPACE
```

To configure Kafka, first get the connection string of the event hub namespace.

```shell
KAFKA_CONNECTION_STRING=$(az eventhubs namespace authorization-rule keys list \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $KAFKA_NAMESPACE \
  --name RootManageSharedAccessKey \
  --output json | jq -r .primaryConnectionString)
  
JAAS_CONFIG='org.apache.kafka.common.security.plain.PlainLoginModule required username="$ConnectionString" password="'
KAFKA_JAAS_CONFIG="${JAAS_CONFIG}${KAFKA_CONNECTION_STRING}\";"

echo $KAFKA_CONNECTION_STRING
echo $KAFKA_JAAS_CONFIG
```


## Create the Schema Registry

Messages sent and consumed from Kafka need to be validated against a schema.
These schemas are deployed to Apicurio.
Notice the `--min-replicas 1` so Apicurio does not scale to 0 and is always available:

```shell
az containerapp create \
  --resource-group $RESOURCE_GROUP \
  --image $APICURIO_IMAGE \
  --name $APICURIO_APP \
  --environment $CONTAINERAPPS_ENVIRONMENT \
  --env-vars REGISTRY_AUTH_ANONYMOUS_READ_ACCESS_ENABLED=true \
  --ingress external \
  --target-port 8080 \
  --min-replicas 1
```

```shell
APICURIO_URL=$(az containerapp ingress show \
  --name $APICURIO_APP \
  --resource-group $RESOURCE_GROUP \
  --output json | jq -r .fqdn)
  
echo $APICURIO_URL  
```

Because we are running on HTTPS, we need to set the APICURIO_URL in the following variables:

```shell
APICURIO_REGISTRY_UI_CONFIG_APIURL="https://${APICURIO_URL}/apis/registry"
APICURIO_REGISTRY_UI_CONFIG_UIURL="https://${APICURIO_URL}/ui"
```

And then, update the Apicurio container with these new variables:

```shell
az containerapp update \
  --resource-group $RESOURCE_GROUP \
  --name $APICURIO_APP \
  --set-env-vars REGISTRY_UI_CONFIG_APIURL=$APICURIO_REGISTRY_UI_CONFIG_APIURL \
                 REGISTRY_UI_CONFIG_UIURL=$APICURIO_REGISTRY_UI_CONFIG_UIURL
```

You can go to the Apicurio web console:

```shell
open $APICURIO_REGISTRY_UI_CONFIG_UIURL
```

## Deploying the Applications

Now that the Azure Container Apps environment is all set, we need to configure our microservices, build them as Docker images, push them to Docker Hub, and deploy these images to Azure Container Apps.
So let's configure, build, push and deploy each Microservice.

### Heroes Microservice

The Heroes microservice needs to access the managed Postgres database.
Therefore, we need to set the right properties using our environment variables.
Notice that the Heroes microservice is not accessible from outside.
If you want to access it, you can add `--ingress external --target-port 8083` to the following command:

```shell
az containerapp create \
  --resource-group $RESOURCE_GROUP \
  --image $HEROES_IMAGE \
  --name $HEROES_APP \
  --environment $CONTAINERAPPS_ENVIRONMENT \
  --ingress external \
  --target-port 8083 \
  --min-replicas 1 \
  --env-vars QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION=validate \
             QUARKUS_HIBERNATE_ORM_SQL_LOAD_SCRIPT=no-file \
             QUARKUS_DATASOURCE_USERNAME=$POSTGRES_DB_ADMIN \
             QUARKUS_DATASOURCE_PASSWORD=$POSTGRES_DB_PWD \
             QUARKUS_DATASOURCE_REACTIVE_URL="$HEROES_DB_CONNECT_STRING"
```

The following command sets the URL of the deployed application to the `HEROES_URL` variable:

```shell
HEROES_URL=$(az containerapp ingress show \
  --name $HEROES_APP \
  --resource-group $RESOURCE_GROUP \
  --output json | jq -r .fqdn)
    
echo $HEROES_URL
```
You can now invoke the Hero microservice APIs with:

```shell
curl https://$HEROES_URL/api/heroes/hello
curl https://$HEROES_URL/api/heroes | jq
```

To access the logs of the Heroes microservice, you can write the following query:

````shell
az monitor log-analytics query \
--workspace $LOG_ANALYTICS_WORKSPACE_CLIENT_ID \
--analytics-query "ContainerAppConsoleLogs_CL | where ContainerAppName_s == '$HEROES_APP' | project ContainerAppName_s, Log_s, TimeGenerated " \
--out table
````

### Villains Microservice

The Villain microservice also needs to access the managed Postgres database, so we need to set the right variables:

```shell
az containerapp create \
  --resource-group $RESOURCE_GROUP \
  --image $VILLAINS_IMAGE \
  --name $VILLAINS_APP \
  --environment $CONTAINERAPPS_ENVIRONMENT \
  --ingress external \
  --target-port 8084 \
  --min-replicas 1 \
  --env-vars QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION=validate \
             QUARKUS_HIBERNATE_ORM_SQL_LOAD_SCRIPT=no-file \
             QUARKUS_DATASOURCE_USERNAME=$POSTGRES_DB_ADMIN \
             QUARKUS_DATASOURCE_PASSWORD=$POSTGRES_DB_PWD \
             QUARKUS_DATASOURCE_JDBC_URL="$VILLAINS_DB_CONNECT_STRING"
```

The following command sets the URL of the deployed application to the `VILLAINS_URL` variable:

```shell
VILLAINS_URL=$(az containerapp ingress show \
  --name $VILLAINS_APP \
  --resource-group $RESOURCE_GROUP \
  --output json | jq -r .fqdn)
  
echo $VILLAINS_URL
```
You can now invoke the Hero microservice APIs with:

```shell
curl https://$VILLAINS_URL/api/villains/hello
curl https://$VILLAINS_URL/api/villains | jq
```

To access the logs of the Villain microservice, you can write the following query:

````shell
az monitor log-analytics query \
--workspace $LOG_ANALYTICS_WORKSPACE_CLIENT_ID \
--analytics-query "ContainerAppConsoleLogs_CL | where ContainerAppName_s == '$VILLAINS_APP' | project ContainerAppName_s, Log_s, TimeGenerated " \
--out table
````

### Statistics Microservice

The Statistics microservice listens to a Kafka topics and consumes all the fights.
The fight messages are defined by an Avro schema stored in Apicurio (`APICURIO_URL` and we append `/apis/registry/v2`):.
Notice that we use the value of the `$$KAFKA_JAAS_CONFIG` in the `password`.

```shell
az containerapp create \
  --resource-group $RESOURCE_GROUP \
  --image $STATISTICS_IMAGE \
  --name $STATISTICS_APP \
  --environment $CONTAINERAPPS_ENVIRONMENT \
  --ingress external \
  --target-port 8085 \
  --min-replicas 1 \
  --env-vars KAFKA_BOOTSTRAP_SERVERS=$KAFKA_BOOTSTRAP_SERVERS \
             KAFKA_SECURITY_PROTOCOL=SASL_SSL \
             KAFKA_SASL_MECHANISM=PLAIN \
             KAFKA_SASL_JAAS_CONFIG="$KAFKA_JAAS_CONFIG" \
             MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_APICURIO_REGISTRY_URL=https://${APICURIO_URL}/apis/registry/v2
```

The following command sets the URL of the deployed application to the `STATISTICS_URL` variable:


```shell
STATISTICS_URL=$(az containerapp ingress show \
  --name $STATISTICS_APP \
  --resource-group $RESOURCE_GROUP \
  --output json | jq -r .fqdn)
  
echo $STATISTICS_URL  
```

You can now display the Statistics UI with:

```shell
open https://$STATISTICS_URL
```

To access the logs of the Statistics microservice, you can write the following query:

````shell
az monitor log-analytics query \
--workspace $LOG_ANALYTICS_WORKSPACE_CLIENT_ID \
--analytics-query "ContainerAppConsoleLogs_CL | where ContainerAppName_s == '$STATISTICS_APP' | project ContainerAppName_s, Log_s, TimeGenerated " \
--out table
````

### Fights Microservice

The Fight microservice invokes the Heroes and Villains microserivces, sends fight messages to a Kafka topics and stores the fights into a MongoDB database.
We need to configure Kafka (same connection string as the one used by the Statistics microservice) as well as Mongo and Apicurio (variable `APICURIO_URL` and append `apis/registry/v2`).
As for the microservice invocations, you need to set the URLs of both Heroes and Villains microservices.

```shell
az containerapp create \
  --resource-group $RESOURCE_GROUP \
  --image $FIGHTS_IMAGE \
  --name $FIGHTS_APP \
  --environment $CONTAINERAPPS_ENVIRONMENT \
  --ingress external \
  --target-port 8082 \
  --min-replicas 1 \
  --env-vars QUARKUS_LIQUIBASE_MONGODB_MIGRATE_AT_START=false \
             QUARKUS_MONGODB_CONNECTION_STRING=$MONGO_CONNECTION_STRING \
             QUARKUS_REST_CLIENT_HERO_CLIENT_URL=https://$HEROES_URL \
             FIGHT_VILLAIN_CLIENT_BASE_URL=https://$VILLAINS_URL \
             KAFKA_BOOTSTRAP_SERVERS=$KAFKA_BOOTSTRAP_SERVERS \
             KAFKA_SECURITY_PROTOCOL=SASL_SSL \
             KAFKA_SASL_MECHANISM=PLAIN \
             KAFKA_SASL_JAAS_CONFIG="$KAFKA_JAAS_CONFIG" \
             MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_APICURIO_REGISTRY_URL=https://${APICURIO_URL}/apis/registry/v2
```

The following command sets the URL of the deployed application to the `FIGHTS_URL` variable:

```shell
FIGHTS_URL=$(az containerapp ingress show \
--name $FIGHTS_APP \
--resource-group $RESOURCE_GROUP \
--output json | jq -r .fqdn)

echo $FIGHTS_URL
```

```shell
curl https://$FIGHTS_URL/api/fights/hello
curl https://$FIGHTS_URL/api/fights/hello/villains
curl https://$FIGHTS_URL/api/fights/hello/heroes
curl https://$FIGHTS_URL/api/fights | jq
curl https://$FIGHTS_URL/api/fights/randomfighters | jq
```

To access the logs of the Fight microservice, you can write the following query:

````shell
az monitor log-analytics query \
--workspace $LOG_ANALYTICS_WORKSPACE_CLIENT_ID \
--analytics-query "ContainerAppConsoleLogs_CL | where ContainerAppName_s == '$FIGHTS_APP' | project ContainerAppName_s, Log_s, TimeGenerated " \
--out table
````

### Super Hero UI
#### Super Hero UI (using Azure Container Apps)

```shell
az containerapp create \
  --resource-group $RESOURCE_GROUP \
  --image $UI_IMAGE \
  --name $UI_APP \
  --environment $CONTAINERAPPS_ENVIRONMENT \
  --ingress external \
  --target-port 8080 \
  --env-vars API_BASE_URL=https://$FIGHTS_URL
```

```shell
UI_URL=$(az containerapp ingress show \
  --name $UI_APP \
  --resource-group $RESOURCE_GROUP \
  --output json | jq -r .fqdn)
  
echo $UI_URL  
```

```shell
open http://$UI_URL
```

#### Super Hero UI (optional using Azure Static Webapps)

If you are building the UI locally with Node 17 you have to set the `NODE_OPTIONS` variable:

```shell
node version
export NODE_OPTIONS=--openssl-legacy-provider
```

Then, to execute the app locally, set `API_BASE_URL` with the same value of the Fight microservice URL (so it accesses the remote Fight microservice): 

```shell
export API_BASE_URL=https://${FIGHT_URL}/api
ui-super-heroes$ npm install && npm run build && npm start
```

You can check the URL is correctly set with:

```shell
curl http://localhost:8080/env.js
```

Then, we will deploy the Angular application using [Azure Static Webapps](https://azure.microsoft.com/en-us/services/app-service/static).
This creates a GitHub action and deploys the application each time you push the code:

```shell
az staticwebapp create \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --name $UI_APP \
  --source https://github.com/agoncal/quarkus-super-heroes \
  --branch azure \
  --app-location /ui-super-heroes \
  --login-with-github
```

If you have an issue with secrets, you can list the secrets that exist for the static web app:

```shell
az staticwebapp secrets list  \
  --resource-group $RESOURCE_GROUP \
  --name $UI_APP \
  --out table
```

# Miscellaneous

## Restarting a Microservice

If you need to restart a microservice, you need to actually restart the active revision.
For that, first get the active revision:

```shell
az containerapp revision list \
  --resource-group $RESOURCE_GROUP \
  --name $FIGHTS_APP \
  --out table
```

Then, restart it:

```shell
az containerapp revision restart \
  --resource-group $RESOURCE_GROUP \
  --app $FIGHTS_APP \
  --name rest-fights-app--mh396rg
```

## Redeploying a new version of a microservice

If you need to push a new version of a Docker image, make sure it has a different tag.
Then, update the container with this new tagged image:

```shell
az containerapp update \
  --resource-group $RESOURCE_GROUP \
  --image quay.io/quarkus-super-heroes/rest-fights:azure2 \
  --name $FIGHTS_APP
```

```shell
az containerapp update \
  --resource-group $RESOURCE_GROUP \
  --name $APICURIO_APP \
  --set-env-vars QUARKUS_LOG_LEVEL=DEBUG
```
