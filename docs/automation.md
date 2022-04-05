# Table of Contents
- [Introduction](#introduction)
- [GitHub action automation](#github-action-automation)
    - [Basic building and testing](#basic-building-and-testing-workflow)
    - [Build and push container images](#build-and-push-container-images-workflow)
        - [Build and test JVM container image job](#build-and-test-jvm-container-images-job)
        - [Build and test native container image job](#build-and-test-native-container-images-job)
        - [Build UI image job](#build-ui-image-job)
        - [Push application container images job](#push-application-container-images-job)
        - [Push UI image job](#push-ui-image-job)
    - [Create deploy resources](#create-deploy-resources-workflow)
- [Application Resource Generation](#application-resource-generation)
    - [Kubernetes (and variants) resource generation](#kubernetes-and-variants-resource-generation)
    - [Docker compose resource generation](#docker-compose-resource-generation)

# Introduction
This document describes the overall automation strategy for the Quarkus superheroes services and how all the automation behind it works. There is a lot of automation and resource generation that happens "behind the scenes" when changes are pushed to the GitHub repo.

# GitHub action automation
There are 3 GitHub action workflows that run when code is pushed: [Basic building and testing](#basic-building-and-testing-workflow), [Build and push container images](#build-and-push-container-images-workflow), and [Create deploy resources](#create-deploy-resources-workflow).

## Basic building and testing workflow
The [Basic building and testing](../.github/workflows/simple-build-test.yml) workflow is a "sanity check". It is required to pass before pull requests can be merged.

It runs whenever code is pushed to the `main` branch as well as upon any pull requests.
   > The workflow can also be [triggered manually](https://docs.github.com/en/actions/managing-workflow-runs/manually-running-a-workflow).

It runs `./mvnw clean verify` on the [`event-statistics`](../event-statistics), [`rest-fights`](../rest-fights), [`rest-heroes`](../rest-heroes), and [`rest-villains`](../rest-villains) applications on both Java 11 and 17.

## Build and push container images workflow
The [Build and push container images](../.github/workflows/build-push-container-images.yml) workflow does pretty much what it sounds like: builds and pushes container images.

It only runs on pushes to the `main` branch after successful completion of the above [_Basic building and testing_](#basic-building-and-testing-workflow) workflow.
   > The workflow can also be [triggered manually](https://docs.github.com/en/actions/managing-workflow-runs/manually-running-a-workflow).

It consists of 5 jobs: 
- [_Build and test JVM container images_](#build-and-test-jvm-container-images-job)
- [_Build and test native container images_](#build-and-test-native-container-images-job)
- [_Build UI image_](#build-ui-image-job)
- [_Push application container images_](#push-application-container-images-job)
- [_Push UI image_](#push-ui-image-job)

If any step in any of the jobs fail then the entire workflow fails.

This image is a visual of what the workflow consists of:

![build-push-images-workflow](../images/build-push-container-images-workflow.png)

### Build and test JVM container images job
1. [Builds JVM container images](https://quarkus.io/guides/container-image#building) for the [`event-statistics`](../event-statistics), [`rest-fights`](../rest-fights), [`rest-heroes`](../rest-heroes), and [`rest-villains`](../rest-villains) applications on both Java 11 and 17 using the [Quarkus Docker container image extension](https://quarkus.io/guides/container-image#docker).
    - Each container image created has 2 tags: `{{app-version}}-quarkus-{{quarkus-version}}-java{{java-version}}` and `java{{java-version}}-latest`.
        - Replace `{{app-version}}` with the application version (i.e. `1.0`).
        - Replace `{{quarkus-version}}` with Quarkus version the application uses (i.e. `2.6.3.Final`).
        - Replace `{{java-version}}` with the Java version the application was built with (i.e. `11` or `17`).
              
2. Once each JVM container image is built (4 applications x 2 JVM versions = 8 total JVM container images), each JVM container is launched and the [integration tests are run against the image](https://quarkus.io/guides/getting-started-testing#quarkus-integration-test).
      
### Build and test native container images job
Runs in parallel with the [_Build and test JVM container images_](#build-and-test-jvm-container-images-job) and [_Build UI image_](#build-ui-image-job) jobs.

1. [Builds native executables](https://quarkus.io/guides/building-native-image) for the [`event-statistics`](../event-statistics), [`rest-fights`](../rest-fights), [`rest-heroes`](../rest-heroes), and [`rest-villains`](../rest-villains) applications on both Java 11 and 17 using [Mandrel](https://github.com/graalvm/mandrel).
       
2. Once each native executable is created (4 applications x 2 JVM versions = 8 total native executables), each native executable is launched and the [integration tests are run against the executable](https://quarkus.io/guides/getting-started-testing#quarkus-integration-test).
       
3. Once tested, [a container image is created from each native executable](https://quarkus.io/guides/building-native-image#using-the-container-image-extensions) using the [Quarkus Docker container image extension](https://quarkus.io/guides/container-image#docker).
   > **NOTE:** The native executable is not re-created. It is [re-used from the previous step](https://quarkus.io/guides/building-native-image#quarkus-native-pkg-native-config_quarkus.native.reuse-existing).
    - Each container image created has 2 tags: `{{app-version}}-quarkus-{{quarkus-version}}-native-java{{java-version}}` and `native-java{{java-version}}-latest`.
        - Replace `{{app-version}}` with the application version (i.e. `1.0`).
        - Replace `{{quarkus-version}}` with Quarkus version the application uses (i.e. `2.6.3.Final`).
        - Replace `{{java-version}}` with the Java version the application was built with (i.e. `11` or `17`).
              
4. Once each native executable container image is built (8 total native executable container images), each container is launched and the [integration tests are run against the image](https://quarkus.io/guides/getting-started-testing#quarkus-integration-test).

### Build UI image job
Runs in parallel with the [_Build and test JVM container images_](#build-and-test-jvm-container-images-job) and [_Build and test native container images_](#build-and-test-native-container-images-job) jobs.

Builds the [`ui-super-heroes`](../ui-super-heroes) container image with the following 2 tags: `{{app-version}}` and `latest`.
- Replace `{{app-version}}` with the application version (i.e. `1.0`).
      
### Push application container images job
Runs after successful completion of the [_Build and test JVM container image_](#build-and-test-jvm-container-images-job), [_Build and test native container image_](#build-and-test-native-container-images-job), and [_Build UI image_](#build-ui-image-job) jobs and in parallel with the [_Push UI image_](#push-ui-image-job) job.

All of the container images created in the [_Build and test JVM container image_](#build-and-test-jvm-container-images-job) and [_Build and test native container image_](#build-and-test-native-container-images-job) jobs (16 total container images/32 tags) are pushed to https://quay.io/quarkus-super-heroes.

### Push UI image job
Runs after successful completion of the [_Build and test JVM container image_](#build-and-test-jvm-container-images-job), [_Build and test native container image_](#build-and-test-native-container-images-job), and [_Build UI image_](#build-ui-image-job) jobs and in parallel with the [_Push application container images_](#push-application-container-images-job) job.

Pushes the [`ui-super-heroes`](../ui-super-heroes) container image with the following 2 tags: `{{app-version}}` and `latest`.
- Replace `{{app-version}}` with the application version (i.e. `1.0`).

## Create deploy resources workflow
The [Create deploy resources](../.github/workflows/create-deploy-resources.yml) workflow is responsible for [generating all of the application resources](#application-resource-generation), described in a later section of this document.

It only runs on pushes to the `main` branch after successful completion of the [_Build and push container images_](#build-and-push-container-images-workflow) workflow.
   > The workflow can also be [triggered manually](https://docs.github.com/en/actions/managing-workflow-runs/manually-running-a-workflow).

All generated resources are subsequently pushed back into the repo by the action in a single commit.

# Application resource generation
The resources and descriptors in the [root `deploy` directory](../deploy) as well as in each individual project's `deploy` directory ([`event-statistics`](../event-statistics/deploy), [`rest-fights`](../rest-fights/deploy), [`rest-heroes`](../rest-heroes/deploy), [`rest-villains`](../rest-villains/deploy), and [`ui-super-heroes`](../ui-super-heroes/deploy)) are used for deploying the entire system or subsets of it into various environments (i.e. Docker compose, OpenShift, Minikube, Kubernetes, etc).

Resources in these directories are generated by the [_Create deploy resources_ workflow](#create-deploy-resources-workflow) mentioned in the previous section. Any manual changes made to anything in any of those directories will be overwritten by the workflow upon its next execution.

## Kubernetes (and variants) resource generation
Kubernetes resources are generated into a `deploy/k8s` directory, either in the [project root directory](../deploy/k8s) or in each individual project's directory.

### Quarkus projects
Each Quarkus project ([`event-statistics`](../event-statistics), [`rest-fights`](../rest-fights), [`rest-heroes`](../rest-heroes), [`rest-villains`](../rest-villains)) uses the [Quarkus Kubernetes extension](https://quarkus.io/guides/deploying-to-kubernetes) to generate Kubernetes and KNative manifests, the [Quarkus Minikube extension](https://quarkus.io/guides/deploying-to-kubernetes#deploying-to-minikube) to generate Minikube manifests, and the [Quarkus OpenShift extension](https://quarkus.io/guides/deploying-to-openshift) to generate OpenShift manifests.

These extensions generate the manifests needed for the application itself but not for any other services. [These extensions can also incorporate additional resources](https://quarkus.io/guides/deploying-to-kubernetes#using-existing-resources) by placing additional resources in each project's `src/main/kubernetes` directory.

The [`generate-k8s-resources.sh` script](../scripts/generate-k8s-resources.sh) loops through all versions of each application (Java versions 11 & 17, both JVM and native - 16 total versions) and merges the contents of files these extensions generate and places them into each project's `deploy/k8s` directory as well as the respective files in the [root `deploy/k8s` directory](../deploy/k8s).

The [`generate-k8s-resources.sh` script](../scripts/generate-k8s-resources.sh) additionally creates the Prometheus descriptors within the [root `deploy/k8s` directory](../deploy/k8s) for each Kubernetes variant platform.

In the [`rest-fights` project](../rest-fights), the [`generate-k8s-resources.sh` script](../scripts/generate-k8s-resources.sh) additionally copies in generated resources from the [`rest-heroes`](../rest-heroes) and [`rest-villains`](../rest-villains) projects into the `all-downstream.yml` files in the [`deploy/k8s` directory of the `rest-fights` project](../rest-fights/deploy/k8s).

### UI Project
Since the [`ui-super-heroes` project](../ui-super-heroes) isn't a Quarkus project it therefore doesn't have a way to generate the Kubernetes manifests.

The [`generate-k8s-resources.sh` script](../scripts/generate-k8s-resources.sh) implements the same pattern for this project. The [`ui-super-heroes` project](../ui-super-heroes) has a [`src/main/kubernetes` directory](../ui-super-heroes/src/main/kubernetes) containing the Kubernetes manifests.

The [`generate-k8s-resources.sh` script](../scripts/generate-k8s-resources.sh) merges the contents of these files into the files in the [`ui-super-heroes` project's `deploy/k8s` directory](../ui-super-heroes/deploy/k8s) as well as all the files in the [root `deploy/k8s` directory](../deploy/k8s).

## Docker compose resource generation
Docker compose resource generation follows a similar pattern as the [Kubernetes resource generation](#kubernetes-and-variants-resource-generation).

Docker compose resources are generated into a `deploy/docker-compose` directory, either in the [project root directory](../deploy/docker-compose) or in each individual project's directory.

### Quarkus projects
Each Quarkus project ([`event-statistics`](../event-statistics/src/main/docker-compose), [`rest-fights`](../rest-fights/src/main/docker-compose), [`rest-heroes`](../rest-heroes/src/main/docker-compose), [`rest-villains`](../rest-villains/src/main/docker-compose)) contains a `src/main/docker-compose` directory.

Inside this directory are a set of yaml files with a particular naming convention: `infra.yml`, `java{{java-version}}.yml`, and `native-java{{java-version}}.yml`. Each of these files contains what we are calling _Docker compose snippets_. These snippets aren't a complete Docker compose file on their own. Instead, they contain service definitions that will ultimately end up inside the `services` block in a Docker compose file.

This table describes the different files that can be found inside a project's `src/main/docker-compose` directory.

| File name                         | Description                                                                                                                                                                                                                                                                                                                                                                                                                  |
|-----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `infra.yml`                       | Any infrastructure definitions that are needed by the application. Definitions in here a re-used for each version of the application (i.e. JVM 11, JVM 17, Native Java 11, Native Java 17).                                                                                                                                                                                                                                  |
| `java{{java-version}}.yml`        | Definition for the JVM version of application itself for a particular java version, denoted by `{{java-version}}`.                                                                                                                                                                                                                                                                                                           |
| `native-java{{java-version}}.yml` | Definition for the native image version of the application itself, built with a particular java version, denoted by `{{java-version}}`.                                                                                                                                                                                                                                                                                      |

The [`generate-docker-compose-resources.sh` script](../scripts/generate-docker-compose-resources.sh) loops through all versions of each application (Java versions 11 & 17, both JVM and native - 16 total versions) and merges contents of these files from each project's `src/main/docker-compose` directory into each project's `deploy/docker-compose` directory as well as the respective files in the [root `deploy/docker-compose` directory](../deploy/docker-compose).

The [`generate-docker-compose-resources.sh` script](../scripts/generate-docker-compose-resources.sh) additionally creates the [Prometheus compose file (`prometheus.yml`)](../deploy/docker-compose/prometheus.yml) within the [root `deploy/docker-compose` directory](../deploy/docker-compose).

### UI Project
Like the Quarkus projects, the [`ui-super-heroes` project](../ui-super-heroes) also has a [`src/main/docker-compose` directory](../ui-super-heroes/src/main/docker-compose). There is only a single [`app.yml` file](../ui-super-heroes/src/main/docker-compose/app.yml) containing the _Docker compose snippet_.

The [`generate-docker-compose-resources.sh` script](../scripts/generate-docker-compose-resources.sh) merges the contents of this file into the [`ui-super-heroes` project's `deploy/docker-compose/app.yml` file](../ui-super-heroes/deploy/docker-compose/app.yml) as well as all the files in the [root `deploy/docker-compose` directory](../deploy/docker-compose).
