package io.quarkus.sample.superheroes.auth.service;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import io.quarkus.sample.superheroes.auth.webauthn.User;

import io.quarkus.sample.superheroes.auth.webauthn.WebAuthnCredential;

import io.smallrye.mutiny.Uni;

import io.vertx.ext.auth.webauthn.Authenticator;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthService {
  @WithTransaction
  public Uni<User> findUserByUserName(String userName){
    return User.findByUserName(userName);
  }
  @WithTransaction
  public Uni<User> persistCredentialAndUser(Authenticator authenticator,String userName, String plan ){
    User newUser = new User();
    newUser.userName = authenticator.getUserName();
    newUser.plan = plan;
    WebAuthnCredential credential = new WebAuthnCredential(authenticator, newUser);
    return credential.persist().flatMap(c -> newUser.<User>persist());
  }

}
