package io.quarkus.sample.superheroes.fight.rest;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class FightResourceTest {

    private static final String DEFAULT_WINNER_NAME = "Super Baguette";
    private static final String DEFAULT_WINNER_PICTURE = "super_baguette.png";
    private static final int DEFAULT_WINNER_LEVEL = 42;
    private static final String DEFAULT_LOSER_NAME = "Super Chocolatine";
    private static final String DEFAULT_LOSER_PICTURE = "super_chocolatine.png";
    private static final int DEFAULT_LOSER_LEVEL = 6;

    private static final int NB_FIGHTS = 3;
    private static String fightId;

//    @Test
//    void shouldPingOpenAPI() {
//        given()
//            .header(ACCEPT, APPLICATION_JSON)
//            .when().get("/q/openapi")
//            .then()
//            .statusCode(OK.getStatusCode());
//    }
//
//    @Test
//    public void testHelloEndpoint() {
//        given()
//            .header(ACCEPT, TEXT_PLAIN)
//            .when().get("/api/fights/hello")
//            .then()
//            .statusCode(200)
//            .body(is("Hello Fight Resource"));
//    }
//
//    @Test
//    void shouldNotGetUnknownFight() {
//        Long randomId = new Random().nextLong();
//        given()
//            .pathParam("id", randomId)
//            .when().get("/api/fights/{id}")
//            .then()
//            .statusCode(NO_CONTENT.getStatusCode());
//    }
//
//    @Test
//    void shouldNotAddInvalidItem() {
//        Fighters fighters = new Fighters();
//        fighters.hero = null;
//        fighters.villain = null;
//
//        given()
//            .body(fighters)
//            .header(CONTENT_TYPE, APPLICATION_JSON)
//            .header(ACCEPT, APPLICATION_JSON)
//            .when()
//            .post("/api/fights")
//            .then()
//            .statusCode(BAD_REQUEST.getStatusCode());
//    }
//
//    @Test
//    @Order(1)
//    void shouldGetInitialItems() {
//        List<Fight> fights = get("/api/fights").then()
//            .statusCode(OK.getStatusCode())
//            .extract().body().as(getFightTypeRef());
//        assertEquals(NB_FIGHTS, fights.size());
//    }
//
//    @Test
//    @Order(2)
//    void shouldAddAnItem() {
//        Hero hero = new Hero();
//        hero.name = DEFAULT_WINNER_NAME;
//        hero.picture = DEFAULT_WINNER_PICTURE;
//        hero.level = DEFAULT_WINNER_LEVEL;
//        Villain villain = new Villain();
//        villain.name = DEFAULT_LOSER_NAME;
//        villain.picture = DEFAULT_LOSER_PICTURE;
//        villain.level = DEFAULT_LOSER_LEVEL;
//        Fighters fighters = new Fighters();
//        fighters.hero = hero;
//        fighters.villain = villain;
//
//        fightId = given()
//            .body(fighters)
//            .header(CONTENT_TYPE, APPLICATION_JSON)
//            .header(ACCEPT, APPLICATION_JSON)
//            .when()
//            .post("/api/fights")
//            .then()
//            .statusCode(OK.getStatusCode())
//            .body(containsString("winner"), containsString("loser"))
//            .extract().body().jsonPath().getString("id");
//
//        assertNotNull(fightId);
//
//        given()
//            .pathParam("id", fightId)
//            .when().get("/api/fights/{id}")
//            .then()
//            .statusCode(OK.getStatusCode())
//            .header(CONTENT_TYPE, APPLICATION_JSON)
//            .body("winnerName", Is.is(DEFAULT_WINNER_NAME))
//            .body("winnerPicture", Is.is(DEFAULT_WINNER_PICTURE))
//            .body("winnerLevel", Is.is(DEFAULT_WINNER_LEVEL))
//            .body("loserName", Is.is(DEFAULT_LOSER_NAME))
//            .body("loserPicture", Is.is(DEFAULT_LOSER_PICTURE))
//            .body("loserLevel", Is.is(DEFAULT_LOSER_LEVEL))
//            .body("fightDate", Is.is(notNullValue()));
//
//        List<Fight> fights = get("/api/fights").then()
//            .statusCode(OK.getStatusCode())
//            .extract().body().as(getFightTypeRef());
//        assertEquals(NB_FIGHTS + 1, fights.size());
//    }
//
//    @Test
//    void shouldGetRandomFighters() {
//        given()
//            .when().get("/api/fights/randomfighters")
//            .then()
//            .statusCode(OK.getStatusCode())
//            .header(CONTENT_TYPE, APPLICATION_JSON)
//            .body("hero.name", Is.is(MockHeroClient.DEFAULT_HERO_NAME))
//            .body("hero.picture", Is.is(MockHeroClient.DEFAULT_HERO_PICTURE))
//            .body("hero.level", Is.is(MockHeroClient.DEFAULT_HERO_LEVEL))
//            .body("villain.name", Is.is(MockVillainClient.DEFAULT_VILLAIN_NAME))
//            .body("villain.picture", Is.is(MockVillainClient.DEFAULT_VILLAIN_PICTURE))
//            .body("villain.level", Is.is(MockVillainClient.DEFAULT_VILLAIN_LEVEL));
//    }
//
//    private TypeRef<List<Fight>> getFightTypeRef() {
//        return new TypeRef<>() {
//	        // Kept empty on purpose
//        };
//    }
}
