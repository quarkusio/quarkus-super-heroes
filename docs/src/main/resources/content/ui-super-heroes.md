---
title: Super Hero Battle UI
description: A React application served via Quarkus Quinoa for superhero battles with optional AI narration.
layout: page
content-toc: true
---

## Introduction

This is the main user interface for the application. The application is a React application served via [Quinoa](https://quarkus.io/extensions/io.quarkiverse.quinoa/quarkus-quinoa).

![ui-super-heroes]({site.image('ui-super-heroes.png')})

The main UI allows you to pick up one random Hero and Villain by clicking on "New Fighters", then pick a random location for the fight to take place by clicking on "New Location".

Then it's just a matter of clicking on "Fight!" to get them to fight. The table at the bottom shows the list of the previous fights.

![main-ui]({site.image('main-ui.png')})

## Building the Application

Environment variables can be injected into the build using the [ngx-env](https://github.com/chihab/ngx-env) plugin. Remember, these are pulled in at build time and are inserted as string literals in the resulting JS files.

Production builds are served using a Quarkus server. This server serves the compiled React application and an `env.js` file. This `env.js` file is generated at startup, and adds a `window.APP_CONFIG` property that the React application can read from.

Currently, the `env.js` will expose just the `API_BASE_URL` that's set at runtime. This will control the base URL to connect to the [fights]({site.url('/rest-fights')}) service. The default if unset is `http://localhost:8082`. You can control the base URL using normal Quarkus configuration, such as setting `api.base.url` in `application.properties` or an `API_BASE_URL` environment variable.

```bash
quarkus package
```

It is also possible to build a native binary, using:

```bash
./mvnw -B clean package -DskipTests -Pnative
```

## Local Development

Use the following command:

```shell
quarkus dev
```

This starts both Quarkus and the React hot reloading server at `http://localhost:3000`. The Quarkus server supplies the `env.js` file to the Javascript front-end.

The Quarkus server port can be changed in the usual way, with `application.properties`.

## Integration with Microcks

[Microcks](https://microcks.io) is an open source tool for API mocking and testing. It allows developers to turn an API contract or [Postman Collection](https://learning.postman.com/docs/getting-started/first-steps/creating-the-first-collection) into live mocks.

This can be especially useful while developing applications that have downstream dependencies, like the `ui-super-heroes` application does. The `ui-super-heroes` application depends on [rest-fights]({site.url('/rest-fights')}).

This problem is what the [Microcks Quarkus extension](https://github.com/microcks/microcks-quarkus) solves. This extension has been integrated into `ui-super-heroes` so that when live coding in dev mode, mock responses from [rest-fights]({site.url('/rest-fights')}) will automatically get served.

Furthermore, the [Microcks user interface](https://microcks.io/documentation/using/mocks) is accessible from the [Quarkus Dev UI](https://quarkus.io/guides/dev-ui):

![Microcks user interface]({site.image('microcks-dev-ui.png')})

If you wish to disable this functionality, simply add `-Dquarkus.profile=no-microcks` when you run Quarkus dev mode (via [Maven](https://quarkus.io/guides/maven-tooling#dev-mode), [Gradle](https://quarkus.io/guides/gradle-tooling#dev-mode), or the [Quarkus CLI](https://quarkus.io/guides/cli-tooling#development-mode)). In this case, the Microcks dev service will be disabled and the `ui-super-heroes` application will attempt to make live calls to the downstream services.

## Running the Application

As discussed in the [Integration with Microcks](#integration-with-microcks) section, simply running this application in dev mode will get you up and running with default mock responses.

If you want _real_ backend services, follow these instructions:

1. Start up all of the downstream services ([Heroes Service]({site.url('/rest-heroes')}), [Villains Service]({site.url('/rest-villains')}), [Location Service]({site.url('/grpc-locations')}), and [Fights Service]({site.url('/rest-fights')})).
    - The [Event Statistics Service]({site.url('/event-statistics')}) is optional.
2. Follow the steps in the *Building the Application* section above.
3. Start the service using the command `quarkus dev -Dquarkus.profile=no-microcks`.
    - You can also set the environment variable `CALCULATE_API_BASE_URL=true` to have it compute the base URL. Only use this option if the UI url is in the form of `ui-super-heroes.somewhere.com`. In this instance, setting `CALCULATE_API_BASE_URL=true` will replace `ui-super-heroes` in the URL with `rest-fights`.

There is also a container image available that you can use instead:

```bash
docker run -p 8080:8080 -e API_BASE_URL=http://localhost:8082 quay.io/quarkus-super-heroes/ui-super-heroes:latest
```

## Running Locally via Docker Compose

Pre-built images for this application can be found at [`quay.io/quarkus-super-heroes/ui-super-heroes`](https://quay.io/repository/quarkus-super-heroes/ui-super-heroes?tab=tags).

The application can be started outside of docker compose simply with `docker run -p 8080:8080 quay.io/quarkus-super-heroes/ui-super-heroes:latest`.

If you want to use docker compose, from the `quarkus-super-heroes/ui-super-heroes` directory run:

```bash
docker compose -f deploy/docker-compose/java25.yml up
```

or

```bash
docker compose -f deploy/docker-compose/native.yml up
```

Once started the application will be exposed at `http://localhost:8080`.

## Deploying to Kubernetes

The application can be deployed to Kubernetes using pre-built images or by deploying directly via the Quarkus Kubernetes Extension.

### Using pre-built images

Pre-built images for this application can be found at [`quay.io/quarkus-super-heroes/ui-super-heroes`](https://quay.io/repository/quarkus-super-heroes/ui-super-heroes?tab=tags).

Deployment descriptors for these images are provided in the `deploy/k8s` directory. There are versions for [OpenShift](https://www.openshift.com), [Minikube](https://quarkus.io/guides/deploying-to-kubernetes#deploying-to-minikube), [Kubernetes](https://www.kubernetes.io), and [Knative](https://knative.dev).

**Note:** The [Knative](https://knative.dev/docs/) variant can be used on any Knative installation that runs on top of Kubernetes or OpenShift. For OpenShift, you need [OpenShift Serverless](https://docs.openshift.com/serverless/latest/about/about-serverless.html) installed from the OpenShift operator catalog. Using Knative has the benefit that services are scaled down to zero replicas when they are not used.

Pick one of the versions of the application from the table below and deploy the appropriate descriptor from the `deploy/k8s` directory.

| Description | Image Tag       | OpenShift Descriptor            | Minikube Descriptor            | Kubernetes Descriptor              | Knative Descriptor            |
|-------------|-----------------|---------------------------------|--------------------------------|------------------------------------|-------------------------------|
| JVM Java 25 | `java25-latest` | `java25-openshift.yml`          | `java25-minikube.yml`          | `java25-kubernetes.yml`            | `java25-knative.yml`          |
| Native      | `native-latest` | `native-openshift.yml`          | `native-minikube.yml`          | `native-kubernetes.yml`            | `native-knative.yml`          |

The application is exposed outside of the cluster on port `80`.

### Using Helm

Helm charts for this application are provided in the `deploy/helm` directory with separate charts per deployment target.

To deploy using Helm (e.g. JVM Java 25 on Kubernetes):

```shell
helm install ui-super-heroes deploy/helm/kubernetes/ -f deploy/helm/kubernetes/values-java25.yaml
```

For native:

```shell
helm install ui-super-heroes deploy/helm/kubernetes/ -f deploy/helm/kubernetes/values-native.yaml
```

### Deploying directly via Kubernetes Extensions

Following the [deployment section](https://quarkus.io/guides/deploying-to-kubernetes#deployment) of the [Quarkus Kubernetes Extension Guide](https://quarkus.io/guides/deploying-to-kubernetes), you can run one of the following commands to deploy the application and any of its dependencies to your preferred Kubernetes distribution.

**Note:** For non-OpenShift or minikube Kubernetes variants, you will most likely need to [push the image to a container registry](https://quarkus.io/guides/container-image#pushing) by adding the `-Dquarkus.container-image.push=true` flag, as well as setting the `quarkus.container-image.registry`, `quarkus.container-image.group`, and/or the `quarkus.container-image.name` properties to different values.

| Target Platform        | Java Version | Command                                                                                                                                                                                                                                      |
|------------------------|:------------:|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Kubernetes             | 25 | `./mvnw clean package -Dquarkus.profile=kubernetes -Dquarkus.kubernetes.deploy=true -DskipTests`                                                                                                                                             |
| OpenShift              | 25 | `./mvnw clean package -Dquarkus.profile=openshift -Dquarkus.container-image.registry=image-registry.openshift-image-registry.svc:5000 -Dquarkus.container-image.group=$(oc project -q) -Dquarkus.kubernetes.deploy=true -DskipTests`         |
| Minikube               | 25 | `./mvnw clean package -Dquarkus.profile=minikube -Dquarkus.kubernetes.deploy=true -DskipTests`                                                                                                                                               |
| Knative                | 25 | `./mvnw clean package -Dquarkus.profile=knative -Dquarkus.kubernetes.deploy=true -DskipTests`                                                                                                                                                |
| Knative (on OpenShift) | 25 | `./mvnw clean package -Dquarkus.profile=knative-openshift -Dquarkus.container-image.registry=image-registry.openshift-image-registry.svc:5000 -Dquarkus.container-image.group=$(oc project -q) -Dquarkus.kubernetes.deploy=true -DskipTests` |

### Routing

There are 2 environment variables that can be set on this application to control how the React UI communicates with the [rest-fights]({site.url('/rest-fights')}) application:

| Env Var                  | Default Value                                        | Description                                                                                                                                                                                                                                                                                                      |
|--------------------------|------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `API_BASE_URL`           | `undefined`                                          | The base URL for the [rest-fights]({site.url('/rest-fights')}) application. Set this to a fully qualified URL (i.e. `http://www.example.com` or `http://somehost.com:someport`) to define the URL for the [rest-fights]({site.url('/rest-fights')}) application.                                                                              |
| `CALCULATE_API_BASE_URL` | `false` on Minikube/Kubernetes. `true` on OpenShift. | If `true`, look at the URL in the browser and replace the `ui-super-heroes` host name with `rest-fights`. This is because on OpenShift, each application has its own `Route` which exposes a unique hostname within the cluster. On Minikube and Kubernetes, an `Ingress` using different paths is used instead. |

---

[View source on GitHub](https://github.com/quarkusio/quarkus-super-heroes/tree/main/ui-super-heroes)
