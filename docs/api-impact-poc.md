# API Impact Analysis PoC

## Goal

When a pull request changes a REST contract, automatically detect known consumer services, publish evidence, notify their owners, request reviewers when possible, and stop a breaking change from being merged unnoticed.

The dependency result comes from the deterministic Java analyzer. Copilot can explain the report, but it is not the source of truth.

## Demonstrated dependency

`rest-heroes` provides `GET /api/heroes/random`; `rest-fights` consumes it through `HeroRestClient.findRandomHero()`.

- Provider contract: `rest-heroes/src/main/resources/openapi/openapi.yml`
- Consumer evidence: `rest-fights/src/main/java/io/quarkus/sample/superheroes/fight/client/HeroRestClient.java`
- Consumer configuration: `rest-fights/src/main/resources/application.properties`

## Automated pull request flow

1. A developer changes an OpenAPI contract or Java endpoint/client and pushes a pull request update.
2. `.github/workflows/api-impact.yml` compares the branch with the pull request base SHA.
3. The Java analyzer scans OpenAPI operations and declarative Quarkus REST clients.
4. It writes `api-impact-report.md` and `api-dependency-index.json`.
5. GitHub Actions adds the Markdown report to the run summary and creates or updates one PR comment.
6. The workflow maps impacted consumer services to `.github/service-owners.json`, mentions those owners, and tries to request them as reviewers.
7. A breaking API change fails the compatibility check. A developer reviews and updates the affected client or explicitly decides how to handle the change.

## Configure service owners

Edit `.github/service-owners.json` using GitHub mentions:

```json
{
  "rest-fights": ["@my-company/fights-team", "@alice"],
  "rest-heroes": ["@my-company/heroes-team"]
}
```

`@repository-owner` is a demo placeholder resolved to the current repository owner. Replace it with real users or organization teams before enterprise use. A team review request works when the repository belongs to the same organization and the workflow token has permission; the PR comment remains the fallback notification.

## Current limits and enterprise extensions

The scanner currently proves literal declarative REST-client relationships inside this repository. Dynamic URLs, programmatic clients, external repositories, gateway routing, and runtime calls still require additional evidence.

Later, publish dependency snapshots to a central catalog, use a GitHub App for cross-repository notifications, and expose the index through MCP for Jira, Confluence, and Copilot queries.
