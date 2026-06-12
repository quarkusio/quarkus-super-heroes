---
layout: page
title: Quarkus Super Heroes
description: A sample application demonstrating Quarkus features and best practices through a microservices-based superhero battle system.
name: Quarkus Super Heroes
simple-name: Quarkus Super Heroes
---

A sample application demonstrating Quarkus features and best practices. Superheroes fight against supervillains across multiple microservices communicating via REST, gRPC, and Kafka.

## Architecture

The application consists of several microservices, communicating either synchronously via REST or asynchronously using Kafka. The base JVM version for all the applications is Java 21.

![Superheroes architecture diagram]({site.image('application-architecture.png')})

## Services

| Service | Port | Type | Database | Language |
|---------|------|------|----------|----------|
| [Hero REST API]({site.url('/rest-heroes')}) | 8083 | REST (reactive) | PostgreSQL | Java |
| [Villain REST API]({site.url('/rest-villains')}) | 8084 | REST (blocking) | PostgreSQL | Java |
| [Fight REST API]({site.url('/rest-fights')}) | 8082 | REST (reactive) | MongoDB | Java |
| [Narration REST API]({site.url('/rest-narration')}) | 8087 | REST (blocking) | — (OpenAI) | Java |
| [Location gRPC API]({site.url('/grpc-locations')}) | 8089 | gRPC (blocking) | MariaDB | Kotlin |
| [Event Statistics]({site.url('/event-statistics')}) | 8085 | Kafka consumer + WebSocket | — | Java |
| [Battle UI]({site.url('/ui-super-heroes')}) | 8080 | React (via Quinoa) | — | TypeScript |

## Demo

<video src="https://github.com/quarkusio/quarkus-super-heroes/assets/363447/55a0a63f-c636-4719-9a7b-9a9034116e77" controls width="100%"></video>

## Communication Flow

- **UI** → rest-fights (REST)
- **rest-fights** → rest-heroes, rest-villains, rest-narration (REST via SmallRye Stork)
- **rest-fights** → grpc-locations (gRPC)
- **rest-fights** → Kafka (Avro messages via Apicurio Registry)
- **event-statistics** ← Kafka consumer

## Observability

All services export traces, metrics, and logs via [OpenTelemetry](https://opentelemetry.io) (OTLP) to the [Grafana LGTM stack](https://github.com/grafana/docker-otel-lgtm) (Loki, Grafana, Tempo, Prometheus).

## Getting Started

- [Running Locally via Docker Compose]({site.url('/running-locally')})
- [Deploying to Kubernetes]({site.url('/deploying')})
- [CI/CD Automation]({site.url('/automation')})
