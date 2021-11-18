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
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@QuarkusTest
@QuarkusTestResource(value = HeroesVillainsWiremockServer.class, restrictToAnnotatedClass = true)
class HeroClientTests {
	private static final String HERO_URI = "/api/heroes/random";
	private static final String DEFAULT_HERO_NAME = "Super Baguette";
	private static final String DEFAULT_HERO_PICTURE = "super_baguette.png";
	private static final String DEFAULT_HERO_POWERS = "eats baguette really quickly";
	private static final int DEFAULT_HERO_LEVEL = 42;

	private static final Hero DEFAULT_HERO = new Hero(
		DEFAULT_HERO_NAME,
		DEFAULT_HERO_LEVEL,
		DEFAULT_HERO_PICTURE,
		DEFAULT_HERO_POWERS
	);

	@InjectWireMock
	WireMockServer server;

	@Inject
	HeroClient heroClient;

	@Inject
	ObjectMapper objectMapper;

	@BeforeEach
	public void beforeEach() {
		this.server.resetAll();
	}

	@Test
	public void findsRandom() {
		this.server.stubFor(
			get(urlEqualTo(HERO_URI))
				.willReturn(okForContentType(APPLICATION_JSON, getDefaultHeroJson()))
		);

		var hero = this.heroClient.findRandomHero()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(hero)
			.isNotNull()
			.extracting(
				Hero::getName,
				Hero::getLevel,
				Hero::getPicture,
				Hero::getPowers
			)
			.containsExactly(
				DEFAULT_HERO_NAME,
				DEFAULT_HERO_LEVEL,
				DEFAULT_HERO_PICTURE,
				DEFAULT_HERO_POWERS
			);

		this.server.verify(1,
			getRequestedFor(urlEqualTo(HERO_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);
	}

	@Test
	public void recoversFrom404() {
		this.server.stubFor(
			get(urlEqualTo(HERO_URI))
				.willReturn(notFound())
		);

		this.heroClient.findRandomHero()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.assertItem(null);

		this.server.verify(1,
			getRequestedFor(urlEqualTo(HERO_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);
	}

	@Test
	public void doesntRecoverFrom500() {
		this.server.stubFor(
			get(urlEqualTo(HERO_URI))
				.willReturn(serverError())
		);

		this.heroClient.findRandomHero()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitFailure(Duration.ofSeconds(5))
			.assertFailedWith(WebApplicationException.class);

		this.server.verify(4,
			getRequestedFor(urlEqualTo(HERO_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);
	}

	private String getDefaultHeroJson() {
		try {
			return this.objectMapper.writeValueAsString(DEFAULT_HERO);
		}
		catch (JsonProcessingException ex) {
			throw new RuntimeException(ex);
		}
	}
}
