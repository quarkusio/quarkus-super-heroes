package io.quarkus.sample.superheroes.ui;

import static io.restassured.RestAssured.get;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests the resource layer ({@link EnvResource}).
 */
@QuarkusTest
public class EnvResourceTests {

  /**
   * Checks that the react app would be able to do
   * <script src="env.js"></script>
   * and get something sensible back
   */
	@Test
	public void javascriptEndpoint() {
		get("/env.js")
			.then()
			.statusCode(OK.getStatusCode())
			.body(is("window.APP_CONFIG={\"API_BASE_URL\":\"http://localhost:8082\",\"CALCULATE_API_BASE_URL\":false}"));
    // This content is javascript, not json. Doing a simple equality check like this
    // is brittle, but we can update it to something more flexible if we start to see issues
	}

}
