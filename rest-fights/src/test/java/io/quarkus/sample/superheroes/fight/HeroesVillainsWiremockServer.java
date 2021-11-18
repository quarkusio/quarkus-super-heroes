package io.quarkus.sample.superheroes.fight;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.util.Map;

import io.quarkus.sample.superheroes.fight.client.InjectWireMock;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;

public class HeroesVillainsWiremockServer implements QuarkusTestResourceLifecycleManager {
	private WireMockServer wireMockServer;

	@Override
	public Map<String, String> start() {
		this.wireMockServer = new WireMockServer(
			wireMockConfig()
				.dynamicPort()
				.notifier(new ConsoleNotifier(true))
		);

		this.wireMockServer.start();

		return Map.of(
			"quarkus.rest-client.hero-client.url", this.wireMockServer.baseUrl(),
			"fight.villain.client-base-url", this.wireMockServer.baseUrl()
		);
	}

	@Override
	public void stop() {
		if (this.wireMockServer != null) {
			this.wireMockServer.stop();
		}
	}

	@Override
	public void inject(TestInjector testInjector) {
		testInjector.injectIntoFields(
			this.wireMockServer,
			new AnnotatedAndMatchesType(InjectWireMock.class, WireMockServer.class)
		);
	}
}
