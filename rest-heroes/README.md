# Superheroes Hero Microservice

> **Full documentation: [https://quarkus.io/quarkus-super-heroes/rest-heroes](https://quarkus.io/quarkus-super-heroes/rest-heroes)**

## Introduction

This is the Hero REST API microservice. It is a reactive HTTP microservice exposing CRUD operations on Heroes. Hero information is stored in a PostgreSQL database. This service is implemented using [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive) with reactive endpoints and [Quarkus Hibernate Reactive with Panache's repository pattern](https://quarkus.io/guides/hibernate-reactive-panache#solution-2-using-the-repository-pattern).

This service uses a **contract-first** approach: the REST API interface is generated at build time from the OpenAPI specification ([`src/main/resources/openapi/openapi.yml`](src/main/resources/openapi/openapi.yml)) using the [Quarkiverse OpenAPI Generator Server extension](https://docs.quarkiverse.io/quarkus-openapi-generator/dev/server.html).

Additionally, this application favors constructor injection of beans over field injection (i.e. `@Inject` annotation).

![rest-heroes](images/rest-heroes.png)

## Running the Application

The application runs on port `8083` (defined by `quarkus.http.port` in [`application.yml`](src/main/resources/application.yml)).

From the `quarkus-super-heroes/rest-heroes` directory, simply run `./mvnw quarkus:dev` to run [Quarkus Dev Mode](https://quarkus.io/guides/maven-tooling#dev-mode). The application will be exposed at http://localhost:8083 and the [Quarkus Dev UI](https://quarkus.io/guides/dev-ui) will be exposed at http://localhost:8083/q/dev.

![heroes-ui](images/heroes-ui.png)







# 1. Pornești de la ultima versiune
git switch main
git pull origin main

# 2. Creezi un branch nou
git switch -c change/random-hero-endpoint

# 3. Modifici manual endpointul în openapi.yml

# 4. Compilezi analizatorul
.\mvnw.cmd -f .\api-impact-analyzer\pom.xml clean package

# 5. Rulezi analiza local
git fetch origin
java -jar .\api-impact-analyzer\target\api-impact-analyzer.jar --repo-root . --base-ref origin/main --report api-impact-report.md --index api-dependency-index.json

# 6. Vezi raportul local
Get-Content .\api-impact-report.md

# 7. Verifici fișierele modificate
git status --short

# 8. Adaugi fișierul modificat
git add rest-heroes/src/main/resources/openapi/openapi.yml

# Dacă ai modificat și ownerii:
git add .github/service-owners.json

# 9. Creezi commitul
git commit -m "Change random hero endpoint"

# 10. Trimiți branch-ul pe GitHub
git push -u origin change/random-hero-endpoint
