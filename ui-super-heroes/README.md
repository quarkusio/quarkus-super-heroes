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

Environment variables can be injected into the build using the
[ngx-env](https://github.com/chihab/ngx-env) plugin. Remember, these are pulled
in at build time and are inserted as string literals in the resulting JS files.
Variables must start with the `NG_APP` prefix, e.g `NG_APP_MY_URL=http://localhost:1234`.

Production builds are served using a Node.js server. This server serves
the compiled Angular application and an `env.js` file. This `env.js` file is
generated at startup, and adds a `window.NG_CONFIG` property that the Angular
application can read from.

Currently the `env.js` will expose just the `API_BASE_URL` that's set at runtime.

```bash
API_BASE_URL=http://localhost:8282 ./package.sh
```

## Local Development

Use the following command:

```
API_BASE_URL=http://localhost:8181 HTTP_PORT=8080 npm run dev
```

This starts the Angular hot reloading server at http://localhost:4200, and the
Node.js server to supply the `env.js` file. The Angular server on port 4200
will proxy the request for `env.js` to the Node.js server on port 8080. The
Node.js server port can be changed by setting the `HTTP_PORT` variable.

## Running the Application
First you need to start up all of the downstream services ([Heroes Service](../rest-heroes), [Villains Service](../rest-villains), and [Fights Service](../rest-fights) - the [Event Statistics Service](../event-statistics) is optional).

Follow the steps above section, *Building the Application*. Start the service
using the command `HTTP_PORT=8080 API_BASE_URL=http://localhost:8181 npm start`.
Replace the `API_BASE_URL` with the appropriate Fights Service hostname and
port.
