#!/usr/bin/env bash

set -e

oc delete secret rest-narration-config-creds --ignore-not-found=true
oc create secret generic rest-narration-config-creds --from-literal=quarkus.langchain4j.openai.api-key="${OPENAI_API_KEY}"
oc set env deployment/rest-narration --from=secret/rest-narration-config-creds
oc set env deployment/rest-narration QUARKUS_PROFILE=openai
oc set env deployment/rest-narration QUARKUS_LANGCHAIN4J_OPENAI_TIMEOUT=2m
oc rollout restart deployment/rest-narration
