package io.quarkus.sample.superheroes.fight.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.fight.HeroesVillainsWiremockServer;
import io.quarkus.sample.superheroes.fight.InjectWireMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@QuarkusTest
@QuarkusTestResource(HeroesVillainsWiremockServer.class)
class VillainClientTests {
	private static final String VILLAIN_API = "/api/villains/random";
	private static final String DEFAULT_VILLAIN_NAME = "Super Chocolatine";
	private static final String DEFAULT_VILLAIN_PICTURE = "super_chocolatine.png";
	private static final String DEFAULT_VILLAIN_POWERS = "does not eat pain au chocolat";
	private static final int DEFAULT_VILLAIN_LEVEL = 42;

	private static final Villain DEFAULT_VILLAIN = new Villain(
		DEFAULT_VILLAIN_NAME,
		DEFAULT_VILLAIN_LEVEL,
		DEFAULT_VILLAIN_PICTURE,
		DEFAULT_VILLAIN_POWERS
	);

	@InjectWireMock
	WireMockServer server;

	@Inject
	VillainClient villainClient;

	@Inject
	ObjectMapper objectMapper;

	@BeforeEach
	public void beforeEach() {
		this.server.resetAll();
	}

	@Test
	public void findsRandom() {
		this.server.stubFor(
			get(urlEqualTo(VILLAIN_API))
				.willReturn(okForContentType(APPLICATION_JSON, getDefaultVillainJson()))
		);

		var villain = this.villainClient.findRandomVillain()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(villain)
			.isNotNull()
			.extracting(
				Villain::getName,
				Villain::getLevel,
				Villain::getPicture,
				Villain::getPowers
			)
			.containsExactly(
				DEFAULT_VILLAIN_NAME,
				DEFAULT_VILLAIN_LEVEL,
				DEFAULT_VILLAIN_PICTURE,
				DEFAULT_VILLAIN_POWERS
			);

		this.server.verify(1,
			getRequestedFor(urlEqualTo(VILLAIN_API))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);
	}

	@Test
	public void recoversFrom404() {
		this.server.stubFor(
			get(urlEqualTo(VILLAIN_API))
				.willReturn(notFound())
		);

		this.villainClient.findRandomVillain()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.assertItem(null);

		this.server.verify(1,
			getRequestedFor(urlEqualTo(VILLAIN_API))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);
	}

	@Test
	public void doesntRecoverFrom500() {
		this.server.stubFor(
			get(urlEqualTo(VILLAIN_API))
				.willReturn(serverError())
		);

		this.villainClient.findRandomVillain()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitFailure(Duration.ofSeconds(5))
			.assertFailedWith(WebApplicationException.class);

		this.server.verify(4,
			getRequestedFor(urlEqualTo(VILLAIN_API))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);
	}

	private String getDefaultVillainJson() {
		try {
			return this.objectMapper.writeValueAsString(DEFAULT_VILLAIN);
		}
		catch (JsonProcessingException ex) {
			throw new RuntimeException(ex);
		}
	}
}
