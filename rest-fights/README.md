# Superheroes Fight Microservice

## Table of Contents
- [Introduction](#introduction)
- [Configuration](#configuration)
- [Resiliency](#resiliency)
    - [Timeouts](#timeouts)
    - [Fallbacks](#fallbacks)
    - [Retries](#retries)
        - [Hero Client](#hero-client)
        - [Villain Client](#villain-client)
- [Testing](#testing) 
- [Running the Application](#running-the-application)
- [Running Locally via Docker Compose](#running-locally-via-docker-compose)
- [Deploying to Kubernetes](#deploying-to-kubernetes)

## Introduction
This is the Fight REST API microservice. It is a reactive HTTP microservice exposing an API for performing fights between [Heroes](../rest-heroes) and [Villains](../rest-villains). Each fight is persisted into a PostgreSQL database and can be retrieved via the REST API. This service is implemented using [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive) with reactive endpoints and [Quarkus Hibernate Reactive with Panache's active record pattern](http://quarkus.io/guides/hibernate-reactive-panache).

![rest-fights](images/rest-fights.png)

The following table lists the available REST endpoints. The [OpenAPI document](openapi-schema.yml) for the REST endpoints is also available.

| Path                         | HTTP method | Response Status | Response Object                                                               | Description                                                                                                                             |
|------------------------------|-------------|-----------------|-------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `/api/fights`                | `GET`       | `200`           | [`List<Fight>`](src/main/java/io/quarkus/sample/superheroes/fight/Fight.java) | All Fights. Empty array (`[]`) if none.                                                                                                 |
| `/api/fights`                | `POST`      | `200`           | [`Fight`](src/main/java/io/quarkus/sample/superheroes/fight/Fight.java)       | Performs a fight.                                                                                                                       |
| `/api/fights`                | `POST`      | `400`           |                                                                               | Invalid [`Fighters`](src/main/java/io/quarkus/sample/superheroes/fight/Fighters.java) passed in request body (or no request body found) |
| `/api/fights/randomfighters` | `GET`       | `200`           | [`Fighters`](src/main/java/io/quarkus/sample/superheroes/fight/Fighters.java) | Finds random fighters                                                                                                                   |
| `/api/fights/{id}`           | `GET`       | `200`           | [`Fight`](src/main/java/io/quarkus/sample/superheroes/fight/Fight.java)       | Fight with id == `{id}`                                                                                                                 |
| `/api/fights/{id}`           | `GET`       | `404`           |                                                                               | No Fight with id == `{id}` found                                                                                                        |
| `/api/fights/hello`          | `GET`       | `200`           | `String`                                                                      | Ping "hello" endpoint                                                                                                                   |

## Configuration
The [`FightConfig`](src/main/java/io/quarkus/sample/superheroes/fight/config/FightConfig.java) stores all the application-specific configuration that can be overridden at runtime.

## Resiliency
### Timeouts
The [`FightService`](src/main/java/io/quarkus/sample/superheroes/fight/service/FightService.java) class uses [timeouts](https://quarkus.io/guides/smallrye-fault-tolerance#adding-resiliency-timeouts) from [SmallRye Fault Tolerance](https://quarkus.io/guides/smallrye-fault-tolerance) to protect against calls to the downstream [Hero](../rest-heroes) and [Villain](../rest-villains) services. Tests for these conditions can be found in [`FightServiceTests`](src/test/java/io/quarkus/sample/superheroes/fight/service/FightServiceTests.java).

### Fallbacks
The [`FightService`](src/main/java/io/quarkus/sample/superheroes/fight/service/FightService.java) class uses [fallbacks](https://quarkus.io/guides/smallrye-fault-tolerance#adding-resiliency-fallbacks) from [SmallRye Fault Tolerance](https://quarkus.io/guides/smallrye-fault-tolerance) to protect against calls to the downstream [Hero](../rest-heroes) and [Villain](../rest-villains) services. Tests for these conditions can be found in [`FightServiceTests`](src/test/java/io/quarkus/sample/superheroes/fight/service/FightServiceTests.java).

### Retries
Retry logic to the downstream [Hero](../rest-heroes) and [Villain](../rest-villains) services is implemented in the clients for each service.

#### Hero Client
The [`HeroRestClient`](src/main/java/io/quarkus/sample/superheroes/fight/client/HeroRestClient.java) is implemented using the [reactive rest client](https://quarkus.io/guides/rest-client-reactive). All of its configuration can be found in [`application.properties`](src/main/resources/application.properties) under the `quarkus.rest-client.hero-client` key. This client is not exposed outside of the `io.quarkus.sample.superheroes.fight.client` package.

Instead, the [`HeroClient`](src/main/java/io/quarkus/sample/superheroes/fight/client/HeroClient.java) class wraps the `HeroRestClient` and adds some resiliency to it:
- The downstream [Hero service](../rest-heroes) returns a `404` if no random [`Hero`](src/main/java/io/quarkus/sample/superheroes/fight/client/Hero.java) is found. `HeroClient` handles this case and simulates the service returning nothing.
- In the event the downstream [Hero service](../rest-heroes) returns an error, `HeroClient` adds 3 retries with a 200ms delay between each retry.

#### Villain Client
The [`VillainClient`](src/main/java/io/quarkus/sample/superheroes/fight/client/VillainClient.java) is implemented using the [JAX-RS client API](https://docs.oracle.com/javaee/7/tutorial/jaxrs-client001.htm) with the [RESTEasy Reactive client](https://quarkus.io/guides/resteasy-reactive#resteasy-reactive-client). All of its configuration can be found in [`application.properties`](src/main/resources/application.properties) under the `fight.villain.client-base-url` key.
- The downstream [Villain service](../rest-villains) returns a `404` if no random [`Villain`](src/main/java/io/quarkus/sample/superheroes/fight/client/Villain.java) is found. `VillainClient` handles this case and simulates the service returning nothing.
- In the event the downstream [Villain service](../rest-heroes) returns an error, `VillainClient` adds 3 retries with a 200ms delay between each retry.

## Testing
This application has a full suite of tests, including an [integration test suite](src/test/java/io/quarkus/sample/superheroes/fight/rest/FightResourceIT.java). 
- The test suite uses [Wiremock](http://wiremock.org/) for [mocking http calls](https://quarkus.io/guides/rest-client-reactive#using-a-mock-http-server-for-tests) (see [`HeroesVillainsWiremockServerResource`](src/test/java/io/quarkus/sample/superheroes/fight/HeroesVillainsWiremockServerResource.java)) to the downstream [Hero](../rest-heroes) and [Villain](../rest-villains) services.
- The test suite configures the application to use the [in-memory connector](https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/3.11/testing/testing.html) from [SmallRye Reactive Messaging](https://smallrye.io/smallrye-reactive-messaging) (see the `%test.mp.messaging.outgoing.fights` configuration in [`application.properties`](src/main/resources/application.properties)) for verifying interactions with Kafka.
- The [integration test suite](src/test/java/io/quarkus/sample/superheroes/fight/rest/FightResourceIT.java) uses [Quarkus Dev Services](https://quarkus.io/guides/getting-started-testing#testing-dev-services) (see [`KafkaConsumerResource`](src/test/java/io/quarkus/sample/superheroes/fight/KafkaConsumerResource.java)) to interact with a Kafka instance so messages placed onto the Kafka broker by the application can be verified.

## Running the Application
First you need to start up all of the downstream services ([Heroes Service](../rest-heroes) and [Villains Service](../rest-villains) - the [Event Statistics Service](../event-statistics) is optional).

The application runs on port `8082` (defined by `quarkus.http.port` in [`application.properties`](src/main/resources/application.properties)).

From the `quarkus-super-heroes/rest-fights` directory, simply run `./mvnw quarkus:dev` to run [Quarkus Dev Mode](https://quarkus.io/guides/maven-tooling#dev-mode), or running `quarkus dev` using the [Quarkus CLI](https://quarkus.io/guides/cli-tooling). The application will be exposed at http://localhost:8082 and the [Quarkus Dev UI](https://quarkus.io/guides/dev-ui) will be exposed at http://localhost:8082/q/dev.

**NOTE:** Running the application outside of Quarkus dev mode requires standing up a PostgreSQL instance and an Apache Kafka instance and binding them to the app.

Furthermore, since this service also communicates with additional downstream services ([rest-heroes](../rest-heroes) and [rest-villains](../rest-villains)), those would need to be stood up as well, although this service does have fallbacks in case those other services aren't available.

By default, the application is configured with the following:

| Description             | Environment Variable                  | Java Property                         | Value                                         |
|-------------------------|---------------------------------------|---------------------------------------|-----------------------------------------------|
| Database URL            | `QUARKUS_DATASOURCE_REACTIVE_URL`     | `quarkus.datasource.reactive.url`     | `postgresql://localhost:5432/fights_database` |
| Database username       | `QUARKUS_DATASOURCE_USERNAME`         | `quarkus.datasource.username`         | `superfight`                                  |
| Database password       | `QUARKUS_DATASOURCE_PASSWORD`         | `quarkus.datasource.password`         | `superfight`                                  |
| Kafka Bootstrap servers | `KAFKA_BOOTSTRAP_SERVERS`             | `kafka.bootstrap.servers`             | `PLAINTEXT://localhost:9092`                  |
| Heroes Service URL      | `quarkus.rest-client.hero-client.url` | `quarkus.rest-client.hero-client.url` | `http://localhost:8083`                       |
| Villains Service URL    | `fight.villain.client-base-url`       | `fight.villain.client-base-url`       | `http://localhost:8084`                       |

## Running Locally via Docker Compose
Pre-built images for this application can be found at [`quay.io/quarkus-super-heroes/rest-fights`](https://quay.io/repository/quarkus-super-heroes/rest-fights?tab=tags). 

Pick one of the 4 versions of the application from the table below and execute the appropriate docker compose command from the `quarkus-super-heroes/rest-fights` directory.

   > **NOTE**: You may see errors as the applications start up. This may happen if an application completes startup before one if its required services (i.e. database, kafka, etc). This is fine. Once everything completes startup things will work fine.

| Description                  | Image Tag              | Docker Compose Run Command                                                      |
|------------------------------|------------------------|---------------------------------------------------------------------------------|
| JVM Java 11                  | `java11-latest`        | `docker-compose -f deploy/docker-compose/java11.yml up --remove-orphans`        |
| JVM Java 17                  | `java17-latest`        | `docker-compose -f deploy/docker-compose/java17.yml up --remove-orphans`        |
| Native compiled with Java 11 | `native-java11-latest` | `docker-compose -f deploy/docker-compose/native-java11.yml up --remove-orphans` |
| Native compiled with Java 17 | `native-java17-latest` | `docker-compose -f deploy/docker-compose/native-java17.yml up --remove-orphans` |

These Docker Compose files are meant for standing up this application and the required database and Kafka broker only. If you want to stand up this application and its downstream services ([rest-villains](../rest-villains) and [rest-heroes](../rest-heroes)), pick one of the 4 versions from the table below and execute the appropriate docker compose command from the `quarkus-super-heroes/rest-fights` directory.

   > **NOTE**: You may see errors as the applications start up. This may happen if an application completes startup before one if its required services (i.e. database, kafka, etc). This is fine. Once everything completes startup things will work fine.

| Description                  | Image Tag              | Docker Compose Run Command                                                                     |
|------------------------------|------------------------|------------------------------------------------------------------------------------------------|
| JVM Java 11                  | `java11-latest`        | `docker-compose -f deploy/docker-compose/java11-all-downstream.yml up --remove-orphans`        |
| JVM Java 17                  | `java17-latest`        | `docker-compose -f deploy/docker-compose/java17-all-downstream.yml up --remove-orphans`        |
| Native compiled with Java 11 | `native-java11-latest` | `docker-compose -f deploy/docker-compose/native-java11-all-downstream.yml up --remove-orphans` |
| Native compiled with Java 17 | `native-java17-latest` | `docker-compose -f deploy/docker-compose/native-java17-all-downstream.yml up --remove-orphans` |

If you want to stand up the entire system, [follow these instructions](../README.md#running-locally-via-docker-compose).

Once started the application will be exposed at `http://localhost:8082`.

## Deploying to Kubernetes
Pre-built images for this application can be found at [`quay.io/quarkus-super-heroes/rest-fights`](https://quay.io/repository/quarkus-super-heroes/rest-fights?tab=tags).

Deployment descriptors for these images are provided in the [`deploy/k8s`](deploy/k8s) directory. There are versions for [OpenShift](https://www.openshift.com), [Minikube](https://quarkus.io/guides/deploying-to-kubernetes#deploying-to-minikube), and [Kubernetes](https://www.kubernetes.io).

Pick one of the 4 versions of the application from the table below and deploy the appropriate descriptor from the [`deploy/k8s` directory](deploy/k8s).

| Description                  | Image Tag              | OpenShift Descriptor                                                    | Minikube Descriptor                                                   | Kubernetes Descriptor                                                     |
|------------------------------|------------------------|-------------------------------------------------------------------------|-----------------------------------------------------------------------|---------------------------------------------------------------------------|
| JVM Java 11                  | `java11-latest`        | [`java11-openshift.yml`](deploy/k8s/java11-openshift.yml)               | [`java11-minikube.yml`](deploy/k8s/java11-minikube.yml)               | [`java11-kubernetes.yml`](deploy/k8s/java11-kubernetes.yml)               |
| JVM Java 17                  | `java17-latest`        | [`java17-openshift.yml`](deploy/k8s/java17-openshift.yml)               | [`java17-minikube.yml`](deploy/k8s/java17-minikube.yml)               | [`java17-kubernetes.yml`](deploy/k8s/java17-kubernetes.yml)               |
| Native compiled with Java 11 | `native-java11-latest` | [`native-java11-openshift.yml`](deploy/k8s/native-java11-openshift.yml) | [`native-java11-minikube.yml`](deploy/k8s/native-java11-minikube.yml) | [`native-java11-kubernetes.yml`](deploy/k8s/native-java11-kubernetes.yml) |
| Native compiled with Java 17 | `native-java17-latest` | [`native-java17-openshift.yml`](deploy/k8s/native-java17-openshift.yml) | [`native-java17-minikube.yml`](deploy/k8s/native-java17-minikube.yml) | [`native-java17-kubernetes.yml`](deploy/k8s/native-java17-kubernetes.yml) |

The application is exposed outside of the cluster on port `80`.

These are only the descriptors for this application and the required database and Kafka broker only. If you want to deploy this application and its downstream services ([rest-villains](../rest-villains) and [rest-heroes](../rest-heroes)), pick one of the 4 versions of the application from the table below and deploy the appropriate descriptor from the [`rest-fights/deploy/k8s` directory](deploy/k8s).

| Description                  | Image Tag              | OpenShift Descriptor                                                                                  | Minikube Descriptor                                                                                 | Kubernetes Descriptor                                                                                   |
|------------------------------|------------------------|-------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| JVM Java 11                  | `java11-latest`        | [`java11-openshift-all-downstream.yml`](deploy/k8s/java11-openshift-all-downstream.yml)               | [`java11-minikube-all-downstream.yml`](deploy/k8s/java11-minikube-all-downstream.yml)               | [`java11-kubernetes-all-downstream.yml`](deploy/k8s/java11-kubernetes-all-downstream.yml)               |
| JVM Java 17                  | `java17-latest`        | [`java17-openshift-all-downstream.yml`](deploy/k8s/java17-openshift-all-downstream.yml)               | [`java17-minikube-all-downstream.yml`](deploy/k8s/java17-minikube-all-downstream.yml)               | [`java17-kubernetes-all-downstream.yml`](deploy/k8s/java17-kubernetes-all-downstream.yml)               |
| Native compiled with Java 11 | `native-java11-latest` | [`native-java11-openshift-all-downstream.yml`](deploy/k8s/native-java11-openshift-all-downstream.yml) | [`native-java11-minikube-all-downstream.yml`](deploy/k8s/native-java11-minikube-all-downstream.yml) | [`native-java11-kubernetes-all-downstream.yml`](deploy/k8s/native-java11-kubernetes-all-downstream.yml) |
| Native compiled with Java 17 | `native-java17-latest` | [`native-java17-openshift-all-downstream.yml`](deploy/k8s/native-java17-openshift-all-downstream.yml) | [`native-java17-minikube-all-downstream.yml`](deploy/k8s/native-java17-minikube-all-downstream.yml) | [`native-java17-kubernetes-all-downstream.yml`](deploy/k8s/native-java17-kubernetes-all-downstream.yml) |

Each application is exposed outside of the cluster on port `80`.

If you want to deploy the entire system, [follow these instructions](../README.md#deploying-to-kubernetes).
