#!/bin/bash -e

# Display help
help() {
  echo "This script creates the Azure OpenAI resources"
  echo "It assumes you have the the following utilities installed on your machine:"
  echo "  1) The Azure CLI (https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)"
  echo "  2) jq (https://stedolan.github.io/jq)"
  echo
  echo "Additionally, it assumes you are already logged into your Azure environment via the Azure CLI."
  echo
  echo "Syntax: create-azure-openai-resources.sh [options]"
  echo "options:"
  echo "  -c <cognitive_service_name>            Name of the Azure Cognitive service"
  echo "                                             Default: 'cs-super-heroes'"
  echo "  -d <cognitive_service_deployment_name> Name of the Azure Cognitive service deployment"
  echo "                                             Default: 'csdeploy-super-heroes'"
  echo "  -g <resource_group_name>               Name of the Azure resource group to use to deploy resources"
  echo "                                             Default: 'rg-super-heroes'"
  echo "  -h                                     Prints this help message"
  echo "  -l <location>                          The location (region) to deploy resources into"
  echo "                                             Default: 'eastus'"
  echo "  -m <model>                             The model to use"
  echo "                                             Default: 'gpt-35-turbo'"
  echo "  -t <tag>                               The tag to use when creating resources"
  echo "                                             Default: 'super-heroes'"
  echo "  -u <unique_identifier>                 A unique identifier to append to some resources. Some Azure services require unique names within a region (across users)."
  echo "                                             Default is to use the output of the 'whoami' command."
  echo "  -v <model_version>                     The model version to use"
  echo "                                             Default: '0301'"
}

exit_abnormal() {
  echo
  help
  exit 1
}

# Define defaults
COGNITIVE_SERVICE="cs-super-heroes"
COGNITIVE_DEPLOYMENT="csdeploy-super-heroes"
RESOURCE_GROUP="rg-super-heroes"
LOCATION="eastus"
TAG="super-heroes"
MODEL="gpt-35-turbo"
MODEL_VERSION="0301"
UNIQUE_IDENTIFIER=$(whoami)

# Process the input options
while getopts "c:d:g:hm:l:t:u:v:" option; do
  case $option in
    c) COGNITIVE_SERVICE=$OPTARG
       ;;

    d) COGNITIVE_DEPLOYMENT=$OPTARG
       ;;

    g) RESOURCE_GROUP=$OPTARG
       ;;

    h) help
       exit
       ;;

    l) LOCATION=$OPTARG
       ;;

    m) MODEL=$OPTARG
       ;;

    v) MODEL_VERSION=$OPTARG
       ;;

    t) TAG=$OPTARG
       ;;

    u) UNIQUE_IDENTIFIER=$OPTARG
       ;;

    *) exit_abnormal
       ;;
  esac
done

# Now run the script
echo "Creating Azure OpenAI Resources with the following configuration:"
echo "  Cognitive Service Name: $COGNITIVE_SERVICE"
echo "  Cognitive Service Deployment: $COGNITIVE_DEPLOYMENT"
echo "  Resource Group: $RESOURCE_GROUP"
echo "  Location: $LOCATION"
echo "  Model: $MODEL"
echo "  Model Version: $MODEL_VERSION"
echo "  Tag: $TAG"
echo "  Unique Identifier: $UNIQUE_IDENTIFIER"
echo
echo "Please be patient. This may take several minutes."

COGNITIVE_SERVICE="${COGNITIVE_SERVICE}-${UNIQUE_IDENTIFIER}"
COGNITIVE_DEPLOYMENT="${COGNITIVE_DEPLOYMENT}-${UNIQUE_IDENTIFIER}"

# Create resource group
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $RESOURCE_GROUP resource group in the $LOCATION location"
echo "-----------------------------------------"
az group create \
  --name "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --tags system="$TAG"
echo

# Create cognitive service
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Creating the $COGNITIVE_SERVICE cognitive service"
echo "-----------------------------------------"
az cognitiveservices account create \
  --name "$COGNITIVE_SERVICE" \
  --resource-group "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --custom-domain "$COGNITIVE_SERVICE" \
  --tags system="$TAG" \
  --kind OpenAI \
  --sku S0 \
  --yes

# Deploy the model
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Deploying the $COGNITIVE_DEPLOYMENT model"
echo "-----------------------------------------"
az cognitiveservices account deployment create \
  --name "$COGNITIVE_SERVICE" \
  --resource-group "$RESOURCE_GROUP" \
  --deployment-name "$COGNITIVE_DEPLOYMENT" \
  --model-name "$MODEL" \
  --model-version "$MODEL_VERSION" \
  --model-format OpenAI \
  --sku-name Standard \
  --sku-capacity 1

# Get keys
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Getting keys"
echo "-----------------------------------------"
AZURE_OPENAI_KEY=$(
  az cognitiveservices account keys list \
    --name "$COGNITIVE_SERVICE" \
    --resource-group "$RESOURCE_GROUP" \
    | jq -r .key1
)

AZURE_OPENAI_ENDPOINT=$(
  az cognitiveservices account show \
    --name "$COGNITIVE_SERVICE" \
    --resource-group "$RESOURCE_GROUP" \
    | jq -r .properties.endpoint
)

echo
echo "Deployment took $SECONDS seconds to complete."
echo
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: All services have been deployed"
echo "-----------------------------------------"
echo "Here are some key values you may need to use in your application's configuration:"
echo
echo "API key (quarkus.langchain4j.azure-openai.api-key: $AZURE_OPENAI_KEY"
echo "Endpoint: $AZURE_OPENAI_ENDPOINT"
echo "Resource Name (quarkus.langchain4j.azure-openai.resource-name): $COGNITIVE_SERVICE"
echo "Deployment Name/Id (quarkus.langchain4j.azure-openai.deployment-id): $COGNITIVE_DEPLOYMENT"
