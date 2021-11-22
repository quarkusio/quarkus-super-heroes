package io.quarkus.sample.superheroes.fight;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType;

import com.github.tomakehurst.wiremock.WireMockServer;

public class HeroesVillainsWiremockServerResource implements QuarkusTestResourceLifecycleManager {
	private final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());

	@Override
	public Map<String, String> start() {
		this.wireMockServer.start();

		return Map.of(
			"quarkus.rest-client.hero-client.url", this.wireMockServer.baseUrl(),
			"fight.villain.client-base-url", this.wireMockServer.baseUrl()
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
