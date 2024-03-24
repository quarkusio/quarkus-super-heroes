package io.quarkus.sample.superheroes.fight.rest;



import io.quarkus.sample.superheroes.fight.client.AuthRestClient;
import io.quarkus.sample.superheroes.fight.client.WebAuthnRegisterResponse;

import io.smallrye.mutiny.Uni;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;

import java.security.Principal;

import static jakarta.ws.rs.core.MediaType.*;

@Path("/")
public class AuthResource {
  private static final Logger LOG = Logger.getLogger(AuthResource.class);



  @RestClient
  AuthRestClient authClient;


  @Path("/q/webauthn/webauthn.js")
  @GET
  public Response getJavascript(){
    LOG.info("Returning authservice javascript......");
    return authClient.javascript();
  }
  @POST
  @Path("/q/webauthn/register")
  @Consumes(APPLICATION_JSON)
  public Response setUpObtainRegistrationChallenge(JsonObject jsonObject){

    LOG.info("Returning registration challenge....");
    LOG.info(jsonObject);

    return authClient.setUpObtainRegistrationChallenge(jsonObject);
  }
  @POST
  @Path("/register")
  public Response register( @FormParam("userName") String userName,@FormParam("role") String role,@BeanParam WebAuthnRegisterResponse webAuthnResponse){
    return authClient.register(userName,role,webAuthnResponse);
  }

  @POST
  @Path("/q/webauthn/login")
  @Consumes(APPLICATION_JSON)
  public Response setUpObtainLoginChallenge(JsonObject jsonObject) {
    return authClient.setUpObtainLoginChallenge(jsonObject);
  }

  @POST
  @Path("/q/webauthn/callback")
  public Response callback(JsonObject jsonObject) {
    return authClient.callback(jsonObject);
  }

  @GET
  @Path("/q/webauthn/logout")
  public Response logout() {
    return authClient.logout();
  }

  @GET
  @Path("/me")
  @Produces(MediaType.TEXT_PLAIN)
  public String me(@Context SecurityContext securityContext) {
    return authClient.me(securityContext);
  }

  @GET
  @Path("/check-cookie")
  public Response checkCookie(){ return authClient.verify();}

}
