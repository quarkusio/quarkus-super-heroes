package io.quarkus.sample.superheroes.ui;

import static io.restassured.RestAssured.get;
import static io.restassured.http.ContentType.HTML;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.matchesPattern;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import io.quarkiverse.quinoa.testing.QuinoaTestProfiles;

/**
 * Tests the served javascript application.
 * By default, the Web UI is not build/served in @QuarkusTest. The goal is to be able to test
 * your api without having to wait for the Web UI build.
 * The `Enable` test profile enables the Web UI (build and serve).
 */
@QuarkusTest
@TestProfile(QuinoaTestProfiles.Enable.class)
public class WebUITests {

    @Test
    public void webApplicationEndpoint() {
        get("/")
            .then()
            .statusCode(OK.getStatusCode())
            .contentType(HTML)
            .body(matchesPattern(Pattern.compile(".*<div id=\"root\">.*", Pattern.DOTALL)));
    }
}
