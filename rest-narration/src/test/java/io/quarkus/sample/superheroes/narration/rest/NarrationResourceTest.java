package io.quarkus.sample.superheroes.narration.rest;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.*;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.service.NarrationService;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;

import io.smallrye.mutiny.Uni;

@QuarkusTest
public class NarrationResourceTest {
  private static final String HERO_NAME = "Super Baguette";
  private static final int HERO_LEVEL = 42;
  private static final String HERO_POWERS = "Eats baguette in less than a second";
  private static final String HERO_TEAM_NAME = "heroes";
  private static final String VILLAIN_NAME = "Super Chocolatine";
  private static final int VILLAIN_LEVEL = 43;
  private static final String VILLAIN_POWERS = "Transforms chocolatine into pain au chocolat";
  private static final String VILLAIN_TEAM_NAME = "villains";
  private static final String NARRATION = "Lorem ipsum dolor sit amet";
  private static final Fight FIGHT = new Fight(VILLAIN_NAME, VILLAIN_LEVEL, VILLAIN_POWERS, HERO_NAME, HERO_LEVEL, HERO_POWERS, VILLAIN_TEAM_NAME, HERO_TEAM_NAME);
  private static final ArgumentMatcher<Fight> FIGHT_MATCHER = fight -> FIGHT.equals(fight);

  @Inject
  Instance<NarrationService> narrationServiceInstance;

  NarrationService narrationService;

  @BeforeEach
  public void setup() {
    this.narrationService = mock((Class<NarrationService>) this.narrationServiceInstance.getHandle().getBean().getBeanClass());
    QuarkusMock.installMockForInstance(narrationService, this.narrationServiceInstance.get());

    when(this.narrationService.narrate(argThat(FIGHT_MATCHER)))
      .thenReturn(Uni.createFrom().item(NARRATION));
  }

  @Test
  void shouldPingOpenAPI() {
    given()
      .accept(JSON)
      .when().get("/q/openapi").then()
      .statusCode(OK.getStatusCode())
      .contentType(JSON);

    verifyNoInteractions(this.narrationService);
  }

  @Test
  public void helloEndpoint() {
    get("/api/narration/hello").then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Hello Narration Resource"));

    verifyNoInteractions(this.narrationService);
  }

  @Test
  void shouldNarrateAFight() {
    given()
      .body(FIGHT)
      .contentType(JSON)
      .accept(TEXT)
      .when().post("/api/narration").then()
        .statusCode(OK.getStatusCode())
        .contentType(TEXT)
        .body(is(NARRATION));

    verify(this.narrationService).narrate(argThat(FIGHT_MATCHER));
    verifyNoMoreInteractions(this.narrationService);
  }

  @Test
  public void invalidFightToNarrate() {
    given()
      .contentType(JSON)
      .accept(TEXT)
      .when().post("/api/narration").then()
        .statusCode(400);

    verifyNoInteractions(this.narrationService);
  }
}
