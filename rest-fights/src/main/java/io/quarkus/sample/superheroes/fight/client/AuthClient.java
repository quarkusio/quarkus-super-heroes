//package io.quarkus.sample.superheroes.fight.client;
//
//import io.smallrye.mutiny.Uni;
//
//import io.vertx.ext.web.RoutingContext;
//
//import jakarta.enterprise.context.ApplicationScoped;
//
//import jakarta.ws.rs.BeanParam;
//import jakarta.ws.rs.GET;
//import jakarta.ws.rs.POST;
//import jakarta.ws.rs.Path;
//import jakarta.ws.rs.Produces;
//import jakarta.ws.rs.core.Context;
//import jakarta.ws.rs.core.Response;
//
//import jakarta.ws.rs.core.SecurityContext;
//
//import org.eclipse.microprofile.rest.client.inject.RestClient;
//import org.jboss.resteasy.reactive.RestForm;
//
//import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
//
//@ApplicationScoped
//public class AuthClient {
//
//  private final AuthRestClient authClient;
//  public AuthClient(@RestClient AuthRestClient authClient) {
//    this.authClient = authClient;
//  }
//
//  public Response javascript() {
//    return this.authClient.javascript();
//  }
//
//  public Response setUpObtainRegistrationChallenge() {return this.authClient.setUpObtainRegistrationChallenge();}
//
//  public Response register(@RestForm String userName, @RestForm String role,
//    @BeanParam WebAuthnRegisterResponse webAuthnResponse,
//    RoutingContext ctx){return this.authClient.register( userName, role, webAuthnResponse,ctx);}
//  public Response setUpObtainLoginChallenge(){return this.authClient.setUpObtainLoginChallenge();}
//
//  public Response callback() {return this.authClient.callback();}
//
//  public Response logout(){return this.authClient.logout();}
//
//  public String me(@Context SecurityContext securityContext){return this.authClient.me(securityContext);}
//
//
//}
