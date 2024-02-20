package io.quarkus.sample.superheroes.fight;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.util.Map;

import org.wiremock.grpc.GrpcExtensionFactory;
import org.wiremock.grpc.dsl.WireMockGrpcService;

import io.quarkus.sample.superheroes.location.grpc.LocationsGrpc;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * Quarkus {@link QuarkusTestResourceLifecycleManager} wrapping a {@link WireMockServer}, {@link WireMock}, or {@link WireMockGrpcService}, while binding its base url to the locations services, and exposing it to tests that want to inject it via {@link InjectGrpcWireMock}.
 *
 * @see InjectGrpcWireMock
 */
public class LocationsWiremockGrpcServerResource implements QuarkusTestResourceLifecycleManager {
  private final WireMockServer wireMockServer =
    new WireMockServer(
      wireMockConfig()
        .dynamicPort()
        .withRootDirectory("target/test-classes/wiremock")
        .extensions(new GrpcExtensionFactory())
    );

  @Override
  public Map<String, String> start() {
    this.wireMockServer.start();

    var port = getPort();

    return Map.of(
      "quarkus.grpc.clients.locations.host", "localhost",
      "quarkus.grpc.clients.locations.port", String.valueOf(port),
      "quarkus.grpc.clients.locations.test-port", String.valueOf(port)
    );
  }

  @Override
  public void stop() {
    this.wireMockServer.stop();
  }

  @Override
  public void inject(TestInjector testInjector) {
    var wireMock = new WireMock(getPort());

    testInjector.injectIntoFields(
      wireMock,
      new AnnotatedAndMatchesType(InjectGrpcWireMock.class, WireMock.class)
    );

    testInjector.injectIntoFields(
      new WireMockGrpcService(wireMock, LocationsGrpc.SERVICE_NAME),
      new AnnotatedAndMatchesType(InjectGrpcWireMock.class, WireMockGrpcService.class)
    );

    testInjector.injectIntoFields(
      this.wireMockServer,
      new AnnotatedAndMatchesType(InjectGrpcWireMock.class, WireMockServer.class)
    );
  }

  private int getPort() {
    return this.wireMockServer.isHttpsEnabled() ?
           this.wireMockServer.httpsPort() :
           this.wireMockServer.port();
  }
}
