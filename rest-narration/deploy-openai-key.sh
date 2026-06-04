#!/usr/bin/env bash

set -e

oc create secret generic rest-narration-config-creds --from-literal=quarkus.langchain4j.openai.api-key="${OPENAI_API_KEY}"
oc set env deployment/rest-narration --from=secret/rest-narration-config-creds
oc rollout restart deployment/rest-narration
