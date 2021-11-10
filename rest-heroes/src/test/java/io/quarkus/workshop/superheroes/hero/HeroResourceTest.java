package io.quarkus.workshop.superheroes.hero;

import io.quarkus.test.junit.QuarkusTest;
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
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HeroResourceTest {

    private static final String JSON = "application/json;charset=UTF-8";

    private static final String DEFAULT_NAME = "Super Baguette";
    private static final String UPDATED_NAME = "Super Baguette (updated)";
    private static final String DEFAULT_OTHER_NAME = "Super Baguette Tradition";
    private static final String UPDATED_OTHER_NAME = "Super Baguette Tradition (updated)";
    private static final String DEFAULT_PICTURE = "super_baguette.png";
    private static final String UPDATED_PICTURE = "super_baguette_updated.png";
    private static final String DEFAULT_POWERS = "eats baguette really quickly";
    private static final String UPDATED_POWERS = "eats baguette really quickly (updated)";
    private static final int DEFAULT_LEVEL = 42;
    private static final int UPDATED_LEVEL = 43;

    private static final int NB_HEROES = 951;
    private static String heroId;

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
            .when().get("/api/heroes/hello")
            .then()
            .statusCode(200)
            .body(is("Hello Hero Resource"));
    }

    @Test
    void shouldNotGetUnknownHero() {
        Long randomId = new Random().nextLong();
        given()
            .pathParam("id", randomId)
            .when().get("/api/heroes/{id}")
            .then()
            .statusCode(NO_CONTENT.getStatusCode());
    }

    @Test
    void shouldGetRandomHero() {
        given()
            .when().get("/api/heroes/random")
            .then()
            .statusCode(OK.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_JSON);
    }

    @Test
    void shouldNotAddInvalidItem() {
        Hero hero = new Hero();
        hero.name = null;
        hero.otherName = DEFAULT_OTHER_NAME;
        hero.picture = DEFAULT_PICTURE;
        hero.powers = DEFAULT_POWERS;
        hero.level = 0;

        given()
            .body(hero)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .header(ACCEPT, APPLICATION_JSON)
            .when()
            .post("/api/heroes")
            .then()
            .statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    @Order(1)
    void shouldGetInitialItems() {
        List<Hero> heroes = get("/api/heroes").then()
            .statusCode(OK.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .extract().body().as(getHeroTypeRef());
        assertEquals(NB_HEROES, heroes.size());
    }

    @Test
    @Order(2)
    void shouldAddAnItem() {
        Hero hero = new Hero();
        hero.name = DEFAULT_NAME;
        hero.otherName = DEFAULT_OTHER_NAME;
        hero.picture = DEFAULT_PICTURE;
        hero.powers = DEFAULT_POWERS;
        hero.level = DEFAULT_LEVEL;

        String location = given()
            .body(hero)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .header(ACCEPT, APPLICATION_JSON)
            .when()
            .post("/api/heroes")
            .then()
            .statusCode(CREATED.getStatusCode())
            .extract().header("Location");
        assertTrue(location.contains("/api/heroes"));

        // Stores the id
        String[] segments = location.split("/");
        heroId = segments[segments.length - 1];
        assertNotNull(heroId);

        given()
            .pathParam("id", heroId)
            .when().get("/api/heroes/{id}")
            .then()
            .statusCode(OK.getStatusCode())
            .body("name", Is.is(DEFAULT_NAME))
            .body("otherName", Is.is(DEFAULT_OTHER_NAME))
            .body("level", Is.is(DEFAULT_LEVEL))
            .body("picture", Is.is(DEFAULT_PICTURE))
            .body("powers", Is.is(DEFAULT_POWERS));

        List<Hero> heroes = get("/api/heroes").then()
            .statusCode(OK.getStatusCode())
            .extract().body().as(getHeroTypeRef());
        assertEquals(NB_HEROES + 1, heroes.size());
    }

    @Test
    @Order(3)
    void shouldUpdateAnItem() {
        Hero hero = new Hero();
        hero.id = Long.valueOf(heroId);
        hero.name = UPDATED_NAME;
        hero.otherName = UPDATED_OTHER_NAME;
        hero.picture = UPDATED_PICTURE;
        hero.powers = UPDATED_POWERS;
        hero.level = UPDATED_LEVEL;

        given()
            .body(hero)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .header(ACCEPT, APPLICATION_JSON)
            .when()
            .put("/api/heroes")
            .then()
            .statusCode(OK.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .body("name", Is.is(UPDATED_NAME))
            .body("otherName", Is.is(UPDATED_OTHER_NAME))
            .body("level", Is.is(UPDATED_LEVEL))
            .body("picture", Is.is(UPDATED_PICTURE))
            .body("powers", Is.is(UPDATED_POWERS));

        List<Hero> heroes = get("/api/heroes").then()
            .statusCode(OK.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .extract().body().as(getHeroTypeRef());
        assertEquals(NB_HEROES + 1, heroes.size());
    }

    @Test
    @Order(4)
    void shouldRemoveAnItem() {
        given()
            .pathParam("id", heroId)
            .when().delete("/api/heroes/{id}")
            .then()
            .statusCode(NO_CONTENT.getStatusCode());

        List<Hero> heroes = get("/api/heroes").then()
            .statusCode(OK.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .extract().body().as(getHeroTypeRef());
        assertEquals(NB_HEROES, heroes.size());
    }

    private TypeRef<List<Hero>> getHeroTypeRef() {
        return new TypeRef<List<Hero>>() {
            // Kept empty on purpose
        };
    }
}
