package io.quarkus.sample.superheroes.auth.webauthn;


import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import io.smallrye.mutiny.Uni;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.Set;

@Table(name = "user_table")
@Entity
public class User extends PanacheEntity {

  @Column(unique = true)
  public String userName;

  @Column
  public String plan;


  // non-owning side, so we can add more credentials later
  @OneToOne(mappedBy = "user")
  public WebAuthnCredential webAuthnCredential;

  public static Uni<User> findByUserName(String userName) {
    return find("userName", userName).firstResult();
  }
}
