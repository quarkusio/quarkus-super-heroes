#!/bin/bash -e

# Display help
help() {
  echo "This script deletes the Azure OpenAI resources"
  echo "It assumes you have the the following utilities installed on your machine:"
  echo "  1) The Azure CLI (https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)"
  echo
  echo "Additionally, it assumes you are already logged into your Azure environment via the Azure CLI."
  echo
  echo "Syntax: delete-azure-openai-resources.sh [options]"
  echo "options:"
  echo "  -c <cognitive_service_name>            Name of the Azure Cognitive service"
  echo "                                             Default: 'cs-super-heroes'"
  echo "  -g <resource_group_name>               Name of the Azure resource group to use to deploy resources"
  echo "                                             Default: 'rg-super-heroes'"
  echo "  -h                                     Prints this help message"
  echo "  -l <location>                          The location (region) to deploy resources into"
  echo "                                             Default: 'eastus'"
  echo "  -u <unique_identifier>                 A unique identifier to append to some resources. Some Azure services require unique names within a region (across users)."
  echo "                                             Default is to use the output of the 'whoami' command."
}

exit_abnormal() {
  echo
  help
  exit 1
}

# Define defaults
COGNITIVE_SERVICE="cs-super-heroes"
RESOURCE_GROUP="rg-super-heroes"
LOCATION="eastus"
UNIQUE_IDENTIFIER=$(whoami)

# Process the input options
while getopts "c:g:hl:" option; do
  case $option in
    c) COGNITIVE_SERVICE=$OPTARG
       ;;

    g) RESOURCE_GROUP=$OPTARG
       ;;

    h) help
       exit
       ;;

    l) LOCATION=$OPTARG
       ;;

    u) UNIQUE_IDENTIFIER=$OPTARG
       ;;

    *) exit_abnormal
       ;;
  esac
done

# Now run the script
echo "Deleting Azure OpenAI Resources with the following configuration:"
echo "  Cognitive Service Name: $COGNITIVE_SERVICE"
echo "  Resource Group: $RESOURCE_GROUP"
echo "  Location: $LOCATION"
echo "  Unique Identifier: $UNIQUE_IDENTIFIER"
echo
echo "Please be patient. This may take several minutes."

COGNITIVE_SERVICE="${COGNITIVE_SERVICE}-${UNIQUE_IDENTIFIER}"

# Delete cognitive service
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Deleting the $COGNITIVE_SERVICE cognitive service"
echo "-----------------------------------------"
az cognitiveservices account delete \
  --name "$COGNITIVE_SERVICE" \
  --resource-group "$RESOURCE_GROUP"

az cognitiveservices account purge \
  --name "$COGNITIVE_SERVICE" \
  --resource-group "$RESOURCE_GROUP" \
  --location "$LOCATION"

# Delete resource group
echo "-----------------------------------------"
echo "[$(date +"%m/%d/%Y %T")]: Deleting the $RESOURCE_GROUP resource group in the $LOCATION location"
echo "-----------------------------------------"
az group delete \
  --name "$RESOURCE_GROUP" \
  --yes

echo
echo "Deletion of resources completed in $SECONDS seconds."
echo "All resources have been deleted."
