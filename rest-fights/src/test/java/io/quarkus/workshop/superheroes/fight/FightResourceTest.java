package io.quarkus.workshop.superheroes.fight;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.workshop.superheroes.fight.client.Hero;
import io.quarkus.workshop.superheroes.fight.client.MockHeroProxy;
import io.quarkus.workshop.superheroes.fight.client.MockVillainProxy;
import io.quarkus.workshop.superheroes.fight.client.Villain;
import io.restassured.common.mapper.TypeRef;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import java.util.Random;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FightResourceTest {

    private static final String DEFAULT_WINNER_NAME = "Super Baguette";
    private static final String DEFAULT_WINNER_PICTURE = "super_baguette.png";
    private static final int DEFAULT_WINNER_LEVEL = 42;
    private static final String DEFAULT_LOSER_NAME = "Super Chocolatine";
    private static final String DEFAULT_LOSER_PICTURE = "super_chocolatine.png";
    private static final int DEFAULT_LOSER_LEVEL = 6;

    private static final int NB_FIGHTS = 3;
    private static String fightId;

    @Test
    void shouldPingOpenAPI() {
        given()
            .header(ACCEPT, APPLICATION_JSON)
            .when().get("/q/openapi")
            .then()
            .statusCode(OK.getStatusCode());
    }

    @Test
    public void testHelloEndpoint() {
        given()
            .header(ACCEPT, TEXT_PLAIN)
            .when().get("/api/fights/hello")
            .then()
            .statusCode(200)
            .body(is("Hello Fight Resource"));
    }

    @Test
    void shouldNotGetUnknownFight() {
        Long randomId = new Random().nextLong();
        given()
            .pathParam("id", randomId)
            .when().get("/api/fights/{id}")
            .then()
            .statusCode(NO_CONTENT.getStatusCode());
    }

    @Test
    void shouldNotAddInvalidItem() {
        Fighters fighters = new Fighters();
        fighters.hero = null;
        fighters.villain = null;

        given()
            .body(fighters)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .header(ACCEPT, APPLICATION_JSON)
            .when()
            .post("/api/fights")
            .then()
            .statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    @Order(1)
    void shouldGetInitialItems() {
        List<Fight> fights = get("/api/fights").then()
            .statusCode(OK.getStatusCode())
            .extract().body().as(getFightTypeRef());
        assertEquals(NB_FIGHTS, fights.size());
    }

    @Test
    @Order(2)
    void shouldAddAnItem() {
        Hero hero = new Hero();
        hero.name = DEFAULT_WINNER_NAME;
        hero.picture = DEFAULT_WINNER_PICTURE;
        hero.level = DEFAULT_WINNER_LEVEL;
        Villain villain = new Villain();
        villain.name = DEFAULT_LOSER_NAME;
        villain.picture = DEFAULT_LOSER_PICTURE;
        villain.level = DEFAULT_LOSER_LEVEL;
        Fighters fighters = new Fighters();
        fighters.hero = hero;
        fighters.villain = villain;

        fightId = given()
            .body(fighters)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .header(ACCEPT, APPLICATION_JSON)
            .when()
            .post("/api/fights")
            .then()
            .statusCode(OK.getStatusCode())
            .body(containsString("winner"), containsString("loser"))
            .extract().body().jsonPath().getString("id");

        assertNotNull(fightId);

        given()
            .pathParam("id", fightId)
            .when().get("/api/fights/{id}")
            .then()
            .statusCode(OK.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .body("winnerName", Is.is(DEFAULT_WINNER_NAME))
            .body("winnerPicture", Is.is(DEFAULT_WINNER_PICTURE))
            .body("winnerLevel", Is.is(DEFAULT_WINNER_LEVEL))
            .body("loserName", Is.is(DEFAULT_LOSER_NAME))
            .body("loserPicture", Is.is(DEFAULT_LOSER_PICTURE))
            .body("loserLevel", Is.is(DEFAULT_LOSER_LEVEL))
            .body("fightDate", Is.is(notNullValue()));

        List<Fight> fights = get("/api/fights").then()
            .statusCode(OK.getStatusCode())
            .extract().body().as(getFightTypeRef());
        assertEquals(NB_FIGHTS + 1, fights.size());
    }

    @Test
    void shouldGetRandomFighters() {
        given()
            .when().get("/api/fights/randomfighters")
            .then()
            .statusCode(OK.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .body("hero.name", Is.is(MockHeroProxy.DEFAULT_HERO_NAME))
            .body("hero.picture", Is.is(MockHeroProxy.DEFAULT_HERO_PICTURE))
            .body("hero.level", Is.is(MockHeroProxy.DEFAULT_HERO_LEVEL))
            .body("villain.name", Is.is(MockVillainProxy.DEFAULT_VILLAIN_NAME))
            .body("villain.picture", Is.is(MockVillainProxy.DEFAULT_VILLAIN_PICTURE))
            .body("villain.level", Is.is(MockVillainProxy.DEFAULT_VILLAIN_LEVEL));
    }

    private TypeRef<List<Fight>> getFightTypeRef() {
        return new TypeRef<List<Fight>>() {
            // Kept empty on purpose
        };
    }
}
