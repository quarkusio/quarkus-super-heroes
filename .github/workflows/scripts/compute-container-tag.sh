#!/bin/bash -e

####################
# Create the CONTAINER_TAG that should be used

if [[ $# -lt 4 ]]; then
  echo "$0: Should have at least 4 arguments: [app_version quarkus_version kind java_version]"
  exit 1
fi

app_version=$1
quarkus_version=$2
kind=$3
java_version=$4

container_tag="${app_version}-quarkus-${quarkus_version}"

if [[ "$kind" == "native-" ]]; then
  container_tag="${container_tag}-native"
else
  container_tag="${container_tag}-${kind}java${java_version}"
fi

if [[ $# -eq 5 && $5 != "" ]]; then
  # They specified an openai type
  openai_type=$5
  container_tag="${container_tag}-${openai_type}"
fi

echo "$container_tag"