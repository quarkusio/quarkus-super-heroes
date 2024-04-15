package io.quarkus.sample.superheroes.auth.webauthn;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.auth.service.AuthService;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.webauthn.AttestationCertificates;
import io.vertx.ext.auth.webauthn.Authenticator;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.quarkus.sample.superheroes.auth.webauthn.WebAuthnCredential.*;


@ApplicationScoped
public class WebAuthnSetup implements WebAuthnUserProvider {
  @Inject
  AuthService authService;

  @WithTransaction
  @Override
  public Uni<List<Authenticator>> findWebAuthnCredentialsByUserName(String userName) {
    return WebAuthnCredential.findByUserName(userName)
      .flatMap(WebAuthnSetup::toAuthenticators);
  }

  @WithTransaction
  @Override
  public Uni<List<Authenticator>> findWebAuthnCredentialsByCredID(String credID) {
    return WebAuthnCredential.findByCredID(credID)
      .flatMap(WebAuthnSetup::toAuthenticators);
  }

  @WithTransaction
  @Override
  public Uni<Void> updateOrStoreWebAuthnCredentials(Authenticator authenticator) {
     //editing after reading this: https://w3c.github.io/webauthn/#sctn-sign-counter
    return authService.findUserByUserName(authenticator.getUserName())
      .onItem().transformToUni(user -> {
                if (user != null) {
                    // existing user
                    if(user.webAuthnCredential.counter<authenticator.getCounter()){
                      user.webAuthnCredential.counter = authenticator.getCounter();
                    }
                    else{
                      return Uni.createFrom().failure(new RuntimeException("Counter condition not met"));
                    }

                }
                return Uni.createFrom().nullItem();
            });
  }
  private static Uni<List<Authenticator>> toAuthenticators(List<WebAuthnCredential> dbs) {
    // can't call combine/uni on empty list
    if (dbs.isEmpty())
      return Uni.createFrom().item(Collections.emptyList());
    List<Uni<Authenticator>> ret = new ArrayList<>(dbs.size());
    for (WebAuthnCredential db : dbs) {
      ret.add(toAuthenticator(db));
    }
    return Uni.combine().all().unis(ret).combinedWith(f -> (List) f);
  }

  private static Uni<Authenticator> toAuthenticator(WebAuthnCredential credential) {
    return credential.fetch(credential.x5c)
      .map(x5c -> {
        Authenticator ret = new Authenticator();
        ret.setAaguid(credential.aaguid);
        AttestationCertificates attestationCertificates = new AttestationCertificates();
        attestationCertificates.setAlg(credential.alg);
        List<String> x5cs = new ArrayList<>(x5c.size());
        for (WebAuthnCertificate webAuthnCertificate : x5c) {
          x5cs.add(webAuthnCertificate.x5c);
        }
        ret.setAttestationCertificates(attestationCertificates);
        ret.setCounter(credential.counter);
        ret.setCredID(credential.credID);
        ret.setFmt(credential.fmt);
        ret.setPublicKey(credential.publicKey);
        ret.setType(credential.type);
        ret.setUserName(credential.userName);
        return ret;
      });
  }

  @Override
  public Set<String> getRoles(String userId) {
    if(userId.equals("admin")) {
      return Set.of("user", "admin");
    }
    return Collections.singleton("user");
  }
}
