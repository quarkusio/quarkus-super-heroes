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
  echo "  -t <tag>                               The tag to use when creating resources"
  echo "                                             Default: 'super-heroes'"
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

# Process the input options
while getopts "c:d:g:hl:t:" option; do
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

    t) TAG=$OPTARG
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
echo "  Tag: $TAG"
echo
echo "Please be patient. This may take several minutes."

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
  --model-name gpt-35-turbo \
  --model-version 0301 \
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
echo "You can use the following properties to configure the 'rest-narration' application to consume the deployed services:"
echo "  narration.azure-open-ai.enabled=true"
echo "  narration.azure-open-ai.key=$AZURE_OPENAI_KEY"
echo "  narration.azure-open-ai.endpoint=$AZURE_OPENAI_ENDPOINT"
echo "  narration.azure-open-ai.deployment-name=$COGNITIVE_DEPLOYMENT"
echo
echo "Or they can be injected via environment variables:"
echo "  NARRATION_AZURE_OPEN_AI_ENABLED=true"
echo "  NARRATION_AZURE_OPEN_AI_KEY=$AZURE_OPENAI_KEY"
echo "  NARRATION_AZURE_OPEN_AI_ENDPOINT=$AZURE_OPENAI_ENDPOINT"
echo "  NARRATION_AZURE_OPEN_AI_DEPLOYMENT_NAME=$COGNITIVE_DEPLOYMENT"
