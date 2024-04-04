# Quarkus Superheroes Sample :: Scripts

## Shell Scripts

This directory contains a few shell scripts that:

* [`deploy-to-azure-containerapps.sh`](deploy-to-azure-containerapps.sh): Deploys the entire infrastructure and application to [Azure Container Apps](../docs/deploying-to-azure-containerapps.md)
* [`create-azure-openai-resources.sh`](create-azure-openai-resources.sh): Creates Azure OpenAI resources for the [`rest-narration`](../rest-narration) application
* [`delete-azure-openai-resources.sh`](delete-azure-openai-resources.sh): Deletes the Azure OpenAI resources created by [`create-azure-openai-resources.sh`](create-azure-openai-resources.sh).
* [`generate-docker-compose-resources.sh`](generate-docker-compose-resources.sh): Generates the [Docker Compose files](../deploy/docker-compose)
* [`generate-k8s-resources.sh`](generate-k8s-resources.sh): Generates the [Kubernetes manifest files](../deploy/k8s)

