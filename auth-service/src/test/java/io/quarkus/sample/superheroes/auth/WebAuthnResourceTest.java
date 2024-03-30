package io.quarkus.sample.superheroes.auth;

import static io.restassured.RestAssured.given;

import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;

import io.quarkiverse.openfga.client.AuthorizationModelClient;

import io.quarkiverse.openfga.client.model.TupleKey;
import static org.hamcrest.Matchers.*;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.auth.webauthn.User;

import jakarta.inject.Inject;

import jakarta.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.security.webauthn.WebAuthnController;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.webauthn.WebAuthnEndpointHelper;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;

@QuarkusTest
public class WebAuthnResourceTest {
  @Inject
  AuthorizationModelClient defaultAuthModelClient;

  enum Endpoint {
    DEFAULT, MANUAL;
  }

  @Test
  public void testWebAuthnUser() {
    testWebAuthn("viewer", createDefaultUser("viewer","viewer"), Endpoint.MANUAL);
    testWebAuthn("referee", createDefaultUser("referee","ref"), Endpoint.MANUAL);
    testWebAuthn("full", createDefaultUser("full","full"), Endpoint.MANUAL);
  }


  private void testWebAuthn(String userName, User user, Endpoint endpoint) {
    Filter cookieFilter = new RenardeCookieFilter();
    WebAuthnHardware token = new WebAuthnHardware();
    verifyLoggedOut(cookieFilter);
    defaultAuthModelClient.write(null, Collections.singletonList(TupleKey.of("plan:"+user.plan, "subscriber", "user:"+userName))).await().indefinitely();
    // two-step registration
    String challenge = WebAuthnEndpointHelper.invokeRegistration(userName, cookieFilter);
    JsonObject registrationJson = token.makeRegistrationJson(challenge);
    if(endpoint == Endpoint.DEFAULT)
      WebAuthnEndpointHelper.invokeCallback(registrationJson, cookieFilter);
    else {
      invokeCustomEndpoint("/register", cookieFilter, request -> {
        WebAuthnEndpointHelper.addWebAuthnRegistrationFormParameters(request, registrationJson);
        request.formParam("userName", userName);
        request.formParam("plan",user.plan);
      });
    }

    // verify that we can access logged-in endpoints
    verifyLoggedIn(cookieFilter, userName, user);

    // logout
    WebAuthnEndpointHelper.invokeLogout(cookieFilter);

    verifyLoggedOut(cookieFilter);

    // two-step login
    challenge = WebAuthnEndpointHelper.invokeLogin(userName, cookieFilter);
    JsonObject loginJson = token.makeLoginJson(challenge);
    WebAuthnEndpointHelper.invokeCallback(loginJson, cookieFilter);
//    if(endpoint == Endpoint.DEFAULT)
//      WebAuthnEndpointHelper.invokeCallback(loginJson, cookieFilter);
//    else {
//      invokeCustomEndpoint("/login", cookieFilter, request -> {
//        WebAuthnEndpointHelper.addWebAuthnLoginFormParameters(request, loginJson);
//        request.formParam("userName", userName);
//      });
//    }

    // verify that we can access logged-in endpoints
    verifyLoggedIn(cookieFilter, userName, user);
    verifyValidPlans(cookieFilter, userName, user);

    // logout
    WebAuthnEndpointHelper.invokeLogout(cookieFilter);

    verifyLoggedOut(cookieFilter);
  }

  private void invokeCustomEndpoint(String uri, Filter cookieFilter, Consumer<RequestSpecification> requestCustomiser) {
    RequestSpecification request = given()
      .when();
    requestCustomiser.accept(request);
    request
      .filter(cookieFilter)
      .redirects().follow(false)
      .log().ifValidationFails()
      .post(uri)
      .then()
      .statusCode(200)
      .log().ifValidationFails()
      .cookie(WebAuthnController.CHALLENGE_COOKIE, Matchers.is(""))
      .cookie(WebAuthnController.USERNAME_COOKIE, Matchers.is(""))
      .cookie("quarkus-credential", Matchers.notNullValue());
  }

  private void verifyLoggedIn(Filter cookieFilter, String userName, User user) {
    RestAssured.given().filter(cookieFilter)
      .when()
      .get("/verify-session")
      .then()
      .statusCode(200);
  }

  private void verifyLoggedOut(Filter cookieFilter) {
    // public API still good
    RestAssured.given().filter(cookieFilter)
      .when()
      .get("/verify-session")
      .then()
      .statusCode(401);
  }
  private void verifyValidPlans(Filter cookieFilter, String userName, User user) {
    // public API still good
    if(Objects.equals(user.plan, "viewer")){
      RestAssured.given().filter(cookieFilter)
        .when()
        .get("/feature-access/ai")
        .then()
        .statusCode(200)
        .body(is("false"));
    }
    if(Objects.equals(user.plan, "viewer")){
      Log.info("here");
      RestAssured.given().filter(cookieFilter)
        .when()
        .get("/feature-access/ai")
        .then()
        .statusCode(200)
        .body(is("false"));
    }
    if(Objects.equals(user.plan, "ref")){
      RestAssured.given().filter(cookieFilter)
        .when()
        .get("/feature-access/ai")
        .then()
        .statusCode(200)
        .body(is("false"));
    }
    if(Objects.equals(user.plan, "ref")){
      RestAssured.given().filter(cookieFilter)
        .when()
        .get("/feature-access/fight")
        .then()
        .statusCode(200)
        .body(is("true"));
    }
    if(Objects.equals(user.plan, "full")){
      RestAssured.given().filter(cookieFilter)
        .when()
        .get("/feature-access/ai")
        .then()
        .statusCode(200)
        .body(is("true"));
    }
    if(Objects.equals(user.plan, "full")){
      RestAssured.given().filter(cookieFilter)
        .when()
        .get("/feature-access/fight")
        .then()
        .statusCode(200)
        .body(is("true"));
    }

  }

  private static User createDefaultUser(String name, String plan) {
    var user = new User();
    user.userName = name;
    user.plan = plan;
    return user;
  }
}
