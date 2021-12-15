# Superheroes Battle UI

## Table of Contents
- [Introduction](#introduction)
- [Building the Application](#building-the-application)
- [Local Development](#local-development)
- [Running the Application](#running-the-application)
- [Running Locally via Docker Compose](#running-locally-via-docker-compose)

## Introduction
This is the main user interface for the application. The application is an Angular application served via Node.js.

![ui-super-heroes](images/ui-super-heroes.png)

The main UI allows you to pick up one random Hero and Villain by clicking on "New Fighters." Then it’s just a matter of clicking on "Fight!" to get them to fight. The table at the bottom shows the list of the previous fights.

![main-ui](images/main-ui.png)

## Building the Application
Environment variables can be injected into the build using the [ngx-env](https://github.com/chihab/ngx-env) plugin. Remember, these are pulled in at build time and are inserted as string literals in the resulting JS files.

Variables must start with the `NG_APP` prefix, e.g `NG_APP_MY_URL=http://localhost:1234`.

Production builds are served using a Node.js server. This server serves the compiled Angular application and an `env.js` file. This `env.js` file is generated at startup, and adds a `window.NG_CONFIG` property that the Angular application can read from.

Currently, the `env.js` will expose just the `API_BASE_URL` that's set at runtime. This will control the base URL to connect to the [fights](../rest-fights) service. The default if unset is http://localhost:8082.

You also need to make sure the angular CLI is installed (`npm install @angular/cli@12.2.8`).

```bash
npm run build
```

## Local Development
Use the following command:

```shell
npm run dev
```

This starts the Angular hot reloading server at http://localhost:4200, and the Node.js server to supply the `env.js` file. The Angular server on port 4200 will proxy the request for `env.js` to the Node.js server on port 8080.

The Node.js server port can be changed by setting the `HTTP_PORT` variable. The `ng.proxy.config.json` file will need to be updated with the new Node.js server port number if you deviate from 8080.

## Running the Application
1. First you need to start up all of the downstream services ([Heroes Service](../rest-heroes), [Villains Service](../rest-villains), and [Fights Service](../rest-fights). 
    - The [Event Statistics Service](../event-statistics) is optional.
2. Follow the steps above section, *Building the Application*.
3. Start the service using the command `npm start`.
    - Replace the `API_BASE_URL` with the appropriate [Fights Service](../rest-fights) hostname and port.
       > The default is http://localhost:8082, which is the default for the fights service.
    - There is also a container image available that you can use instead:

       ```bash
       docker run -p 8080:8080 quay.io/quarkus-super-heroes/ui-super-heroes:latest
       ```

## Running Locally via Docker Compose
Pre-built images for this application can be found at [`quay.io/quarkus-super-heroes/ui-super-heroes`](https://quay.io/repository/quarkus-super-heroes/ui-super-heroes?tab=tags). 

The application can be started outside of docker compose simply with `docker run -p 8080:8080 quay.io/quarkus-super-heroes/ui-super-heroes:latest`.

If you want to use docker compose, from the `quarkus-super-heroes/ui-super-heroes` directory run:

```bash
docker-compose -f deploy/docker-compose/app.yml up
```

If you want to stand up the entire system, [follow these instructions](../README.md#running-locally-via-docker-compose).

Once started the application will be exposed at `http://localhost:8080`.
