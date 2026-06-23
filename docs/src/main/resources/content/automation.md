---
title: CI/CD Automation
description: GitHub Action workflows and application resource generation for the Quarkus Super Heroes.
layout: page
content-toc: true
---

The Quarkus Super Heroes project uses [GitHub Actions](https://docs.github.com/en/actions) for continuous integration, container image publishing, deployment resource generation, and documentation site deployment. This page describes each workflow and how application resources are generated.

## GitHub Action Automation

All GitHub Action workflow files are located in the [`.github/workflows`](https://github.com/quarkusio/quarkus-super-heroes/tree/main/.github/workflows) directory.

### Basic Building and Testing

**Workflow:** [`simple-build-test.yml`](https://github.com/quarkusio/quarkus-super-heroes/blob/main/.github/workflows/simple-build-test.yml)

This workflow runs on every push to `main` and on all pull requests. It can also be triggered manually. It builds and tests all 7 services using Java 25:

- **JVM build and test** -- Runs `./mvnw clean verify` for each service.
- **JVM container build and test** -- Builds container images and runs tests with containers for applicable services.
- **Native build and test** -- Runs `./mvnw clean verify -Pnative` for each service using Mandrel.
- **Native container build and test** -- Builds native container images and runs tests with containers for applicable services.

### Build and Push Container Images

**Workflow:** [`build-push-container-images.yml`](https://github.com/quarkusio/quarkus-super-heroes/blob/main/.github/workflows/build-push-container-images.yml)

This workflow runs on a daily schedule and can be triggered manually. It builds and pushes container images for all services to [`quay.io/quarkus-super-heroes`](https://quay.io/organization/quarkus-super-heroes). The workflow consists of several jobs:

- **Build JVM container images** -- Builds JVM-based container images for each service on both `amd64` and `arm64` architectures using Java 25.
- **Build native container images** -- Builds GraalVM native container images for each service on both `amd64` and `arm64` architectures.
- **Push application container images** -- Pushes all built images (JVM and native, both architectures) to the `quay.io` container registry.
- **Create application multi-arch manifests** -- Creates and pushes multi-architecture Docker manifests so that a single image tag works on both `amd64` and `arm64`.

After images are pushed, the workflow triggers the [Create Deploy Resources](#create-deploy-resources) workflow to regenerate deployment descriptors.

### Deploy Documentation Site

**Workflow:** [`azure-static-web-apps-orange-rock-0d20a680f.yml`](https://github.com/quarkusio/quarkus-super-heroes/blob/main/.github/workflows/azure-static-web-apps-orange-rock-0d20a680f.yml)

This workflow deploys the documentation site to Azure Static Web Apps. It can be triggered manually.

### Create Deploy Resources

**Workflow:** [`create-deploy-resources.yml`](https://github.com/quarkusio/quarkus-super-heroes/blob/main/.github/workflows/create-deploy-resources.yml)

This workflow generates all Kubernetes, OpenShift, Minikube, Knative, Helm, and Docker Compose deployment descriptors. It is called by the [Build and Push Container Images](#build-and-push-container-images) workflow after images are published, and can also be triggered manually. It runs two scripts:

- [`scripts/generate-k8s-helm-resources.sh`](https://github.com/quarkusio/quarkus-super-heroes/blob/main/scripts/generate-k8s-helm-resources.sh) -- Generates Kubernetes/OpenShift/Minikube/Knative descriptors and Helm charts.
- [`scripts/generate-docker-compose-resources.sh`](https://github.com/quarkusio/quarkus-super-heroes/blob/main/scripts/generate-docker-compose-resources.sh) -- Generates Docker Compose files.

The generated resources are committed via an auto-merged pull request.

### Cleanup Artifacts

**Workflow:** [`delete-artifacts.yml`](https://github.com/quarkusio/quarkus-super-heroes/blob/main/.github/workflows/delete-artifacts.yml)

This workflow is triggered via repository dispatch after the container image build completes. It cleans up build artifacts (saved container images) from the triggering workflow run.

## Application Resource Generation

### Kubernetes (and Variants) Resource Generation

Kubernetes deployment descriptors are generated using the [Quarkus Kubernetes Extension](https://quarkus.io/guides/deploying-to-kubernetes), [Quarkus OpenShift Extension](https://quarkus.io/guides/deploying-to-openshift), and [Quarkus Minikube Extension](https://quarkus.io/guides/deploying-to-kubernetes#deploying-to-minikube), along with the [Quarkus Helm Extension](https://docs.quarkiverse.io/quarkus-helm/dev/index.html). The [`generate-k8s-helm-resources.sh`](https://github.com/quarkusio/quarkus-super-heroes/blob/main/scripts/generate-k8s-helm-resources.sh) script orchestrates the process:

1. For each service, it builds the application with all deployment targets enabled (Kubernetes, OpenShift, Minikube, Knative), producing descriptors in the service's `target/kubernetes` directory and Helm charts in `target/helm`.
2. Per-service descriptors are copied to each service's `deploy/k8s` directory, and combined full-system descriptors are assembled in the top-level `deploy/k8s` directory.
3. Per-service Helm charts are copied to each service's `deploy/helm` directory, and umbrella charts for the full system are created in the top-level `deploy/helm` directory.
4. Monitoring descriptors and Helm charts are generated for Kubernetes, Minikube, and OpenShift.
5. Both JVM (Java 25) and native variants are generated.

The generated files in `deploy/k8s` and `deploy/helm` should not be edited manually -- they are overwritten on each CI run.

### Docker Compose Resource Generation

Docker Compose files are generated by the [`generate-docker-compose-resources.sh`](https://github.com/quarkusio/quarkus-super-heroes/blob/main/scripts/generate-docker-compose-resources.sh) script:

1. For each service, infrastructure definitions (databases, Kafka, etc.) and application service definitions are assembled from source templates in each service's `src/main/docker-compose` directory.
2. Per-service compose files are written to each service's `deploy/docker-compose` directory.
3. Full-system compose files combining all services and infrastructure are assembled in the top-level `deploy/docker-compose` directory.
4. Monitoring compose files and Grafana dashboards are copied to the top-level `deploy/docker-compose` directory.
5. Both JVM (Java 25) and native variants are generated.

The generated files in `deploy/docker-compose` should not be edited manually -- they are overwritten on each CI run.
