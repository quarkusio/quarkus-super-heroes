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
  public Response javascript();
  @POST
  @Path("/q/webauthn/register")
  public Response setUpObtainRegistrationChallenge(JsonObject jsonObject);
  @POST
  @Path("/register")
  public Response register(  @FormParam("userName") String userName,@FormParam("role") String role,@BeanParam WebAuthnRegisterResponse webAuthnResponse);
  @POST
  @Path("/q/webauthn/login")
  public Response setUpObtainLoginChallenge(JsonObject jsonObject);
  @POST
  @Path("/q/webauthn/callback")
  public Response callback(JsonObject jsonObject);
  @GET
  @Path("/q/webauthn/logout")
  public Response logout();
  @GET
  @Path("/me")
  public String me(@Context SecurityContext securityContext);

  @GET
  @Path("/verify-session")
  public Response verify();

  @GET
  @Path("/feature-access/{feature}")
  public Uni<Boolean> checkFeatureAccess(@RestPath String feature);

}
