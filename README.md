# Quarkus Superheroes Sample

## Table of Contents
- [Introduction](#introduction)
- [Project automation](docs/automation.md)
    - [GitHub action automation](docs/automation.md#github-action-automation)
    - [Application Resource Generation](docs/automation.md#application-resource-generation)
- [Running Locally via Docker Compose](#running-locally-via-docker-compose)
- [Deploying to Kubernetes](#deploying-to-kubernetes)
    - [Routing](#routing)
    - [Versions](#versions)
    - [Monitoring](#monitoring)
    - [Jaeger](#jaeger)

## Introduction

This is a sample application demonstrating Quarkus features and best practices. The application allows superheroes to fight against supervillains. The application consists of several microservices, communicating either synchronously via REST or asynchronously using Kafka. All the data used by the applications are [on the `characterdata` branch](https://github.com/quarkusio/quarkus-super-heroes/tree/characterdata) of this repository.

This is **NOT** the workshop with the step-by-step instructions. If you are looking for the Quarkus Super Heroes workshop, you can find it at https://quarkus.io/quarkus-workshops/super-heroes/.

This is **NOT** a single multi-module project. Each service in the system is its own sub-directory of this parent directory. As such, each individual service needs to be run on its own.

The base JVM version for all the applications is Java 21.

- [Super Hero Battle UI](ui-super-heroes)
    - [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_ui-super-heroes&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_ui-super-heroes) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_ui-super-heroes&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_ui-super-heroes) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_ui-super-heroes&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_ui-super-heroes) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_ui-super-heroes&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_ui-super-heroes) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_ui-super-heroes&metric=coverage)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_ui-super-heroes) [![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_ui-super-heroes&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_ui-super-heroes)
    - A React application to pick up a random superhero, a random supervillain, a random location, and makes them fight. Then optionally use AI to perform a narration of the fight.
    - Served with [Quarkus Quinoa](https://quarkiverse.github.io/quarkiverse-docs/quarkus-quinoa/dev/index.html)
- [Villain REST API](rest-villains)
   - [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-villains&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-villains) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-villains&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-villains) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-villains&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-villains) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-villains&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-villains) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-villains&metric=coverage)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-villains) [![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-villains&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-villains)
   - A classical HTTP microservice exposing CRUD operations on Villains, stored in a PostgreSQL database.
    - Implemented with blocking endpoints using [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive) and [Quarkus Hibernate ORM with Panache's active record pattern](https://quarkus.io/guides/hibernate-orm-panache).
    - Favors field injection of beans (`@Inject` annotation) over construction injection.
    - Uses the [Quarkus Qute templating engine](https://quarkus.io/guides/qute) for its [UI](rest-villains/README.md#running-the-application).
    - Contains [contract verification tests](rest-villains/README.md#contract-testing-with-pact) using [Pact](https://pact.io).
- [Hero REST API](rest-heroes)
    - [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-heroes&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-heroes) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-heroes&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-heroes) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-heroes&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-heroes) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-heroes&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-heroes) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-heroes&metric=coverage)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-heroes) [![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-heroes&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-heroes)
    - A reactive HTTP microservice exposing CRUD operations on Heroes, stored in a PostgreSQL database.
    - Implemented with reactive endpoints using [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive) and [Quarkus Hibernate Reactive with Panache's repository pattern](http://quarkus.io/guides/hibernate-reactive-panache).
    - Favors constructor injection of beans over field injection (`@Inject` annotation).
    - Uses the [Quarkus Qute templating engine](https://quarkus.io/guides/qute) for its [UI](rest-heroes/README.md#running-the-application).
    - Contains [contract verification tests](rest-heroes/README.md#contract-testing-with-pact) using [Pact](https://pact.io).
- [Narration REST API](rest-narration)
    - [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-narration&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-narration) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-narration&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-narration) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-narration&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-narration) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-narration&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-narration) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-narration&metric=coverage)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-narration) [![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-narration&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-narration)
    - A blocking HTTP microservice integrating with [OpenAI](https://openai.com/) to narrate a fight.
    - Implemented with blocking endpoints using [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive).
    - Favors constructor injection of beans over field injection (`@Inject` annotation).
    - Contains [contract verification tests](rest-narration/README.md#contract-testing-with-pact) using [Pact](https://pact.io).
    - Uses the [Quarkus WireMock extension](https://docs.quarkiverse.io/quarkus-wiremock/dev/index.html) in development mode so that live calls that cost real money are not being made.
- [Location gRPC API](grpc-locations)
    - [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_grpc-locations&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_grpc-locations) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_grpc-locations&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_grpc-locations) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_grpc-locations&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_grpc-locations) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_grpc-locations&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_grpc-locations) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_grpc-locations&metric=coverage)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_grpc-locations) [![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_grpc-locations&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_grpc-locations)
    - A blocking microservice with [gRPC](https://quarkus.io/guides/grpc) operations exposing CRUD operations on Locations, stored in a MariaDB database.
    - Completely written in [Kotlin](https://quarkus.io/guides/kotlin).
    - Implemented with blocking endpoints using [Quarkus Hibernate ORM with Panache and Kotlin's repository pattern](https://quarkus.io/guides/hibernate-orm-panache-kotlin#using-the-repository-pattern).
    - Favors constructor injection of beans over field injection (`@Inject` annotation).
- [Fight REST API](rest-fights)
    - [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-fights&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-fights) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-fights&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-fights) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-fights&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-fights) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-fights&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-fights) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-fights&metric=coverage)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-fights) [![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_rest-fights&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_rest-fights)
    - A REST API invoking the Hero and Villain APIs to get a random superhero and supervillain. Each fight is then stored in a MongoDB database.
    - Invokes the [Narration API](rest-narration) to narrate the result of a fight.
    - Implemented with reactive endpoints using [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive) and [Quarkus MongoDB Reactive with Panache's active record pattern](https://quarkus.io/guides/mongodb-panache#reactive).
    - Invocations to the Hero and Villain APIs are done using the [reactive rest client](https://quarkus.io/guides/rest-client-reactive) and are protected using [resilience patterns](https://quarkus.io/guides/smallrye-fault-tolerance), such as retry, timeout, and circuit breaking.
    - Each fight is asynchronously sent, via Kafka, to the [Statistics](event-statistics) microservice.
        - Messages on Kafka use [Apache Avro](https://avro.apache.org/docs/current) schemas and are stored in an [Apicurio Registry](https://www.apicur.io/registry), all using [built-in support from Quarkus](https://quarkus.io/guides/kafka-schema-registry-avro).
    - Contains [consumer contract and contract verification tests](rest-fights/README.md#contract-testing-with-pact) using [Pact](https://pact.io).
- [Statistics](event-statistics)
    - [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_event-statistics&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_event-statistics) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_event-statistics&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_event-statistics) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_event-statistics&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_event-statistics) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_event-statistics&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_event-statistics) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_event-statistics&metric=coverage)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_event-statistics) [![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=quarkusio-quarkus-super-heroes_event-statistics&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=quarkusio-quarkus-super-heroes_event-statistics)
    - Calculates statistics about each fight and serves them to an HTML + JQuery UI using [WebSockets](https://quarkus.io/guides/websockets).
- [Grafana LGTM Stack](https://github.com/grafana/docker-otel-lgtm)
    - All services export traces, metrics, and logs via OpenTelemetry (OTLP) to the Grafana LGTM stack (Loki, Grafana, Tempo, Prometheus).

Here is an architecture diagram of the application:
![Superheroes architecture diagram](images/application-architecture.png)

The main UI allows you to pick one random Hero and Villain by clicking on _New Fighters_. Then, click _Fight!_ to start the battle. The table at the bottom shows the list of previous fights.

You can then click the _Narrate Fight_ button if you want to perform a narration using the [Narration Service](rest-narration).

> [!CAUTION]
> Using OpenAI may not be a free resource for you, so please understand this! Unless configured otherwise, the [Narration Service](rest-narration) does **NOT** communicate with any external service. Instead, by default, it just returns a default narration. See the [Integration with OpenAI Providers](rest-narration/README.md#integration-with-openai-providers) for more details.

![Fight screen](images/fight-screen.png)

https://github.com/quarkusio/quarkus-super-heroes/assets/363447/55a0a63f-c636-4719-9a7b-9a9034116e77

## Running Locally via Docker Compose
Pre-built images for all of the applications in the system can be found at [`quay.io/quarkus-super-heroes`](http://quay.io/quarkus-super-heroes).

Pick one of the 4 versions of the application from the table below and execute the appropriate docker compose command from the `quarkus-super-heroes` directory.

> [!NOTE]
> You may see errors as the applications start up. This may happen if an application completes startup before one of its required services (i.e. database, kafka, etc). This is fine. Once everything completes startup things will work fine.
>
> There is a [`watch-services.sh`](scripts/watch-services.sh) script that can be run in a separate terminal that will watch the startup of all the services and report when they are all up and ready to serve requests. 
>
> Run `scripts/watch-services.sh -h` for details about its usage.

| Description | Image Tag       | Docker Compose Run Command                                               | Docker Compose Run Command with Monitoring                                                                       |
|-------------|-----------------|--------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| JVM Java 21 | `java21-latest` | `docker compose -f deploy/docker-compose/java21.yml up --remove-orphans` | `docker compose -f deploy/docker-compose/java21.yml -f deploy/docker-compose/monitoring.yml up --remove-orphans` |
| Native      | `native-latest` | `docker compose -f deploy/docker-compose/native.yml up --remove-orphans` | `docker compose -f deploy/docker-compose/native.yml -f deploy/docker-compose/monitoring.yml up --remove-orphans` |

> [!TIP]
> If your system does not have the `compose` sub-command, you can try the above commands with the `docker-compose` command instead of `docker compose`.

Once started the main application will be exposed at `http://localhost:8080`. If you want to watch the [Event Statistics UI](event-statistics), that will be available at `http://localhost:8085`. The Apicurio Registry will be available at `http://localhost:8086`.

If you launched the monitoring stack, Grafana will be available at `http://localhost:3000`.

## Deploying to Kubernetes
Pre-built images for all of the applications in the system can be found at [`quay.io/quarkus-super-heroes`](http://quay.io/quarkus-super-heroes).

Deployment descriptors for these images are provided in the [`deploy/k8s`](deploy/k8s) directory. There are versions for [OpenShift](https://www.openshift.com), [Minikube](https://quarkus.io/guides/deploying-to-kubernetes#deploying-to-minikube), [Kubernetes](https://www.kubernetes.io), and [Knative](https://knative.dev).

> [!NOTE]
> The [Knative](https://knative.dev/docs/) variant can be used on any Knative installation that runs on top of Kubernetes or OpenShift. For OpenShift, you need [OpenShift Serverless](https://docs.openshift.com/serverless/latest/about/about-serverless.html) installed from the OpenShift operator catalog. Using Knative has the benefit that services are scaled down to zero replicas when they are not used.

The only real difference between the Minikube and Kubernetes descriptors is that all the application `Service`s in the Minikube descriptors use `type: NodePort` so that a list of all the applications can be obtained simply by running `minikube service list`.

> [!NOTE]
> If you'd like to deploy each application directly from source to Kubernetes, please follow the guide located within each application's folder (i.e. [`event-statistics`](event-statistics/README.md#deploying-directly-via-kubernetes-extensions), [`rest-fights`](rest-fights/README.md#deploying-directly-via-kubernetes-extensions), [`rest-heroes`](rest-heroes/README.md#deploying-directly-via-kubernetes-extensions), [`rest-villains`](rest-villains/README.md#deploying-directly-via-kubernetes-extensions), [`rest-narration`](rest-narration/README.md#deploying-directly-via-kubernetes-extensions), [`grpc-locations`](grpc-locations/README.md#deploying-directly-via-kubernetes-extensions)).

### Routing
Both the Minikube and Kubernetes descriptors also assume there is an [Ingress Controller](https://kubernetes.io/docs/concepts/services-networking/ingress-controllers/) installed and configured. There is a single `Ingress` in the Minikube and Kubernetes descriptors denoting `/` and `/api/fights` paths. You may need to add/update the `host` field in the `Ingress` as well in order for things to work.

Both the [`ui-super-heroes`](ui-super-heroes) and the [`rest-fights`](rest-fights) applications need to be exposed from outside the cluster. On Minikube and Kubernetes, the [`ui-super-heroes`](ui-super-heroes) Angular application communicates back to the same host and port as where it was launched from under the `/api/fights` path. [See the routing section in the UI project](ui-super-heroes/README.md#routing) for more details.

On OpenShift, the URL containing the `ui-super-heroes` host name is replaced with `rest-fights`. This is because the OpenShift descriptors use `Route` objects for gaining external access to the application. In most cases, no manual updating of the OpenShift descriptors is needed before deploying the system. Everything should work as-is.

Additionally, there is also a `Route` for the [`event-statistics`](event-statistics) application. On Minikube or Kubernetes, you will need to expose the [`event-statistics`](event-statistics) application, either by using an `Ingress` or doing a `kubectl port-forward`. The [`event-statistics`](event-statistics) application runs on port `8085`.

### Versions
Pick one of the 4 versions of the system from the table below and deploy the appropriate descriptor from the [`deploy/k8s` directory](deploy/k8s). Each descriptor contains all of the resources needed to deploy a particular version of the entire system.

> [!WARNING]
> These descriptors are **NOT** considered to be production-ready. They are basic enough to deploy and run the system with as little configuration as possible. The databases, Kafka broker, and schema registry deployed are not highly-available and do not use any Kubernetes operators for management or monitoring. They also only use ephemeral storage.
>
> For production-ready Kafka brokers, please see the [Strimzi documentation](https://strimzi.io/) for how to properly deploy and configure production-ready Kafka brokers on Kubernetes. You can also try out a [fully hosted and managed Kafka service](https://developers.redhat.com/products/red-hat-openshift-streams-for-apache-kafka/getting-started)!
>
> For a production-ready Apicurio Schema Registry, please see the [Apicurio Registry Operator documentation](https://www.apicur.io/registry/docs/apicurio-registry-operator/1.0.0/index.html). You can also try out a [fully hosted and managed Schema Registry service](https://console.redhat.com/application-services/service-registry)!

| Description | Image Tag       | OpenShift Descriptor                                      | Minikube Descriptor                                     | Kubernetes Descriptor                                       | Knative Descriptor                                    |
|-------------|-----------------|-----------------------------------------------------------|---------------------------------------------------------|-------------------------------------------------------------|-------------------------------------------------------|
| JVM Java 21 | `java21-latest` | [`java21-openshift.yml`](deploy/k8s/java21-openshift.yml) | [`java21-minikube.yml`](deploy/k8s/java21-minikube.yml) | [`java21-kubernetes.yml`](deploy/k8s/java21-kubernetes.yml) | [`java21-knative.yml`](deploy/k8s/java21-knative.yml) |
| Native      | `native-latest` | [`native-openshift.yml`](deploy/k8s/native-openshift.yml) | [`native-minikube.yml`](deploy/k8s/native-minikube.yml) | [`native-kubernetes.yml`](deploy/k8s/native-kubernetes.yml) | [`native-knative.yml`](deploy/k8s/native-knative.yml) |

### Monitoring
There are also Kubernetes deployment descriptors for monitoring with the [Grafana LGTM stack](https://github.com/grafana/docker-otel-lgtm) (Grafana, Loki, Tempo, Prometheus) in the [`deploy/k8s` directory](deploy/k8s) ([`monitoring-openshift.yml`](deploy/k8s/monitoring-openshift.yml), [`monitoring-minikube.yml`](deploy/k8s/monitoring-minikube.yml), [`monitoring-kubernetes.yml`](deploy/k8s/monitoring-kubernetes.yml)). Each descriptor contains the resources necessary to monitor and gather metrics, traces, and logs from all of the applications in the system. Deploy the appropriate descriptor to your cluster if you want it.

The OpenShift descriptor will automatically create a `Route` for Grafana. On Kubernetes/Minikube you may need to expose the Grafana service in order to access it from outside your cluster, either by using an `Ingress` or by using `kubectl port-forward`. On Minikube, the Grafana `Service` is also exposed as a `NodePort`.

> [!WARNING]
> These descriptors are **NOT** considered to be production-ready. They are basic enough to deploy the Grafana LGTM stack with as little configuration as possible. It is not highly-available and does not use any Kubernetes operators for management or monitoring. It also only uses ephemeral storage.

### Observability with Grafana

By now you've performed a few battles, so let's analyze the telemetry data.
Open the Grafana UI based on how you are running the system, either through Docker Compose (`http://localhost:3000`) or by deploying the monitoring stack to Kubernetes.

All services export traces, metrics, and logs via [OpenTelemetry](https://opentelemetry.io) (OTLP) to the Grafana LGTM stack. In the Grafana UI you can:
- **Explore traces** using the Tempo data source
- **View metrics** using the pre-provisioned "Quarkus Micrometer OpenTelemetry" dashboard
- **Search logs** using the Loki data source, with automatic log-to-trace correlation

## Deploying with Helm
Pre-built Helm charts for all of the applications in the system can be found in the [`deploy/helm`](deploy/helm) directory. There is an umbrella chart for each deployment target that includes all services as dependencies.

> [!NOTE]
> Helm charts are generated by the [Quarkus Helm extension](https://docs.quarkiverse.io/quarkus-helm/dev/index.html). Each chart has separate values files for JVM and native variants. Users must specify a values file at install time (e.g. `-f values-java21.yaml` or `-f values-native.yaml`).

> [!WARNING]
> These charts are **NOT** considered to be production-ready. They are basic enough to deploy and run the system with as little configuration as possible.

### Deploying the full system
Pick the umbrella chart for your deployment target:

| Deployment Target | Umbrella Chart |
|-------------------|----------------|
| OpenShift | [`deploy/helm/openshift`](deploy/helm/openshift) |
| Minikube | [`deploy/helm/minikube`](deploy/helm/minikube) |
| Kubernetes | [`deploy/helm/kubernetes`](deploy/helm/kubernetes) |
| Knative | [`deploy/helm/knative`](deploy/helm/knative) |

To deploy the full system (e.g. JVM Java 21 on Kubernetes):
```shell
helm dependency build deploy/helm/kubernetes
helm install super-heroes deploy/helm/kubernetes -f deploy/helm/kubernetes/values-java21.yaml
```

For native:
```shell
helm dependency build deploy/helm/kubernetes
helm install super-heroes deploy/helm/kubernetes -f deploy/helm/kubernetes/values-native.yaml
```

### Deploying individual services
Each service has its own chart with separate values files for JVM and native:
```shell
helm install rest-heroes rest-heroes/deploy/helm/kubernetes/ -f rest-heroes/deploy/helm/kubernetes/values-java21.yaml
```

For native:
```shell
helm install rest-heroes rest-heroes/deploy/helm/kubernetes/ -f rest-heroes/deploy/helm/kubernetes/values-native.yaml
```
