package io.quarkus.sample.superheroes.impact;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class AnalyzerSelfTest {

  private AnalyzerSelfTest() {
  }

  public static void main(String[] args) throws Exception {
    readsOpenApiOperations();
    detectsStructuralContractChanges();
    discoversQuarkusRestClients();
    detectsMovedEndpointsAndMatchesConsumers();
    System.out.println("All API impact analyzer self-tests passed.");
  }

  private static void readsOpenApiOperations() {
    OpenApiContractReader reader = new OpenApiContractReader();
    List<Models.ApiOperation> operations = reader.read(
      contract("/api/heroes/random", "string"),
      "rest-heroes",
      "openapi.yml");
    check(operations.size() == 1, "Expected one operation");
    check("GET /api/heroes/random".equals(operations.get(0).key()), "Unexpected operation key");
    check("getRandomHero".equals(operations.get(0).operationId()), "Unexpected operationId");
  }

  private static void discoversQuarkusRestClients() throws Exception {
    Path repository = Files.createTempDirectory("api-impact-test-");
    Path javaFile = repository.resolve("rest-fights/src/main/java/example/HeroClient.java");
    Files.createDirectories(javaFile.getParent());
    Files.writeString(javaFile, """
        package example;
        import jakarta.ws.rs.GET;
        import jakarta.ws.rs.Path;
        import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
        @Path("/api/heroes")
        @RegisterRestClient(configKey = "hero-client")
        interface HeroClient {
          @GET
          @Path("/random")
          String random();
        }
        """);
    Path properties = repository.resolve("rest-fights/src/main/resources/application.properties");
    Files.createDirectories(properties.getParent());
    Files.writeString(properties, "quarkus.rest-client.hero-client.url=stork://hero-service\n");

    List<Models.ConsumerCall> calls = new RestClientScanner().scan(repository);
    check(calls.size() == 1, "Expected one REST call");
    check("rest-fights".equals(calls.get(0).consumer()), "Unexpected consumer");
    check("GET /api/heroes/random".equals(calls.get(0).key()), "Unexpected REST call");
    check("stork://hero-service".equals(calls.get(0).targetUrl()), "Unexpected target URL");
  }

  private static void detectsStructuralContractChanges() {
    OpenApiContractReader reader = new OpenApiContractReader();
    List<Models.ApiOperation> before = reader.read(contract("/api/heroes/random", "string"), "rest-heroes", "openapi.yml");
    List<Models.ApiOperation> after = reader.read(contract("/api/heroes/random", "integer"), "rest-heroes", "openapi.yml");

    List<Models.ApiChange> changes = new GitContractDiff(Path.of("."), "HEAD", reader).compare(before, after);

    check(changes.size() == 1, "Expected one structural contract change");
    check(changes.get(0).type() == Models.ChangeType.CONTRACT_CHANGED, "Expected CONTRACT_CHANGED");
  }

  private static void detectsMovedEndpointsAndMatchesConsumers() {
    OpenApiContractReader reader = new OpenApiContractReader();
    List<Models.ApiOperation> before = reader.read(contract("/api/heroes/random", "string"), "rest-heroes", "openapi.yml");
    List<Models.ApiOperation> after = reader.read(contract("/api/heroes/random-one", "string"), "rest-heroes", "openapi.yml");
    List<Models.ApiChange> changes = new GitContractDiff(Path.of("."), "HEAD", reader).compare(before, after);
    check(changes.size() == 1 && changes.get(0).type() == Models.ChangeType.MOVED, "Expected a moved endpoint");

    Models.ConsumerCall consumer = new Models.ConsumerCall(
      "rest-fights", "HeroClient.java", "HeroClient", "random", "hero-client",
      "stork://hero-service", "GET", "/api/heroes/random", "HIGH");
    List<Models.Impact> impacts = ApiImpactAnalyzer.matchImpacts(changes, List.of(consumer));
    check(impacts.size() == 1, "Expected the rest-fights consumer to be impacted");
  }

  private static byte[] contract(String path, String responseType) {
    return ("""
        openapi: 3.1.0
        info:
          title: Hero API
          version: "1"
        paths:
          %s:
            get:
              operationId: getRandomHero
              responses:
                "200":
                  description: OK
                  content:
                    application/json:
                      schema:
                        type: %s
        """.formatted(path, responseType)).getBytes(StandardCharsets.UTF_8);
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
