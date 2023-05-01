package io.quarkus.sample.superheroes.fight.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.*;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.MediaType.*;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.ParameterizedTest.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.HeroesVillainsWiremockServerResource;
import io.quarkus.sample.superheroes.fight.InjectWireMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

@QuarkusTest
@QuarkusTestResource(HeroesVillainsWiremockServerResource.class)
public class HeaderPropagationTests {
  private static final String PROPAGATE_HEADER_NAME = "x-propagate";
  private static final String PROPAGATE_HEADER_VALUE = "propagate-value";

  private static final String NO_PROPAGATE_HEADER_NAME = PROPAGATE_HEADER_NAME + "-2";
  private static final String NO_PROPAGATE_HEADER_VALUE = PROPAGATE_HEADER_VALUE + "-2";

  private static final String HERO_API_BASE_URI = "/api/heroes";
  private static final String HERO_API_URI = HERO_API_BASE_URI + "/random";
  private static final String HERO_API_HELLO_URI = HERO_API_BASE_URI + "/hello";
  private static final String DEFAULT_HERO_NAME = "Super Baguette";
  private static final String DEFAULT_HERO_PICTURE = "super_baguette.png";
  private static final String DEFAULT_HERO_POWERS = "eats baguette really quickly";
  private static final int DEFAULT_HERO_LEVEL = 62;

  private static final Hero DEFAULT_HERO = new Hero(
    DEFAULT_HERO_NAME,
    DEFAULT_HERO_LEVEL,
    DEFAULT_HERO_PICTURE,
    DEFAULT_HERO_POWERS
  );

  private static final String VILLAIN_API_BASE_URI = "/api/villains";
  private static final String VILLAIN_API_URI = VILLAIN_API_BASE_URI + "/random";
  private static final String VILLAIN_API_HELLO_URI = VILLAIN_API_BASE_URI + "/hello";
  private static final String DEFAULT_VILLAIN_NAME = "Super Chocolatine";
  private static final String DEFAULT_VILLAIN_PICTURE = "super_chocolatine.png";
  private static final String DEFAULT_VILLAIN_POWERS = "does not eat pain au chocolat";
  private static final int DEFAULT_VILLAIN_LEVEL = 40;
  private static final Villain DEFAULT_VILLAIN = new Villain(
    DEFAULT_VILLAIN_NAME,
    DEFAULT_VILLAIN_LEVEL,
    DEFAULT_VILLAIN_PICTURE,
    DEFAULT_VILLAIN_POWERS
  );

  @InjectWireMock
  WireMockServer wireMockServer;

  @Inject
  ObjectMapper objectMapper;

  @BeforeEach
  public void beforeEach() {
    // Reset WireMock
    this.wireMockServer.resetAll();
  }

  @Test
  public void getRandomFightersAllOk() {
    this.wireMockServer.stubFor(
      WireMock.get(urlEqualTo(HERO_API_URI))
        .willReturn(okForContentType(APPLICATION_JSON, getDefaultHeroJson()))
    );

    this.wireMockServer.stubFor(
      WireMock.get(urlEqualTo(VILLAIN_API_URI))
        .willReturn(okForContentType(APPLICATION_JSON, getDefaultVillainJson()))
    );

    var randomFighters = given()
      .header(PROPAGATE_HEADER_NAME, PROPAGATE_HEADER_VALUE)
      .header(NO_PROPAGATE_HEADER_NAME, NO_PROPAGATE_HEADER_VALUE)
      .when()
        .get("/api/fights/randomfighters")
      .then()
        .statusCode(OK.getStatusCode())
        .contentType(JSON)
        .extract().as(Fighters.class);

    assertThat(randomFighters)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new Fighters(DEFAULT_HERO, DEFAULT_VILLAIN));

    this.wireMockServer.verify(1,
      getRequestedFor(urlEqualTo(HERO_API_URI))
        .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
        .withHeader(PROPAGATE_HEADER_NAME, equalTo(PROPAGATE_HEADER_VALUE))
        .withoutHeader(NO_PROPAGATE_HEADER_NAME)
    );

    this.wireMockServer.verify(1,
      getRequestedFor(urlEqualTo(VILLAIN_API_URI))
        .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
        .withHeader(PROPAGATE_HEADER_NAME, equalTo(PROPAGATE_HEADER_VALUE))
        .withoutHeader(NO_PROPAGATE_HEADER_NAME)
    );
  }

  @ParameterizedTest(name = DISPLAY_NAME_PLACEHOLDER + "[" + INDEX_PLACEHOLDER + "] (" + ARGUMENTS_WITH_NAMES_PLACEHOLDER + ")")
  @MethodSource("helloServiceHeadersPropagateValues")
  public void helloServiceHeadersPropagate(String requestUri, String downstreamUri, String expectedBody) {
    this.wireMockServer.stubFor(
      WireMock.get(urlEqualTo(downstreamUri))
        .willReturn(okForContentType(TEXT_PLAIN, expectedBody))
    );

    given()
      .header(PROPAGATE_HEADER_NAME, PROPAGATE_HEADER_VALUE)
      .header(NO_PROPAGATE_HEADER_NAME, NO_PROPAGATE_HEADER_VALUE)
      .when()
        .get(requestUri)
      .then()
        .statusCode(OK.getStatusCode())
        .contentType(TEXT)
        .body(is(expectedBody));

    this.wireMockServer.verify(1,
      getRequestedFor(urlEqualTo(downstreamUri))
        .withHeader(ACCEPT, containing(TEXT_PLAIN))
        .withHeader(PROPAGATE_HEADER_NAME, equalTo(PROPAGATE_HEADER_VALUE))
        .withoutHeader(NO_PROPAGATE_HEADER_NAME)
    );
  }

  static Stream<Arguments> helloServiceHeadersPropagateValues() {
    return Stream.of(
      arguments("/api/fights/hello/heroes", HERO_API_HELLO_URI, "Hello heroes!"),
      arguments("/api/fights/hello/villains", VILLAIN_API_HELLO_URI, "Hello villains!")
    );
  }

  private <T> String writeAsJson(T entity) {
    try {
      return this.objectMapper.writeValueAsString(entity);
    }
    catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
  }

  private String getDefaultVillainJson() {
    return writeAsJson(DEFAULT_VILLAIN);
  }

  private String getDefaultHeroJson() {
    return writeAsJson(DEFAULT_HERO);
  }
}
