# Superheroes Battle UI

## Table of Contents
- [Introduction](#introduction)
- [Running the Application](#running-the-application)

## Introduction
This is the main user interface for the application. The application is an Angular application but is also packaged as a Quarkus application for convenience.

![ui-super-heroes](images/ui-super-heroes.png)

The main UI allows you to pick up one random Hero and Villain by clicking on "New Fighters." Then it’s just a matter of clicking on "Fight!" to get them to fight. The table at the bottom shows the list of the previous fights.

![main-ui](images/main-ui.png)

## Building the Application

Environment variables can be injected into the build. The
[ngx-env](https://github.com/chihab/ngx-env) plugin. Remember, these are pulled
in at build time and are inserted as string literals in the resulting JS files.
Variables must start with the `NG_APP` prefix, e.g `NG_APP_MY_URL=http://localhost:1234`.

A hot reloading Angular development server can be started like so:

```bash
NG_APP_MY_URL=http://localhost:8282 npm start
```

Production builds are placed into the *main/resources/META-INF* folder and
served directly by Quarkus. To build the application and copy the resulting
build into *main/resources/META-INF* run:

```bash
NG_APP_MY_URL=http://localhost:8282 ./package.sh
```

## Running the Application
First you need to start up all of the downstream services ([Heroes Service](../rest-heroes), [Villains Service](../rest-villains), and [Fights Service](../rest-fights) - the [Event Statistics Service](../event-statistics) is optional).

This application runs on port `8080`.

From the `quarkus-super-heroes/ui-super-heroes` directory, simply run `./mvnw quarkus:dev` to run [Quarkus Dev Mode](https://quarkus.io/guides/maven-tooling#dev-mode), or running `quarkus dev` using the [Quarkus CLI](https://quarkus.io/guides/cli-tooling). The application's UI will be exposed at http://localhost:8080 and the [Quarkus Dev UI](https://quarkus.io/guides/dev-ui) will be exposed at http://localhost:8080/q/dev.
