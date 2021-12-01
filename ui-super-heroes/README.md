# Superheroes Battle UI

## Table of Contents
- [Introduction](#introduction)
- [Running the Application](#running-the-application)

## Introduction
This is the main user interface for the application. The application is an Angular application but is also packaged as a Quarkus application for convenience.

![ui-super-heroes](images/ui-super-heroes.png)

The main UI allows you to pick up one random Hero and Villain by clicking on "New Fighters." Then it’s just a matter of clicking on "Fight!" to get them to fight. The table at the bottom shows the list of the previous fights.

![main-ui](images/main-ui.png)

## Running the Application
First you need to start up all of the downstream services ([Heroes Service](../rest-heroes), [Villains Service](../rest-villains), and [Fights Service](../rest-fights) - the [Event Statistics Service](../event-statistics) is optional).

This application runs on port `8080`.

From the `quarkus-super-heroes/ui-super-heroes` directory, simply run `./mvnw quarkus:dev` to run [Quarkus Dev Mode](https://quarkus.io/guides/maven-tooling#dev-mode), or running `quarkus dev` using the [Quarkus CLI](https://quarkus.io/guides/cli-tooling). The application's UI will be exposed at http://localhost:8080 and the [Quarkus Dev UI](https://quarkus.io/guides/dev-ui) will be exposed at http://localhost:8080/q/dev.
