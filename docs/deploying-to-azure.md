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
az extension add --name containerapp
az extension add --name rdbms-connect
```

Register the Microsoft.Web namespace

```shell
az provider register --namespace Microsoft.App
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
SCHEMA_REGISTRY_GROUP="fights-group"
# Apicurio
APICURIO_APP="apicurio-app"
# Heroes
HEROES_APP="rest-heroes-app"
HEROES_DB_SCHEMA="heroes_database"
# Villains
VILLAINS_APP="rest-villains-app"
VILLAINS_DB_SCHEMA="villains_database"
# Fights
FIGHTS_APP="rest-fights-app"
FIGHTS_DB_SCHEMA="fights"
# Statistics
STATISTICS_APP="event-statistics-app"
# UI
UI_APP="super-heroes-app"
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
  --query connectionStrings.jdbc \
  --out tsv
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

```shell
APICURIO_URL=$(az containerapp create \
  --resource-group $RESOURCE_GROUP \
  --image apicurio/apicurio-registry-mem:2.1.5.Final \
  --name $APICURIO_APP \
  --environment $CONTAINERAPPS_ENVIRONMENT \
  --ingress external \
  --target-port 8080 \
  --query properties.configuration.ingress.fqdn \
  --output tsv)
  
echo $APICURIO_URL  
```

You can go to the Apicurio web console:

```shell
open https://$APICURIO_URL/ui
```

## Deploying the Application

Now that the Azure Container Apps environment is all set, we need to configure our microservices, build them as Docker images, push them to Docker Hub, and deploy these images to Azure Container Apps.
So let's configure, build, push and deploy each Microservice.

### Heroes Microservice

The Heroes microservice needs to access the managed Postgres database.
Therefore, we need to set the right properties in the file `application-azure.yml`.
We know the values for the Postgres database, admin and password (our `POSTGRES_DB`, `POSTGRES_DB_ADMIN` and `POSTGRES_DB_PWD` variables), so the configuration looks like this:

```shell
quarkus:
  datasource:
    username: superheroesadmin
    password: super-heroes-p#ssw0rd-12046
    reactive:
      url: postgresql://super-heroes-db.postgres.database.azure.com:5432/heroes_database?ssl=true&sslmode=require
```

We build the Heroes microservice with the following command:

```shell
rest-heroes$ ./mvnw clean package -Dmaven.test.skip=true -Dquarkus.container-image.build=true -Dquarkus.container-image.tag=$TAG
```

This creates the Docker image `quay.io/quarkus-super-heroes/rest-heroes` with the tag value `$TAG`

```shell
docker login quay.io
docker push quay.io/quarkus-super-heroes/rest-heroes:$TAG
```

The following command will deploy the Heroes image to Azure Container Apps and set the URL of the deployed application to the `HEROES_URL` variable:

```shell
HEROES_URL=$(az containerapp create \
  --resource-group $RESOURCE_GROUP \
  --image quay.io/quarkus-super-heroes/rest-heroes:$TAG \
  --name $HEROES_APP \
  --environment $CONTAINERAPPS_ENVIRONMENT \
  --ingress external \
  --target-port 8083 \
  --env-vars QUARKUS_PROFILE=azure \
  --query properties.configuration.ingress.fqdn \
  --output tsv)
  
echo $HEROES_URL  
```

TODO: --ingress external is for microservices that are accessed from the outside world, but this is not the case, it's only accesses by Fight

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

### Villain Microservice

The Villain microservice also needs to access the managed Postgres database, so we need to set the right properties in the `application-azure.properties` file:

```shell
quarkus.datasource.username=superheroesadmin
quarkus.datasource.password=super-heroes-p#ssw0rd-12046
quarkus.datasource.jdbc.url=jdbc:postgresql://super-heroes-db.postgres.database.azure.com:5432/villains_database?ssl=true&sslmode=require
```

We build the Villain microservice with the following command:

```shell
rest-villains$ ./mvnw clean package -Dmaven.test.skip=true -Dquarkus.container-image.build=true -Dquarkus.container-image.tag=$TAG
```

This creates the Docker image `quay.io/quarkus-super-heroes/rest-villains` with the tag value `$TAG`

```shell
docker login quay.io
docker push quay.io/quarkus-super-heroes/rest-villains:$TAG
```

The following command will deploy the Heroes image to Azure Container Apps and set the URL of the deployed application to the `VILLAINS_URL` variable:

```shell
VILLAINS_URL=$(az containerapp create \
  --resource-group $RESOURCE_GROUP \
  --image quay.io/quarkus-super-heroes/rest-villains:$TAG \
  --name $VILLAINS_APP \
  --environment $CONTAINERAPPS_ENVIRONMENT \
  --ingress external \
  --target-port 8084 \
  --env-vars QUARKUS_PROFILE=azure \
  --query properties.configuration.ingress.fqdn \
  --output tsv)
  
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
The fight messages are defined by an Avro schema stored in the Azure Schema Registry.
To configure Kafka, first get the connection string of the event hub namespace.

```shell
az eventhubs namespace authorization-rule keys list \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $KAFKA_NAMESPACE \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString
```

Add this connection string to the Statistics (and later to the Fight) microservice `application-azure.properties` file as well as the Apicurio URL (`APICURIO_URL`):

```shell
kafka.bootstrap.servers=fights-kafka.servicebus.windows.net:9093
kafka.security.protocol=SASL_SSL
kafka.sasl.mechanism=PLAIN
kafka.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
	username="$ConnectionString" \
	password="Endpoint=sb://fights-kafka.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=00LgwvAcx1hufDy5Kp3AeHraBvI9JSkXiKA8TJ2ov+0=";

mp.messaging.connector.smallrye-kafka.apicurio.registry.url=https://apicurio-app.blueisland-46fb2d13.eastus2.azurecontainerapps.io
```

We build the Statistics microservice with the following command:

```shell
event-statistics$ ./mvnw clean package -Dmaven.test.skip=true -Dquarkus.container-image.build=true -Dquarkus.container-image.tag=$TAG
```

This creates the Docker image `quay.io/quarkus-super-heroes/event-statistics` with the tag value `$TAG`

```shell
docker login quay.io
docker push quay.io/quarkus-super-heroes/event-statistics:$TAG
```

The following command will deploy the Statistics image to Azure Container Apps and set the URL of the deployed application to the `STATISTICS_URL` variable:

```shell
STATISTICS_URL=$(az containerapp create \
  --resource-group $RESOURCE_GROUP \
  --image quay.io/quarkus-super-heroes/event-statistics:$TAG \
  --name $STATISTICS_APP \
  --environment $CONTAINERAPPS_ENVIRONMENT \
  --ingress external \
  --target-port 8085 \
  --env-vars QUARKUS_PROFILE=azure \
  --query properties.configuration.ingress.fqdn \
  --output tsv)
  
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

### Fight Microservice

The Fight microservice invokes the Heroes and Villains microserivces, sends fight messages to a Kafka topics and stores the fights into a MongoDB database.
To configure Kafka, we need the same connection string as the one used by the Statistics microservice as well as the Apicurion URL (variable `APICURIO_URL`).
In the `application-azure.properties` file add:

```shell
kafka.bootstrap.servers=fights-kafka.servicebus.windows.net:9093
kafka.security.protocol=SASL_SSL
kafka.sasl.mechanism=PLAIN
kafka.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
	username="$ConnectionString" \
	password="Endpoint=sb://fights-kafka.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=00LgwvAcx1hufDy5Kp3AeHraBvI9JSkXiKA8TJ2ov+0=";

mp.messaging.connector.smallrye-kafka.apicurio.registry.url=https://apicurio-app.blueisland-46fb2d13.eastus2.azurecontainerapps.io
```

For MongoDB, get the connection string by executing the following command:

````shell
az cosmosdb keys list \
  --resource-group $RESOURCE_GROUP \
  --name $MONGO_DB \
  --type connection-strings \
  --query "connectionStrings[?description=='Primary MongoDB Connection String'].connectionString" \
  --output tsv
````

Add the connection string to the `application-azure.properties` file:

```shell
quarkus.mongodb.connection-string=mongodb://fights-db:tF8pMgaFm6mgyRSKKpPp3cSEkZAEoFmxpHsqrIJ94vMLWMBTqEvHWw1CyuNPebOfinipMK3qPfKVovQAAGQ5DA==@fights-db.mongo.cosmos.azure.com:10255/?ssl=true&replicaSet=globaldb&retrywrites=false&maxIdleTimeMS=120000&appName=@fights-db@
```

As for the microservice invocations, you need to set the URLs of both Heroes and Villains microservices.
For that, get the values of the `HEROES_URL` and `VILLAINS_URL` variables and add them to the `application-azure.properties` file:

```shell
quarkus.rest-client.hero-client.url=https://rest-heroes-app.thankfulpond-0365fcc1.eastus2.azurecontainerapps.io
fight.villain.client-base-url=https://rest-villains-app.thankfulpond-0365fcc1.eastus2.azurecontainerapps.io
```

Build the Fight microservice with the following command:

```shell
rest-fights$ ./mvnw clean package -Dmaven.test.skip=true -Dquarkus.container-image.build=true -Dquarkus.container-image.tag=$TAG
```

This creates the Docker image `quay.io/quarkus-super-heroes/rest-fights` with the tag value `$TAG`

```shell
docker login quay.io
docker push quay.io/quarkus-super-heroes/rest-fights:$TAG
```

The following command will deploy the Fight image to Azure Container Apps and set the URL of the deployed application to the `FIGHTS_URL` variable:

```shell
FIGHTS_URL=$(az containerapp create \
  --resource-group $RESOURCE_GROUP \
  --image quay.io/quarkus-super-heroes/rest-fights:$TAG \
  --name $FIGHTS_APP \
  --environment $CONTAINERAPPS_ENVIRONMENT \
  --ingress external \
  --target-port 8082 \
  --env-vars QUARKUS_PROFILE=azure \
  --query properties.configuration.ingress.fqdn \
  --output tsv)
  
echo $FIGHTS_URL 
```

You can now invoke the Fight microservice APIs with:

```shell
curl https://$FIGHTS_URL/api/fights/hello
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

### Super Hero UIe

If you are building the UI locally with Node 17 you have to set the `NODE_OPTIONS` variable:

```shell
node version
export NODE_OPTIONS=--openssl-legacy-provider
```

Then, to execute the app locally, set `API_BASE_URL` with the same value of the Fight microservice URL (so it accesses the remote Fight microservice): 

```shell
export API_BASE_URL=https://$FIGHT_URL 
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


## Miscellaneous

### Restarting a Microservice

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

### Redeploying a new version of a microservice

If you need to push a new version of a Docker image, make sure it has a different tag.
Then, update the container with this new tagged image:

```shell
az containerapp update \
  --resource-group $RESOURCE_GROUP \
  --image agoncal/rest-fights:azure2 \
  --name $FIGHTS_APP
```
