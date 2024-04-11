package io.quarkus.sample.superheroes.fight.client;


import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;

@RegisterRestClient(configKey = "auth-client")
@RegisterClientHeaders
@Path("/")
public interface AuthRestClient {
  @GET
  @Path("/q/webauthn/webauthn.js")
  Uni<Response> javascript();
  @POST
  @Path("/q/webauthn/register")
  Uni<Response> setUpObtainRegistrationChallenge(JsonObject jsonObject);
  @POST
  @Path("/register")
  Uni<Response> register(  @FormParam("userName") String userName,@FormParam("plan") String plan,@BeanParam WebAuthnRegisterResponse webAuthnResponse);
  @POST
  @Path("/q/webauthn/login")
  Uni<Response> setUpObtainLoginChallenge(JsonObject jsonObject);
  @POST
  @Path("/q/webauthn/callback")
  Uni<Response> callback(JsonObject jsonObject);
  @GET
  @Path("/q/webauthn/logout")
  Uni<Response> logout();

  @GET
  @Path("/verify-session")
  Uni<Response> verify();

  @GET
  @Path("/feature-access/{feature}")
  Uni<Boolean> checkFeatureAccess(@RestPath String feature);

}
