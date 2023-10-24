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

public class LocationWiremockServerResource implements QuarkusTestResourceLifecycleManager {
	private final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort().withRootDirectory("src/test/resources/wiremock").extensions(new GrpcExtensionFactory()));

	@Override
	public Map<String, String> start() {
		this.wireMockServer.start();

		return Map.of("quarkus.grpc.clients.location.host", "localhost", "quarkus.grpc.clients.location.port", String.valueOf(getWiremockPort()));
	}

	@Override
	public void stop() {
		this.wireMockServer.stop();
	}

	private int getWiremockPort() {
		return this.wireMockServer.isHttpsEnabled() ? this.wireMockServer.httpsPort() : this.wireMockServer.port();
	}

	@Override
	public void inject(TestInjector testInjector) {
		testInjector.injectIntoFields(this.wireMockServer, new AnnotatedAndMatchesType(InjectWireMock.class, WireMockServer.class));

		testInjector.injectIntoFields(new WireMockGrpcService(new WireMock(getWiremockPort()), LocationsGrpc.SERVICE_NAME), new AnnotatedAndMatchesType(InjectWireMock.class, WireMockGrpcService.class));
	}
}
