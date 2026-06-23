---
title: Running Locally via Docker Compose
description: How to run the Quarkus Super Heroes system locally using Docker Compose.
layout: page
content-toc: true
---

Pre-built container images for all Quarkus Super Heroes services are available at [`quay.io/quarkus-super-heroes`](https://quay.io/organization/quarkus-super-heroes). You can run the entire system or individual services using Docker Compose, or start any service in Quarkus Dev Mode for local development.

## Running the Full System

Pick either the JVM or Native version from the table below and run the corresponding `docker compose` command **from the repository root directory**.

| Description | Docker Compose Command |
|-------------|------------------------|
| JVM Java 25 | `docker compose -f deploy/docker-compose/java25.yml up --remove-orphans` |
| Native      | `docker compose -f deploy/docker-compose/native.yml up --remove-orphans` |

To include the **monitoring stack** (Grafana, Loki, Tempo, Prometheus), append the monitoring compose file:

```bash
docker compose -f deploy/docker-compose/java25.yml -f deploy/docker-compose/monitoring.yml up --remove-orphans
```

**Note:** You may see errors as the applications start up. This can happen if an application completes startup before one of its required services (database, Kafka, etc.) is ready. This is normal. Once everything finishes starting up, the system will work fine.

**Tip:** Older versions of Docker used a standalone `docker-compose` binary. Modern Docker includes compose as a sub-command (`docker compose`). If `docker compose` does not work for you, try `docker-compose` instead.

Once running, the following services are available:

| Service | URL |
|---------|-----|
| Super Hero Battle UI | [http://localhost:8080](http://localhost:8080) |
| Event Statistics UI | [http://localhost:8085](http://localhost:8085) |
| Apicurio Schema Registry | [http://localhost:8086](http://localhost:8086) |
| Grafana (if monitoring enabled) | [http://localhost:3000](http://localhost:3000) |

## Running Individual Services

Each service can be run standalone with its own Docker Compose file, which starts only that service and its required infrastructure (database, etc.). Run the appropriate command from the **service directory**.

| Service | Port | Docker Compose Command (JVM) | Docker Compose Command (Native) |
|---------|:----:|------------------------------|----------------------------------|
| [Hero REST API]({site.url('/rest-heroes')}) | 8083 | `docker compose -f deploy/docker-compose/java25.yml up --remove-orphans` | `docker compose -f deploy/docker-compose/native.yml up --remove-orphans` |
| [Villain REST API]({site.url('/rest-villains')}) | 8084 | `docker compose -f deploy/docker-compose/java25.yml up --remove-orphans` | `docker compose -f deploy/docker-compose/native.yml up --remove-orphans` |
| [Fight REST API]({site.url('/rest-fights')}) | 8082 | `docker compose -f deploy/docker-compose/java25.yml up --remove-orphans` | `docker compose -f deploy/docker-compose/native.yml up --remove-orphans` |
| [Narration REST API]({site.url('/rest-narration')}) | 8087 | `docker compose -f deploy/docker-compose/java25.yml up --remove-orphans` | `docker compose -f deploy/docker-compose/native.yml up --remove-orphans` |
| [Event Statistics]({site.url('/event-statistics')}) | 8085 | `docker compose -f deploy/docker-compose/java25.yml up --remove-orphans` | `docker compose -f deploy/docker-compose/native.yml up --remove-orphans` |
| [Location gRPC API]({site.url('/grpc-locations')}) | 8089 | `docker compose -f deploy/docker-compose/java25.yml up --remove-orphans` | `docker compose -f deploy/docker-compose/native.yml up --remove-orphans` |
| [Battle UI]({site.url('/ui-super-heroes')}) | 8080 | `docker compose -f deploy/docker-compose/java25.yml up --remove-orphans` | `docker compose -f deploy/docker-compose/native.yml up --remove-orphans` |

These per-service compose files start only the selected service and its direct infrastructure dependencies. They do not start the other application services.

## Running in Dev Mode

Each service supports [Quarkus Dev Mode](https://quarkus.io/guides/maven-tooling#dev-mode), which provides live coding with automatic hot reload. From any service directory, run:

```bash
./mvnw quarkus:dev
```

[Quarkus Dev Services](https://quarkus.io/guides/dev-services) will automatically start all required infrastructure (databases, Kafka, etc.) as containers. **Docker or Podman must be running** for Dev Services to work.

The [Quarkus Dev UI](https://quarkus.io/guides/dev-ui) is available at `http://localhost:<port>/q/dev` for each service.
