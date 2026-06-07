#!/usr/bin/env bash

set -e

./mvnw clean package -DskipTests \
  -Dquarkus.profile=remotedev \
  -Dquarkus.live-reload.password=supersecretquarkuspassword \
  -Dquarkus.container-image.group=$(oc project -q) \
  -Dquarkus.openshift.version=$(oc get deployment rest-fights -o json | jq -r '.metadata.labels["app.kubernetes.io/version"]') \
  -Dquarkus.container-image.tag=$(oc get deployment rest-fights -o json | jq -r '.metadata.labels["app.kubernetes.io/version"]')
