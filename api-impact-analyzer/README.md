API Impact Analyzer PoC

This Java 17 command-line application demonstrates deterministic endpoint impact analysis without MCP, Jira, Confluence, or an external LLM.

It performs four operations:

compares changed OpenAPI files with a Git base reference;

classifies added, removed, moved, and structurally changed operations;

discovers Quarkus REST consumers from @RegisterRestClient, Jakarta REST annotations, and application.properties;

produces a Markdown report and a JSON dependency index.

Build

On Linux, macOS, WSL, or Git Bash:

bash api-impact-analyzer/build.sh

The script uses only the JDK (javac, jar, and java) and downloads no Java dependencies.

Run locally

From the repository root:

java -jar api-impact-analyzer/target/api-impact-analyzer.jar \
--repo-root . \
--base-ref origin/main \
--report api-impact-report.md \
--index api-dependency-index.json

Use --fail-on-breaking when the process should return a non-zero exit code for a breaking API change.

Demonstration scenario

Change this path in rest-heroes/src/main/resources/openapi/openapi.yml:

/api/heroes/random:

to:

/api/heroes/random-one:

Keep operationId: getRandomHero, commit the change on a branch, and open a pull request. The analyzer recognizes the operation as moved and identifies rest-fights/.../HeroRestClient.java as a consumer of the previous path.

Current scope

The PoC intentionally favors traceability over unsupported guesses. It currently supports:

OpenAPI YAML/JSON files located in standard openapi or META-INF resource directories;

HTTP operations declared below the OpenAPI paths section;

declarative Quarkus clients using literal @Path values and @RegisterRestClient;

REST client URLs configured with Quarkus or MicroProfile properties.

The initial scanner does not yet resolve programmatic REST clients, computed annotation values, external component files, API gateways, runtime traces, Kafka consumers, or consumers in another Git repository. These are later enterprise extensions.

Schema changes made only in a reusable OpenAPI components definition are not yet followed through $ref references. For the first demonstration, use the endpoint path move described above.

If the setup pull request is the first pull request that introduces the workflow, GitHub may require the workflow to be approved or merged before it runs normally. In that case, merge the setup first and create a second branch containing only the demonstration endpoint change.
