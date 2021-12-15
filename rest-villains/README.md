# Superheroes Villain Microservice

## Table of Contents
- [Introduction](#introduction)
- [Running the Application](#running-the-application)
- [Running Locally via Docker Compose](#running-locally-via-docker-compose)
- [Deploying to Kubernetes](#deploying-to-kubernetes)

## Introduction
This is the Villain REST API microservice. It is a classical HTTP microservice exposing CRUD operations on Villains. Villain information is stored in a PostgreSQL database. This service is implemented using [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive) with blocking endpoints and [Quarkus Hibernate ORM with Panache's active record pattern](https://quarkus.io/guides/hibernate-orm-panache).

![rest-villains](images/rest-villains.png)

The following table lists the available REST endpoints. The [OpenAPI document](openapi-schema.yml) for the REST endpoints is also available.

| Path | HTTP method | Response Status | Response Object | Description |
| ---- | ----------- | --------------- | --------------- | ----------- |
| `/api/villains` | `GET` | `200` | [`List<Villain>`](src/main/java/io/quarkus/sample/superheroes/villain/Villain.java) | All Villains. Empty array (`[]`) if none. |
| `/api/villains` | `POST` | `201` | | New Villain created. `Location` header contains URL to retrieve Villain |
| `/api/villains` | `POST` | `400` | | Invalid Villain passed in request body (or no request body found) |
| `/api/villains` | `DELETE` | `204` | | Deletes all Villains |
| `/api/villains/random` | `GET` | `200` | [`Villain`](src/main/java/io/quarkus/sample/superheroes/villain/Villain.java) | Random Villain |
| `/api/villains/random` | `GET` | `404` | | No Villain found |
| `/api/villains/{id}` | `GET` | `200` | [`Villain`](src/main/java/io/quarkus/sample/superheroes/villain/Villain.java) | Villain with id == `{id}` |
| `/api/villains/{id}` | `GET` | `404` | | No Villain with id == `{id}` found |
| `/api/villains/{id}` | `PUT` | `204` | | Completely replaces a Villain |
| `/api/villains/{id}` | `PUT` | `400` | | Invalid Villain passed in request body (or no request body found) |
| `/api/villains/{id}` | `PUT` | `404` | | No Villain with id == `{id}` found |
| `/api/villains/{id}` | `PATCH` | `200` | [`Villain`](src/main/java/io/quarkus/sample/superheroes/villain/Villain.java) | Partially updates a Villain. Returns the complete Villain. |
| `/api/villains/{id}` | `PATCH` | `400` | | Invalid Villain passed in request body (or no request body found) |
| `/api/villains/{id}` | `PATCH` | `404` | | No Villain with id == `{id}` found |
| `/api/villains/{id}` | `DELETE` | `204` | | Deletes Villain with id == `{id}` |
| `/api/villains/hello` | `GET` | `200` | `String` | Ping "hello" endpoint |

## Running the Application
The application runs on port `8084` (defined by `quarkus.http.port` in [`application.properties`](src/main/resources/application.properties)).

From the `quarkus-super-heroes/rest-villains` directory, simply run `./mvnw quarkus:dev` to run [Quarkus Dev Mode](https://quarkus.io/guides/maven-tooling#dev-mode), or running `quarkus dev` using the [Quarkus CLI](https://quarkus.io/guides/cli-tooling). The application will be exposed at http://localhost:8084 and the [Quarkus Dev UI](https://quarkus.io/guides/dev-ui) will be exposed at http://localhost:8084/q/dev.

**NOTE:** Running the application outside of Quarkus dev mode requires standing up a PostgreSQL instance and binding it to the app. By default, the application is configured with the following:

| Description       | Environment Variable          | Java Property                 | Value                                                |
|-------------------|-------------------------------|-------------------------------|------------------------------------------------------|
| Database URL      | `QUARKUS_DATASOURCE_JDBC_URL` | `quarkus.datasource.jdbc.url` | `jdbc:postgresql://localhost:5432/villains_database` |
| Database username | `QUARKUS_DATASOURCE_USERNAME` | `quarkus.datasource.username` | `superbad`                                           |
| Database password | `QUARKUS_DATASOURCE_PASSWORD` | `quarkus.datasource.password` | `superbad`                                           |

## Running Locally via Docker Compose
Pre-built images for this application can be found at [`quay.io/quarkus-super-heroes/rest-villains`](https://quay.io/repository/quarkus-super-heroes/rest-villains?tab=tags). 

Pick one of the 4 versions of the application from the table below and execute the appropriate docker compose command from the `quarkus-super-heroes/rest-villains` directory.

   > **NOTE**: You may see errors as the applications start up. This may happen if an application completes startup before one if its required services (i.e. database, kafka, etc). This is fine. Once everything completes startup things will work fine.

| Description                  | Image Tag              | Docker Compose Run Command                                                      |
|------------------------------|------------------------|---------------------------------------------------------------------------------|
| JVM Java 11                  | `java11-latest`        | `docker-compose -f deploy/docker-compose/java11.yml up --remove-orphans`        |
| JVM Java 17                  | `java17-latest`        | `docker-compose -f deploy/docker-compose/java17.yml up --remove-orphans`        |
| Native compiled with Java 11 | `native-java11-latest` | `docker-compose -f deploy/docker-compose/native-java11.yml up --remove-orphans` |
| Native compiled with Java 17 | `native-java17-latest` | `docker-compose -f deploy/docker-compose/native-java17.yml up --remove-orphans` |

These Docker Compose files are meant for standing up this application and the required database only. If you want to stand up the entire system, [follow these instructions](../README.md#running-locally-via-docker-compose).

Once started the application will be exposed at `http://localhost:8084`.

## Deploying to Kubernetes
Pre-built images for this application can be found at [`quay.io/quarkus-super-heroes/rest-villains`](https://quay.io/repository/quarkus-super-heroes/rest-villains?tab=tags).

Deployment descriptors for these images are provided in the [`deploy/k8s`](deploy/k8s) directory. There are versions for [OpenShift](https://www.openshift.com), [Minikube](https://minikube.sigs.k8s.io), and [Kubernetes](https://www.kubernetes.io).

Pick one of the 4 versions of the application from the table below and deploy the appropriate descriptor from the [`deploy/k8s` directory](deploy/k8s).

| Description                  | Image Tag              | OpenShift Descriptor                                                    | Minikube Descriptor                                                   | Kubernetes Descriptor                                                     |
|------------------------------|------------------------|-------------------------------------------------------------------------|-----------------------------------------------------------------------|---------------------------------------------------------------------------|
| JVM Java 11                  | `java11-latest`        | [`java11-openshift.yml`](deploy/k8s/java11-openshift.yml)               | [`java11-minikube.yml`](deploy/k8s/java11-minikube.yml)               | [`java11-kubernetes.yml`](deploy/k8s/java11-kubernetes.yml)               |
| JVM Java 17                  | `java17-latest`        | [`java17-openshift.yml`](deploy/k8s/java17-openshift.yml)               | [`java17-minikube.yml`](deploy/k8s/java17-minikube.yml)               | [`java17-kubernetes.yml`](deploy/k8s/java17-kubernetes.yml)               |
| Native compiled with Java 11 | `native-java11-latest` | [`native-java11-openshift.yml`](deploy/k8s/native-java11-openshift.yml) | [`native-java11-minikube.yml`](deploy/k8s/native-java11-minikube.yml) | [`native-java11-kubernetes.yml`](deploy/k8s/native-java11-kubernetes.yml) |
| Native compiled with Java 17 | `native-java17-latest` | [`native-java17-openshift.yml`](deploy/k8s/native-java17-openshift.yml) | [`native-java17-minikube.yml`](deploy/k8s/native-java17-minikube.yml) | [`native-java17-kubernetes.yml`](deploy/k8s/native-java17-kubernetes.yml) |

The application is exposed outside of the cluster on port `80`.

These are only the descriptors for this application and the required database only. If you want to deploy the entire system, [follow these instructions](../README.md#deploying-to-kubernetes).
