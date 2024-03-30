package io.quarkus.sample.superheroes.auth.webauthn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.webauthn.AttestationCertificates;
import io.vertx.ext.auth.webauthn.Authenticator;
import jakarta.transaction.Transactional;

import static io.quarkus.sample.superheroes.auth.webauthn.WebAuthnCredential.*;


@ApplicationScoped
public class WebAuthnSetup implements WebAuthnUserProvider {

  @ReactiveTransactional
  @Override
  public Uni<List<Authenticator>> findWebAuthnCredentialsByUserName(String userName) {
    return WebAuthnCredential.findByUserName(userName)
      .flatMap(WebAuthnSetup::toAuthenticators);
  }

  @ReactiveTransactional
  @Override
  public Uni<List<Authenticator>> findWebAuthnCredentialsByCredID(String credID) {
    return WebAuthnCredential.findByCredID(credID)
      .flatMap(WebAuthnSetup::toAuthenticators);
  }

  @ReactiveTransactional
  @Override
  public Uni<Void> updateOrStoreWebAuthnCredentials(Authenticator authenticator) {return Uni.createFrom().nullItem();}
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
