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

## Introduction

This is a sample application demonstrating Quarkus features and best practices. The application allows superheroes to fight against supervillains. The application consists of several microservices, communicating either synchronously via REST or asynchronously using Kafka. All the data used by the applications are [on the `characterdata` branch](https://github.com/quarkusio/quarkus-super-heroes/tree/characterdata) of this repository.
- [Super Hero Battle UI](ui-super-heroes)
    - An Angular application to pick up a random superhero, a random supervillain, and makes them fight.
- [Villain REST API](rest-villains)
    - A classical HTTP microservice exposing CRUD operations on Villains, stored in a PostgreSQL database.
    - Implemented with blocking endpoints using [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive) and [Quarkus Hibernate ORM with Panache's active record pattern](https://quarkus.io/guides/hibernate-orm-panache).
    - Favors field injection of beans (`@Inject` annotation) over construction injection.
    - Uses the [Quarkus Qute templating engine](https://quarkus.io/guides/qute) for its [UI](rest-villains/README.md#running-the-application).
- [Hero REST API](rest-heroes)
    - A reactive HTTP microservice exposing CRUD operations on Heroes, stored in a PostgreSQL database.
    - Implemented with reactive endpoints using [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive) and [Quarkus Hibernate Reactive with Panache's repository pattern](http://quarkus.io/guides/hibernate-reactive-panache).
    - Favors constructor injection of beans over field injection (`@Inject` annotation).
- [Fight REST API](rest-fights)
    - A REST API invoking the Hero and Villain APIs to get a random superhero and supervillain. Each fight is then stored in a PostgreSQL database.
    - Implemented with reactive endpoints using [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive) and [Quarkus Hibernate Reactive with Panache's active record pattern](https://quarkus.io/guides/hibernate-reactive-panache#solution-1-using-the-active-record-pattern).
    - Invocations to the Hero and Villain APIs are done using the [reactive rest client](https://quarkus.io/guides/rest-client-reactive) and are protected using [resilience patterns](https://quarkus.io/guides/smallrye-fault-tolerance), such as retry, timeout, and circuit breaking.
    - Each fight is asynchronously sent, via Kafka, to the [Statistics](event-statistics) microservice.
        - Messages on Kafka use [Apache Avro](https://avro.apache.org/docs/current) schemas and are stored in an [Apicurio Registry](https://www.apicur.io/registry), all using [built-in support from Quarkus](https://quarkus.io/guides/kafka-schema-registry-avro).
- [Statistics](event-statistics)
    - Calculates statistics about each fight and serves them to an HTML + JQuery UI using [WebSockets](https://quarkus.io/guides/websockets).
- [Prometheus](https://prometheus.io/)
    - Polls metrics from all of the services within the system.

Here is an architecture diagram of the application:
![Superheroes architecture diagram](images/application-architecture.png)

The main UI allows you to pick one random Hero and Villain by clicking on _New Fighters_. Then, click _Fight!_ to start the battle. The table at the bottom shows the list of previous fights.
![Fight screen](images/fight-screen.png)

## Running Locally via Docker Compose
Pre-built images for all of the applications in the system can be found at [`quay.io/quarkus-super-heroes`](http://quay.io/quarkus-super-heroes).

Pick one of the 4 versions of the application from the table below and execute the appropriate docker compose command from the `quarkus-super-heroes` directory.

   > **NOTE**: You may see errors as the applications start up. This may happen if an application completes startup before one if its required services (i.e. database, kafka, etc). This is fine. Once everything completes startup things will work fine.
   >
   > There is a [`watch-services.sh`](scripts/watch-services.sh) script that can be run in a separate terminal that will watch the startup of all the services and report when they are all up and ready to serve requests. 

| Description                  | Image Tag              | Docker Compose Run Command                                                      | Docker Compose Run Command with Prometheus Monitoring                                                                   |
|------------------------------|------------------------|---------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| JVM Java 11                  | `java11-latest`        | `docker-compose -f deploy/docker-compose/java11.yml up --remove-orphans`        | `docker-compose -f deploy/docker-compose/java11.yml -f deploy/docker-compose/prometheus.yml up --remove-orphans`        |
| JVM Java 17                  | `java17-latest`        | `docker-compose -f deploy/docker-compose/java17 up --remove-orphans`            | `docker-compose -f deploy/docker-compose/java17.yml -f deploy/docker-compose/prometheus.yml up --remove-orphans`        |
| Native compiled with Java 11 | `native-java11-latest` | `docker-compose -f deploy/docker-compose/native-java11.yml up --remove-orphans` | `docker-compose -f deploy/docker-compose/native-java11.yml -f deploy/docker-compose/prometheus.yml up --remove-orphans` |
| Native compiled with Java 17 | `native-java17-latest` | `docker-compose -f deploy/docker-compose/native-java17.yml up --remove-orphans` | `docker-compose -f deploy/docker-compose/native-java17.yml -f deploy/docker-compose/prometheus.yml up --remove-orphans` |

Once started the main application will be exposed at `http://localhost:8080`. If you want to watch the [Event Statistics UI](event-statistics), that will be available at `http://localhost:8085`.

If you launched Prometheus monitoring, Prometheus will be available at `http://localhost:9090`. The Apicurio Registry will be available at `http://localhost:8086`.

## Deploying to Kubernetes
Pre-built images for all of the applications in the system can be found at [`quay.io/quarkus-super-heroes`](http://quay.io/quarkus-super-heroes).

Deployment descriptors for these images are provided in the [`deploy/k8s`](deploy/k8s) directory. There are versions for [OpenShift](https://www.openshift.com), [Minikube](https://quarkus.io/guides/deploying-to-kubernetes#deploying-to-minikube), [Kubernetes](https://www.kubernetes.io), and [KNative](https://knative.dev).

The only real difference between the Minikube and Kubernetes descriptors is that all the application `Service`s in the Minikube descriptors use `type: NodePort` so that a list of all the applications can be obtained simply by running `minikube service list`.

### Routing
Both the Minikube and Kubernetes descriptors also assume there is an [Ingress Controller](https://kubernetes.io/docs/concepts/services-networking/ingress-controllers/) installed and configured. There is a single `Ingress` in the Minikube and Kubernetes descriptors denoting `/` and `/api/fights` paths. You may need to add/update the `host` field in the `Ingress` as well in order for things to work.

Both the [`ui-super-heroes`](ui-super-heroes) and the [`rest-fights`](rest-fights) applications need to be exposed from outside the cluster. On Minikube and Kubernetes, the [`ui-super-heroes`](ui-super-heroes) Angular application communicates back to the same host and port as where it was launched from under the `/api/fights` path. [See the routing section in the UI project](ui-super-heroes/README.md#routing) for more details.

On OpenShift, the URL containing the `ui-super-heroes` host name is replaced with `rest-fights`. This is because the OpenShift descriptors use `Route` objects for gaining external access to the application. In most cases, no manual updating of the OpenShift descriptors is needed before deploying the system. Everything should work as-is.

Additionally, there is also a `Route` for the [`event-statistics`](event-statistics) application. On Minikube or Kubernetes, you will need to expose the [`event-statistics`](event-statistics) application, either by using an `Ingress` or doing a `kubectl port-forward`. The [`event-statistics`](event-statistics) application runs on port `8085`.

### Versions
Pick one of the 4 versions of the system from the table below and deploy the appropriate descriptor from the [`deploy/k8s` directory](deploy/k8s). Each descriptor contains all of the resources needed to deploy a particular version of the entire system.

   > **NOTE:** These descriptors are **NOT** considered to be production-ready. They are basic enough to deploy and run the system with as little configuration as possible. The databases, Kafka broker, and schema registry deployed are not highly-available and do not use any Kubernetes operators for management or monitoring. They also only use ephemeral storage.
   >
   > For production-ready Kafka brokers, please see the [Strimzi documentation](https://strimzi.io/) for how to properly deploy and configure production-ready Kafka brokers on Kubernetes. You can also try out a [fully hosted and managed Kafka service](https://developers.redhat.com/products/red-hat-openshift-streams-for-apache-kafka/getting-started)!
   >
   > For a production-ready Apicurio Schema Registry, please see the [Apicurio Registry Operator documentation](https://www.apicur.io/registry/docs/apicurio-registry-operator/1.0.0/index.html). You can also try out a [fully hosted and managed Schema Registry service](https://console.redhat.com/application-services/service-registry)!

| Description                  | Image Tag              | OpenShift Descriptor                                                    | Minikube Descriptor                                                   | Kubernetes Descriptor                                                     | KNative Descriptor                                                  |
|------------------------------|------------------------|-------------------------------------------------------------------------|-----------------------------------------------------------------------|---------------------------------------------------------------------------|---------------------------------------------------------------------|
| JVM Java 11                  | `java11-latest`        | [`java11-openshift.yml`](deploy/k8s/java11-openshift.yml)               | [`java11-minikube.yml`](deploy/k8s/java11-minikube.yml)               | [`java11-kubernetes.yml`](deploy/k8s/java11-kubernetes.yml)               | [`java11-knative.yml`](deploy/k8s/java11-knative.yml)               |
| JVM Java 17                  | `java17-latest`        | [`java17-openshift.yml`](deploy/k8s/java17-openshift.yml)               | [`java17-minikube.yml`](deploy/k8s/java17-minikube.yml)               | [`java17-kubernetes.yml`](deploy/k8s/java17-kubernetes.yml)               | [`java17-knative.yml`](deploy/k8s/java17-knative.yml)               |
| Native compiled with Java 11 | `native-java11-latest` | [`native-java11-openshift.yml`](deploy/k8s/native-java11-openshift.yml) | [`native-java11-minikube.yml`](deploy/k8s/native-java11-minikube.yml) | [`native-java11-kubernetes.yml`](deploy/k8s/native-java11-kubernetes.yml) | [`native-java11-knative.yml`](deploy/k8s/native-java11-knative.yml) |
| Native compiled with Java 17 | `native-java17-latest` | [`native-java17-openshift.yml`](deploy/k8s/native-java17-openshift.yml) | [`native-java17-minikube.yml`](deploy/k8s/native-java17-minikube.yml) | [`native-java17-kubernetes.yml`](deploy/k8s/native-java17-kubernetes.yml) | [`native-java17-knative.yml`](deploy/k8s/native-java17-knative.yml) |

### Monitoring
There are also Kubernetes deployment descriptors for Prometheus monitoring in the [`deploy/k8s` directory](deploy/k8s) ([`prometheus-openshift.yml`](deploy/k8s/prometheus-openshift.yml), [`prometheus-minikube.yml`](deploy/k8s/prometheus-minikube.yml), [`prometheus-kubernetes.yml`](deploy/k8s/prometheus-kubernetes.yml)). Each descriptor contains the resources necessary to monitor and gather metrics from all of the applications in the system. Deploy the appropriate descriptor to your cluster if you want it.

The OpenShift descriptor will automatically create a `Route` for Prometheus. On Kubernetes/Minikube you may need to expose the Prometheus service in order to access it from outside your cluster, either by using an `Ingress` or by using `kubectl port-forward`. On Minikube, the Prometheus `Service` is also exposed as a `NodePort`.

   > **NOTE:** These descriptors are **NOT** considered to be production-ready. They are basic enough to deploy Prometheus with as little configuration as possible. It is not highly-available and does not use any [Kubernetes operators](https://operatorhub.io/operator/prometheus) for management or monitoring. It also only uses ephemeral storage.
   >
   > For production-ready Prometheus instances, please see the [Prometheus Operator documentation](https://operatorhub.io/operator/prometheus) for how to properly deploy and configure production-ready instances. 
