package io.quarkus.sample.superheroes.auth.rest;

import io.quarkiverse.openfga.client.AuthorizationModelClient;
import io.quarkiverse.openfga.client.StoreClient;
import io.quarkiverse.openfga.client.model.TupleKey;
import io.quarkiverse.zanzibar.annotations.FGARelation;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;


import io.quarkus.sample.superheroes.auth.webauthn.User;

import io.quarkus.sample.superheroes.auth.webauthn.WebAuthnCredential;

import io.quarkus.security.webauthn.WebAuthnRegisterResponse;

import io.quarkus.security.webauthn.WebAuthnSecurity;

import io.smallrye.mutiny.Uni;

import io.vertx.ext.auth.webauthn.Authenticator;
import io.vertx.ext.web.RoutingContext;

import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import jakarta.ws.rs.core.Response.Status;

import jakarta.ws.rs.core.SecurityContext;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import java.security.Principal;

@Path("")
public class AuthResourceReactive {
  private static final Logger LOG = Logger.getLogger(AuthResourceReactive.class);
  @Inject
  WebAuthnSecurity webAuthnSecurity;
  @Inject
  StoreClient storeClient;
  @Inject
  AuthorizationModelClient defaultAuthModelClient;
  @Path("/register")
  @POST
  @FGARelation(FGARelation.ANY)
  @WithTransaction
  public Uni<Response> register(@RestForm String userName,@RestForm String plan, @BeanParam WebAuthnRegisterResponse webAuthnResponse, RoutingContext ctx) {
    // Input validation
    if (userName == null || userName.isEmpty() || !webAuthnResponse.isSet() || !webAuthnResponse.isValid()) {
      return Uni.createFrom().item(Response.status(Status.BAD_REQUEST).build());
    }

    Uni<User> userUni = User.findByUserName(userName);
    return userUni.flatMap(user -> {
      if (user != null) {
        // Duplicate user
        return Uni.createFrom().item(Response.status(Status.BAD_REQUEST).build());
      }
      Uni<Authenticator> authenticator = this.webAuthnSecurity.register(webAuthnResponse, ctx);

      return authenticator
        // store the user
        .flatMap(auth -> {
          User newUser = new User();
          newUser.userName = auth.getUserName();
          newUser.plan = plan;
          WebAuthnCredential credential = new WebAuthnCredential(auth, newUser);
          return credential.persist().flatMap(c -> newUser.<User>persist());

        })
        .flatMap(newUser -> {
          var authModelClient = storeClient.authorizationModels().defaultModel();
          LOG.info(authModelClient);
          return authModelClient.write(TupleKey.of("plan:" + newUser.plan, "subscriber", "user:" + newUser.userName))
            .onItem().transformToUni(voidreturn -> {
            // make a login cookie
            this.webAuthnSecurity.rememberUser(newUser.userName, ctx);
            return Uni.createFrom().item(Response.ok().build());
          });
        })
        // handle login failure
        .onFailure().recoverWithItem(x -> {
          // make a proper error response
          LOG.info(x);
          return Response.status(Status.BAD_REQUEST).build();
        });

  });

  }

    @GET
    @Path("/verify-session")
    @FGARelation(FGARelation.ANY)
    public Uni<Response> verify(@Context SecurityContext securityContext, RoutingContext ctx){
      return Uni.createFrom().item(securityContext.getUserPrincipal())
        .onItem().ifNotNull().transform(principal -> {
          LOG.info("User session validated");
          return Response.ok().build();
        })
        .onItem().ifNull().continueWith(() -> {
          LOG.info("Invalid user session detected");
          return Response.status(Status.UNAUTHORIZED).build();
        });
    }

    @GET
    @Path("/feature-access/{feature}")
    @FGARelation(FGARelation.ANY)
    public Uni<Boolean> checkFeatureAccess(@RestPath String feature,@Context SecurityContext securityContext, RoutingContext ctx) {
      return Uni.createFrom().item(securityContext.getUserPrincipal()).onItem().ifNotNull().transformToUni(principal -> {
        return User.findByUserName(principal.getName()).onItem().ifNotNull().transformToUni(user -> {
          return defaultAuthModelClient.check(TupleKey.of("feature:" + feature, "can_access", "user:" + user.userName), null);
        }
        );
      })
        .onItem().ifNull().continueWith(false)
        .onFailure().recoverWithItem(false);
    }
}
