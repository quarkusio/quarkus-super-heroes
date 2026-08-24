# API impact analysis

The repository uses a deterministic Java analyzer for API dependency detection. Copilot explains its output; it must not invent service dependencies.

## Sources of truth

- `api-impact-report.md`: human-readable contract changes, evidence, confidence, and affected consumers.
- `api-dependency-index.json`: machine-readable dependencies and impacts.
- `.github/service-owners.json`: GitHub users or teams responsible for each service.
- OpenAPI files: provider contracts.
- Quarkus REST client declarations and configuration: consumer evidence.

## When helping with an API pull request

1. Read the generated report before drawing conclusions.
2. State the changed HTTP method and path, provider, impacted consumer, and source-file evidence.
3. Name only owners found in `.github/service-owners.json`.
4. Separate confirmed dependencies from unsupported or dynamic cases requiring human verification.
5. Suggest client and contract-test updates for breaking changes.
6. If the report is missing, tell the developer to open or update the pull request so the `API impact analysis` workflow runs.

## Demonstrated dependency

`rest-heroes` provides `GET /api/heroes/random`; `rest-fights` consumes it through `HeroRestClient.findRandomHero()`.
