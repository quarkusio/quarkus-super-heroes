API Impact Analysis PoC

Goal

When a pull request modifies a REST endpoint, automatically identify known consumer services, show the supporting code evidence, and bring the result to the developer's attention in the pull request. Copilot explains the deterministic report; it is not the source of truth for dependency detection.

Demonstrated relationship

rest-heroes
provides GET /api/heroes/random
↓
rest-fights
consumes it through HeroRestClient.findRandomHero()

The provider contract is stored in rest-heroes/src/main/resources/openapi/openapi.yml. The consumer evidence is stored in rest-fights/src/main/java/io/quarkus/sample/superheroes/fight/client/HeroRestClient.java, and its target is configured in rest-fights/src/main/resources/application.properties.

Pull request flow

A developer changes an OpenAPI contract in a branch.

.github/workflows/api-impact.yml runs on the pull request.

The Java analyzer compares the branch contract with the pull request base SHA.

It scans the repository for Quarkus declarative REST clients.

It matches old method/path pairs with consumer calls.

It publishes api-impact-report.md in the Actions summary and as a PR comment.

A breaking change fails the status check.

Copilot can use the report and .github/copilot-instructions.md to explain the required adaptation.

Why this is automatic but not fully autonomous

Detection, reporting, notification, and the status check are automatic. Human review remains necessary for low-confidence or currently unsupported calls because dynamic URLs and runtime routing cannot always be proven from source code alone.

Future enterprise extensions

Run one scanner in every repository and publish snapshots to a central service catalog.

Use a GitHub App for cross-repository issues, review requests, and workflow dispatches.

Add CODEOWNERS-based team notification.

Add generated-client and Maven package detection.

Add API gateway and runtime tracing evidence.

Expose the dependency index to Copilot through MCP.

Enrich the report with Jira and Confluence only after the technical dependency is established.
