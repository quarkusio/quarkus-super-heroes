package io.quarkus.sample.superheroes.fight;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Quarkus {@link QuarkusTestResourceLifecycleManager} wrapping a {@link WireMockServer}, binding its base url to both the heroes and villains services, and exposing it to tests that want to inject it via {@link InjectWireMock}.
 *
 * @see InjectWireMock
 */
public class HeroesVillainsWiremockServerResource implements QuarkusTestResourceLifecycleManager {
  private final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());

  @Override
  public Map<String, String> start() {
    this.wireMockServer.start();

    var url = String.format(
      "localhost:%d",
      this.wireMockServer.isHttpsEnabled() ? this.wireMockServer.httpsPort() : this.wireMockServer.port()
    );

    return Map.of(
      "quarkus.stork.hero-service.service-discovery.address-list", url,
      "quarkus.stork.villain-service.service-discovery.address-list", url
    );
  }

  @Override
  public void stop() {
    this.wireMockServer.stop();
  }

  @Override
  public void inject(TestInjector testInjector) {
    testInjector.injectIntoFields(
      this.wireMockServer,
      new AnnotatedAndMatchesType(InjectWireMock.class, WireMockServer.class)
    );
  }
}
