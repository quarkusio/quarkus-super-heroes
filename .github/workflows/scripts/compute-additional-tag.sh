#!/bin/bash -e

####################
# Create the ADDITIONAL_TAG that should be used

if [[ $# -lt 4 ]]; then
  echo "$0: Should have at least 4 arguments: [kind java_version latest_image_tag branch]"
  exit 1
fi

kind=$1
java_version=$2
latest_image_tag=$3
branch=$4

additional_tag="${kind}"

if [[ "$kind" == "native-" ]]; then
  additional_tag="${additional_tag}${latest_image_tag}"
else
  additional_tag="${additional_tag}java${java_version}-${latest_image_tag}"
fi

if [[ "$branch" != "main" ]]; then
  additional_tag="${additional_tag}-${branch}"
fi

if [[ $# -eq 5 && $5 != "" ]]; then
  # They specified an openai type
  openai_type=$5
  additional_tag="${additional_tag}-${openai_type}"
fi

echo "$additional_tag"