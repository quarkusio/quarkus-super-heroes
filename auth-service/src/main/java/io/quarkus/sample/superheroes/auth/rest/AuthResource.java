package io.quarkus.sample.superheroes.auth.rest;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.auth.webauthn.User;

import io.quarkus.sample.superheroes.auth.webauthn.WebAuthnCredential;

import io.quarkus.security.webauthn.WebAuthnRegisterResponse;
import io.quarkus.security.webauthn.WebAuthnSecurity;

import io.vertx.ext.auth.webauthn.Authenticator;
import io.vertx.ext.web.RoutingContext;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;

import org.jboss.resteasy.reactive.RestForm;

import java.security.Principal;
import java.util.HashSet;

import org.jboss.logging.Logger;

@Path("/")
public class AuthResource {
  private static final Logger LOG = Logger.getLogger(AuthResource.class);
  @Inject
  WebAuthnSecurity webAuthnSecurity;
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String publicResource() {
    return "public";
  }
  @Path("/register")
  @POST
  @Transactional
  public Response register(@RestForm String userName, @RestForm String role,
    @BeanParam WebAuthnRegisterResponse webAuthnResponse,
    RoutingContext ctx) {
    ctx.request().cookies();
    LOG.info(userName);
    LOG.info(role);
    LOG.info(webAuthnResponse.isValid());
    LOG.info(webAuthnResponse.isSet());

    // Input validation
    if(userName == null || userName.isEmpty() || !webAuthnResponse.isSet() || !webAuthnResponse.isValid()) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    User user = User.findByUserName(userName);
    if(user != null) {
      LOG.info("duplicate user found");
      // Duplicate user
      return Response.status(Status.BAD_REQUEST).build();
    }
    try {
      // store the user
      Authenticator authenticator = this.webAuthnSecurity.register(webAuthnResponse, ctx).await().indefinitely();
      LOG.info(authenticator.getUserName());
      User newUser = new User();
      newUser.userName = authenticator.getUserName();
      if (newUser.role == null) {
        newUser.role = new HashSet<>();
      }
      newUser.role.add(role);
      WebAuthnCredential credential = new WebAuthnCredential(authenticator, newUser);

      newUser.persist();
      credential.persist();

      // make a login cookie

      this.webAuthnSecurity.rememberUser(newUser.userName, ctx);
      return Response.ok().build();
    } catch (Exception ignored) {
      LOG.info("Something has gone terribly wrong");
      LOG.info(ignored.getMessage());
      // handle login failure
      // make a proper error response
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  @GET
  @Path("/verify-session")
  public Response verify(@Context SecurityContext securityContext, RoutingContext ctx){

    LOG.info( "THE USERNAME OF THE PERSON IS.....  "+ securityContext.getUserPrincipal().getName());
    return Response.ok().build();
  }

  @GET
  @Path("/me")
  @Produces(MediaType.TEXT_PLAIN)
  public String me(@Context SecurityContext securityContext) {
    Principal user = securityContext.getUserPrincipal();
    return user != null ? user.getName() : "<not logged in>";
  }



}
