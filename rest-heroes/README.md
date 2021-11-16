# Superheroes Hero Microservice

This is the Hero REST API microservice. It is a reactive HTTP microservice exposing CRUD operations on Heroes. Hero information is stored in a PostgreSQL database. This service is implemented using [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive) with reactive endpoints and [Quarkus Hibernate Reactive with Panache's repository pattern](http://quarkus.io/guides/hibernate-reactive-panache).

![rest-heroes](images/rest-heroes.png)

The following table lists the available REST endpoints. The [OpenAPI document](openapi-schema.yml) for the REST endpoints is also available.

| Path | HTTP method | Response Status | Response Object | Description |
| ---- | ----------- | --------------- | --------------- | ----------- |
| `/api/heroes` | `GET` | `200` | [`List<Hero>`](src/main/java/io/quarkus/sample/superheroes/hero/Hero.java) | All Heroes, assuming there is at least 1 |
| `/api/heroes` | `GET` | `204` | | No Heroes |
| `/api/heroes` | `POST` | `201` | | New Hero created. `Location` header contains URL to retrieve Hero |
| `/api/heroes` | `POST` | `400` | | Invalid Hero passed in request body (or no request body found) |
| `/api/heroes` | `DELETE` | `204` | | Deletes all Heroes |
| `/api/heroes/random` | `GET` | `200` | [`Hero`](src/main/java/io/quarkus/sample/superheroes/hero/Hero.java) | Random Hero |
| `/api/heroes/random` | `GET` | `404` | | No Hero found |
| `/api/heroes/{id}` | `GET` | `200` | [`Hero`](src/main/java/io/quarkus/sample/superheroes/hero/Hero.java) | Hero with id == `{id}` |
| `/api/heroes/{id}` | `GET` | `404` | | No Hero with id == `{id}` found |
| `/api/heroes/{id}` | `PUT` | `204` | | Completely replaces a Hero |
| `/api/heroes/{id}` | `PUT` | `400` | | Invalid Hero passed in request body (or no request body found) |
| `/api/heroes/{id}` | `PUT` | `404` | | No Hero with id == `{id}` found |
| `/api/heroes/{id}` | `PATCH` | `200` | [`Hero`](src/main/java/io/quarkus/sample/superheroes/hero/Hero.java) | Partially updates a Hero. Returns the complete Hero. |
| `/api/heroes/{id}` | `PATCH` | `400` | | Invalid Hero passed in request body (or no request body found) |
| `/api/heroes/{id}` | `PATCH` | `404` | | No Hero with id == `{id}` found |
| `/api/heroes/{id}` | `DELETE` | `204` | | Deletes Hero with id == `{id}` |
| `/api/heroes/hello` | `GET` | `200` | `String` | Ping "hello" endpoint |
