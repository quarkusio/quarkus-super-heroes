# Superheroes Battle UI

## Table of Contents
- [Introduction](#introduction)
- [Running the Application](#running-the-application)
- [Running Locally via Docker Compose](#running-locally-via-docker-compose)

## Introduction
This is the main user interface for the application. The application is an Angular application but is packaged as a Quarkus application for convenience.

![ui-super-heroes](images/ui-super-heroes.png)

The main UI allows you to pick up one random Hero and Villain by clicking on "New Fighters." Then it’s just a matter of clicking on "Fight!" to get them to fight. The table at the bottom shows the list of the previous fights.

![main-ui](images/main-ui.png)

## Running the Application
First you need to start up all of the downstream services ([Heroes Service](../rest-heroes), [Villains Service](../rest-villains), and [Fights Service](../rest-fights) - the [Event Statistics Service](../event-statistics) is optional).

This application runs on port `8080`.

From the `quarkus-super-heroes/ui-super-heroes` directory, simply run `./mvnw quarkus:dev` to run [Quarkus Dev Mode](https://quarkus.io/guides/maven-tooling#dev-mode), or running `quarkus dev` using the [Quarkus CLI](https://quarkus.io/guides/cli-tooling). The application's UI will be exposed at http://localhost:8080 and the [Quarkus Dev UI](https://quarkus.io/guides/dev-ui) will be exposed at http://localhost:8080/q/dev.

## Running Locally via Docker Compose
Pre-built images for this application can be found at [`quay.io/quarkus-super-heroes/ui-super-heroes`](https://quay.io/repository/quarkus-super-heroes/ui-super-heroes?tab=tags). There are 4 versions of the application that can be run (be sure to run from the `quarkus-super-heroes/ui-super-heroes` directory):

| Description                  | Image                  | Docker Compose Run Command                                            |
|------------------------------|------------------------|-----------------------------------------------------------------------|
| JVM Java 11                  | `java11-latest`        | `docker-compose -f infrastructure/docker-compose.app-jvm11.yml up`    |
| JVM Java 17                  | `java17-latest`        | `docker-compose -f infrastructure/docker-compose.app-jvm17.yml up`    |
| Native compiled with Java 11 | `native-java11-latest` | `docker-compose -f infrastructure/docker-compose.app-native11.yml up` |
| Native compiled with Java 17 | `native-java17-latest` | `docker-compose -f infrastructure/docker-compose.app-native17.yml up` |

These Docker Compose files are meant for standing up this application only. If you want to stand up the entire system, [follow these instructions](../README.md#running-locally-via-docker-compose).

Once started the application will be exposed at `http://localhost:8080`.
