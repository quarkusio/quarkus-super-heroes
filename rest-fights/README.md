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

## Introduction
This is the Fight REST API microservice. It is a reactive HTTP microservice exposing an API for performing fights between [Heroes](../rest-heroes) and [Villains](../rest-villains). Each fight is persisted into a PostgreSQL database and can be retrieved via the REST API. This service is implemented using [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive) with reactive endpoints and [Quarkus Hibernate Reactive with Panache's active record pattern](http://quarkus.io/guides/hibernate-reactive-panache).

![rest-fights](images/rest-fights.png)

The following table lists the available REST endpoints. The [OpenAPI document](openapi-schema.yml) for the REST endpoints is also available.

| Path | HTTP method | Response Status | Response Object | Description |
| ---- | ----------- | --------------- | --------------- | ----------- |
| `/api/fights` | `GET` | `200` | [`List<Fight>`](src/main/java/io/quarkus/sample/superheroes/fight/Fight.java) | All Fights. Empty array (`[]`) if none. |
| `/api/fights` | `POST` | `200` | [`Fight`](src/main/java/io/quarkus/sample/superheroes/fight/Fight.java) | Performs a fight. |
| `/api/fights` | `POST` | `400` | | Invalid [`Fighters`](src/main/java/io/quarkus/sample/superheroes/fight/Fighters.java) passed in request body (or no request body found) |
| `/api/fights/randomfighters` | `GET` | `200` | [`Fighters`](src/main/java/io/quarkus/sample/superheroes/fight/Fighters.java) | Finds random fighters |
| `/api/fights/{id}` | `GET` | `200` | [`Fight`](src/main/java/io/quarkus/sample/superheroes/fight/Fight.java) | Fight with id == `{id}` |
| `/api/fights/{id}` | `GET` | `404` | | No Fight with id == `{id}` found |
| `/api/fights/hello` | `GET` | `200` | `String` | Ping "hello" endpoint |

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
- The [integration test suite](src/test/java/io/quarkus/sample/superheroes/fight/rest/FightResourceIT.java) uses a [Kafka resource](https://quarkus.io/guides/kafka#starting-kafka-in-a-test-resource) (see [`KafkaBrokerResource`](src/test/java/io/quarkus/sample/superheroes/fight/KafkaBrokerResource.java)) to stand up a Kafka instance so \messages placed onto the Kafka broker by the application can be verified.
