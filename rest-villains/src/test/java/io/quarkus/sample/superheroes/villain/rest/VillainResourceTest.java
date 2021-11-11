package io.quarkus.sample.superheroes.villain.rest;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;

import org.hamcrest.core.Is;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.sample.superheroes.villain.Villain;
import io.quarkus.test.junit.QuarkusTest;

import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VillainResourceTest {
	private static final String DEFAULT_NAME = "Super Chocolatine";
	private static final String UPDATED_NAME = "Super Chocolatine (updated)";
	private static final String DEFAULT_OTHER_NAME = "Super Chocolatine chocolate in";
	private static final String UPDATED_OTHER_NAME = "Super Chocolatine chocolate in (updated)";
	private static final String DEFAULT_PICTURE = "super_chocolatine.png";
	private static final String UPDATED_PICTURE = "super_chocolatine_updated.png";
	private static final String DEFAULT_POWERS = "does not eat pain au chocolat";
	private static final String UPDATED_POWERS = "does not eat pain au chocolat (updated)";
	private static final int DEFAULT_LEVEL = 42;
	private static final int UPDATED_LEVEL = 43;

	private static final int NB_VILLAINS = 581;
	private static String villainId;

	@Test
	public void testHelloEndpoint() {
		given()
			.accept(TEXT_PLAIN)
			.when().get("/api/villains/hello")
			.then()
				.statusCode(200)
				.body(is("Hello Villain Resource"));
	}

	@Test
	void shouldNotGetUnknownVillain() {
		Long randomId = new Random().nextLong();
		given()
			.pathParam("id", randomId)
			.when().get("/api/villains/{id}")
			.then().statusCode(NOT_FOUND.getStatusCode());
	}

	@Test
	void shouldGetRandomVillain() {
		given()
			.when().get("/api/villains/random")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON);
	}

	@Test
	void shouldNotAddInvalidItem() {
		Villain villain = new Villain();
		villain.name = null;
		villain.otherName = DEFAULT_OTHER_NAME;
		villain.picture = DEFAULT_PICTURE;
		villain.powers = DEFAULT_POWERS;
		villain.level = 0;

		given()
			.body(villain)
			.contentType(JSON)
			.accept(JSON)
			.when()
			.post("/api/villains")
			.then()
			.statusCode(BAD_REQUEST.getStatusCode());
	}

	@Test
	@Order(1)
	void shouldGetInitialItems() {
		List<Villain> villains = get("/api/villains")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.extract()
			.body()
			.as(getVillainTypeRef());
		assertEquals(NB_VILLAINS, villains.size());
	}

	@Test
	@Order(2)
	void shouldAddAnItem() {
		Villain villain = new Villain();
		villain.name = DEFAULT_NAME;
		villain.otherName = DEFAULT_OTHER_NAME;
		villain.picture = DEFAULT_PICTURE;
		villain.powers = DEFAULT_POWERS;
		villain.level = DEFAULT_LEVEL;

		String location = given()
			.body(villain)
			.contentType(JSON)
			.accept(JSON)
			.when()
			.post("/api/villains")
			.then()
			.statusCode(CREATED.getStatusCode())
			.extract()
			.header("Location");
		assertTrue(location.contains("/api/villains"));

		// Stores the id
		String[] segments = location.split("/");
		villainId = segments[segments.length - 1];
		assertNotNull(villainId);

		given()
			.pathParam("id", villainId)
			.when()
			.get("/api/villains/{id}")
			.then()
			.contentType(JSON)
			.statusCode(OK.getStatusCode())
			.body("name", Is.is(DEFAULT_NAME))
			.body("otherName", Is.is(DEFAULT_OTHER_NAME))
			.body("level", Is.is(DEFAULT_LEVEL))
			.body("picture", Is.is(DEFAULT_PICTURE))
			.body("powers", Is.is(DEFAULT_POWERS));

		List<Villain> villains = get("/api/villains")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.extract()
			.body()
			.as(getVillainTypeRef());
		assertEquals(NB_VILLAINS + 1, villains.size());
	}

	@Test
	@Order(3)
	void testUpdatingAnItem() {
		Villain villain = new Villain();
		villain.id = Long.valueOf(villainId);
		villain.name = UPDATED_NAME;
		villain.otherName = UPDATED_OTHER_NAME;
		villain.picture = UPDATED_PICTURE;
		villain.powers = UPDATED_POWERS;
		villain.level = UPDATED_LEVEL;

		given()
			.body(villain)
			.contentType(JSON)
			.accept(JSON)
			.when()
			.put("/api/villains")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.body("name", Is.is(UPDATED_NAME))
			.body("otherName", Is.is(UPDATED_OTHER_NAME))
			.body("level", Is.is(UPDATED_LEVEL))
			.body("picture", Is.is(UPDATED_PICTURE))
			.body("powers", Is.is(UPDATED_POWERS));

		List<Villain> villains = get("/api/villains")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.extract()
			.body()
			.as(getVillainTypeRef());
		assertEquals(NB_VILLAINS + 1, villains.size());
	}

	@Test
	@Order(4)
	void shouldRemoveAnItem() {
		given().pathParam("id", villainId).when().delete("/api/villains/{id}").then().statusCode(NO_CONTENT.getStatusCode());

		List<Villain> villains = get("/api/villains")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.extract()
			.body()
			.as(getVillainTypeRef());
		assertEquals(NB_VILLAINS, villains.size());
	}

	@Test
	void shouldPingOpenAPI() {
		given()
			.accept(JSON)
			.when().get("/q/openapi")
			.then().statusCode(OK.getStatusCode());
	}

	private TypeRef<List<Villain>> getVillainTypeRef() {
		return new TypeRef<>() {
			// Kept empty on purpose
		};
	}
}
