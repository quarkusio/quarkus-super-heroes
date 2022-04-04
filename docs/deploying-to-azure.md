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
# Kafka
EVENTHUB_NAMESPACE="super-heroes-eventhub"
EVENTHUB_TOPIC="fights"
# Postgres
POSTGRES_DB="super-heroes-db"
POSTGRES_DB_ADMIN="superheroesadmin"
POSTGRES_DB_PWD="super-heroes-p#ssw0rd-12046"
# Heroes
HEROES_APP="rest-heroes-app"
HEROES_DB_SCHEMA="heroes_database"
# Villains
VILLAINS_APP="rest-villains-app"
VILLAINS_DB_SCHEMA="villains_database"
# Fights
FIGHTS_APP="rest-fights-app"
FIGHTS_DB="failure-db"
FIGHTS_DB_SCHEMA="failures"
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

## Deploying the Application

### The Heroes Microservice

