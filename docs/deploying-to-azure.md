# Table of Contents

# Introduction
[Azure Container Apps](https://docs.microsoft.com/en-us/azure/container-apps/) allows to run containerized applications without worrying about orchestration or infrastructure (i.e. we don't have to directly use K8s, it's used under the hoods).
This guide goes through setting up the Azure environment and deploying the Super Hero microservices.

## Getting ready with Azure

First of all, you need an Azure subscription.
If you don't have one, go to https://signup.azure.com and register.
Also make sure you have [Azure CLI installed](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli) on your machine.

Once everything is installed, sign in to Azure from the CLI:

```shell
az login
```

Install the Azure Container Apps extension for the Azure CLI:

```shell
az extension add --source https://workerappscliextension.blob.core.windows.net/azure-cli-extension/containerapp-0.2.4-py2.py3-none-any.whl
az extension add --name rdbms-connect
```

Register the Microsoft.Web namespace

```shell
az provider register --namespace Microsoft.Web
```

## Setting Up the Azure Environment

The Azure Container Apps environment will be created using a set of Azure CLI commands.
Set the following environment variables

```shell
RESOURCE_GROUP="super-heroes"
LOCATION="eastus2"
# Container Apps
LOG_ANALYTICS_WORKSPACE="super-heroes-logs"
CONTAINERAPPS_ENVIRONMENT="super-heroes-env"
# Postgres
POSTGRES_DB="super-heroes-db"
POSTGRES_DB_ADMIN="superheroesadmin"
POSTGRES_DB_PWD="super-heroes-p#ssw0rd-12046"
# MongoDB
MONGO_DB="fights-db"
# Kafka
KAFKA_NAMESPACE="fights-kafka"
KAFKA_TOPIC="fights"
# Heroes
HEROES_APP="rest-heroes-app"
HEROES_DB_SCHEMA="heroes_database"
# Villains
VILLAINS_APP="rest-villains-app"
VILLAINS_DB_SCHEMA="villains_database"
# Fights
FIGHTS_APP="rest-fights-app"
FIGHTS_DB_SCHEMA="fights"
```

### Create a resource group

All the resources in Azure have to belong to a resource group.
Execute the following command to create a resource group:

```shell
az group create \
  --name $RESOURCE_GROUP \
  --location $LOCATION
```

### Create a Container Apps environment

Create a Log Analytics workspace:

```shell
az monitor log-analytics workspace create \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --workspace-name $LOG_ANALYTICS_WORKSPACE
```

Retrieve the Log Analytics Client ID and client secret:

```shell
LOG_ANALYTICS_WORKSPACE_CLIENT_ID=`az monitor log-analytics workspace show --query customerId -g $RESOURCE_GROUP -n $LOG_ANALYTICS_WORKSPACE -o tsv | tr -d '[:space:]'`
echo $LOG_ANALYTICS_WORKSPACE_CLIENT_ID
LOG_ANALYTICS_WORKSPACE_CLIENT_SECRET=`az monitor log-analytics workspace get-shared-keys --query primarySharedKey -g $RESOURCE_GROUP -n $LOG_ANALYTICS_WORKSPACE -o tsv | tr -d '[:space:]'`
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

### Create the managed Postgres Database

We need to create a PostgreSQL so the Heroes and Villains microservice can store data.
Create the database with the following command:

```shell
az postgres flexible-server create \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --name $POSTGRES_DB \
  --admin-user $POSTGRES_DB_ADMIN \
  --admin-password $POSTGRES_DB_PWD \
  --public all \
  --sku-name Standard_D2s_v3 \
  --storage-size 4096 \
  --version 13
```

Then, we create two database schemas, one for Heroes, another one for Villains

```shell
az postgres flexible-server db create \
    --resource-group $RESOURCE_GROUP \
    --server-name $POSTGRES_DB \
    --database-name $HEROES_DB_SCHEMA
```

```shell
az postgres flexible-server db create \
    --resource-group $RESOURCE_GROUP \
    --server-name $POSTGRES_DB \
    --database-name $VILLAINS_DB_SCHEMA
```

Add data to both databases using the following commands:

```shell
az postgres flexible-server execute \
    --name $POSTGRES_DB \
    --admin-user $POSTGRES_DB_ADMIN \
    --admin-password $POSTGRES_DB_PWD \
    --database-name $HEROES_DB_SCHEMA \
    --file-path rest-heroes/deploy/db-init/initialize-tables.sql
```

```shell
az postgres flexible-server execute \
    --name $POSTGRES_DB \
    --admin-user $POSTGRES_DB_ADMIN \
    --admin-password $POSTGRES_DB_PWD \
    --database-name $VILLAINS_DB_SCHEMA \
    --file-path rest-villains/deploy/db-init/initialize-tables.sql
```

You can check the content of the tables with the following commands:

```shell
az postgres flexible-server execute \
    --name $POSTGRES_DB \
    --admin-user $POSTGRES_DB_ADMIN \
    --admin-password $POSTGRES_DB_PWD \
    --database-name $HEROES_DB_SCHEMA \
    --querytext "select * from hero"
```

```shell
az postgres flexible-server execute \
    --name $POSTGRES_DB \
    --admin-user $POSTGRES_DB_ADMIN \
    --admin-password $POSTGRES_DB_PWD \
    --database-name $VILLAINS_DB_SCHEMA \
    --querytext "select * from villain"
```

If you want to access the Postgres database from your local machine, you need to give access to your local IP address.
A convenient way to know the local IP address is to go to http://whatismyip.akamai.com:

```shell
az postgres flexible-server firewall-rule create \
    --resource-group $RESOURCE_GROUP \
    --rule-name $POSTGRES_DB-allow-local-ip \
    --name $POSTGRES_DB \
    --start-ip-address <LOCAL_IP_ADDRESS> \
    --end-ip-address <LOCAL_IP_ADDRESS>
```

You can check the firewall rules with:

````shell
az postgres flexible-server firewall-rule list  \
    --resource-group $RESOURCE_GROUP \
    --name $POSTGRES_DB \
    --out table
````

Get the connection string with the following command so you can connect to it:

```shell
az postgres flexible-server show-connection-string \
  --database-name $POSTGRES_DB \
  --query connectionStrings.jdbc
```

### Create the managed MongoDB Database

We need to create a MongoDB so the Fight microservice can store data.
Create a database in the region where it's available:

```shell
az cosmosdb create \
  --resource-group $RESOURCE_GROUP \
  --locations regionName="$LOCATION" failoverPriority=0 \
  --name $MONGO_DB \
  --kind MongoDB \
  --server-version 4.0
```

Create the Fight collection:

````shell
az cosmosdb mongodb database create \
  --resource-group $RESOURCE_GROUP \
  --account-name $MONGO_DB \
  --name $FIGHTS_DB_SCHEMA
````

The Book microservice communicates with the Book Fail microservice through Kafka.
We need to create an Azure event hub for that.

```shell
az eventhubs namespace create \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --name $EVENTHUB_NAMESPACE
```

```shell
az eventhubs eventhub create \
  --resource-group $RESOURCE_GROUP \
  --name $EVENTHUB_TOPIC \
  --namespace-name $EVENTHUB_NAMESPACE
```

### Create the Managed Kafka

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

## Deploying the Application

Now that the Azure Container Apps environment is all set, we need to configure our microservices, build them as Docker images, push them to Docker Hub, and deploy these images to Azure Container Apps.
So let's configure, build, push and deploy each Microservice.

### Heroes Microservice

The Heroes microservice needs to access the managed Postgres database.
Therefore, we need to set the right properties in the file `application-azure.yml`.
We know the values for the Postgres database, admin and password (our `POSTGRES_DB`, `POSTGRES_DB_ADMIN` and `POSTGRES_DB_PWD` variables), so the configuration looks like this:

```shell
"%azure":
  quarkus:
    datasource:
      username: superheroesadmin
      password: super-heroes-p#ssw0rd-12046
      reactive:
        url: postgresql://super-heroes-db.postgres.database.azure.com:5432/heroes_database?ssl=true&sslmode=require
```

We build the Heroes microservice with the following command:

```shell
rest-heroes$ mvn clean package -Dmaven.test.skip=true -Dquarkus.container-image.build=true -Dquarkus.container-image.tag=$TAG
```

This creates the Docker image `quay.io/quarkus-super-heroes/rest-heroes:azure`

```shell
docker login quay.io
docker push quay.io/quarkus-super-heroes/rest-heroes:azure
```

The following command will deploy the Heroes image to Azure Container Apps and set the URL of the deployed application to the `HEROES_URL` variable:

```shell
HEROES_URL=$(az containerapp create \
  --resource-group $RESOURCE_GROUP \
  --image agoncal/rest-heroes:$TAG \
  --name $HEROES_APP \
  --environment $CONTAINERAPPS_ENVIRONMENT \
  --ingress external \
  --target-port 8083 \
  --environment-variables QUARKUS_PROFILE=azure \
  --query configuration.ingress.fqdn \
  --output tsv)
echo $HEROES_URL  
```

You can now invoke the Hero microservice APIs with:

```shell
curl https://$HEROES_URL/api/heroes | jq
```
