package io.quarkus.sample.superheroes.fight.client;


import io.quarkus.logging.Log;
import io.quarkus.security.UnauthorizedException;

import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.RestPath;

import java.time.temporal.ChronoUnit;

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
  @Timeout(value = 30, unit = ChronoUnit.SECONDS)
  @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
  @Fallback(value=fallbackVerify.class,skipOn =ClientWebApplicationException.class )
  Uni<Response> verify();

  @GET
  @Path("/feature-access/{feature}")
  @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 2, delayUnit = ChronoUnit.SECONDS)
  @CircuitBreakerName("checkFeatureAccess")
  @Timeout(value = 30, unit = ChronoUnit.SECONDS)
  @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
  @Fallback(fallbackCheckFeatureAccess.class)
  Uni<Boolean> checkFeatureAccess(@RestPath String feature);

  public static class fallbackVerify implements FallbackHandler<Uni<Response>> {
    private static final Uni<Response> FALLBACK_VERIFY_RESPONSE = Uni.createFrom().item(Response.ok().build());
    @Override
    public Uni<Response> handle(ExecutionContext context) {
      Throwable failure = context.getFailure();
      Log.info(failure.getClass());
      Log.info(failure.getMessage());
//      if (failure instanceof ClientWebApplicationException) {
//        return Uni.createFrom().item(Response.status(Status.UNAUTHORIZED).build());
//      }
      return FALLBACK_VERIFY_RESPONSE;
    }
  }
  public static class fallbackCheckFeatureAccess implements FallbackHandler<Uni<Boolean>> {
    private static final Uni<Boolean> FALLBACK_CHECK_FEATURE_ACCESS = Uni.createFrom().item(true);
    @Override
    public Uni<Boolean> handle(ExecutionContext context) {
      return FALLBACK_CHECK_FEATURE_ACCESS;
    }
  }
}
