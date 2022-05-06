package io.quarkus.sample.superheroes.fight.client;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.jboss.resteasy.reactive.client.impl.UniInvoker;

import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.fight.config.FightConfig;

import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceInstance;

/**
 * Bean to be used for interacting with the Villain service.
 * <p>
 *   Uses the <a href="https://docs.oracle.com/javaee/7/tutorial/jaxrs-client001.htm">JAX-RS Rest Client</a> with the <a href="https://quarkus.io/guides/resteasy-reactive#resteasy-reactive-client">RESTEasy Reactive client</a>.
 * </p>
 */
@ApplicationScoped
public class VillainClient {
  private static final String STORK_PREFIX = "stork://";
  private static final String VILLAINS_API_PATH = "/api/villains";

	private final WebTargetProvider webTargetProvider;

	public VillainClient(FightConfig fightConfig) {
    var villainClientBaseUrl = fightConfig.villain().clientBaseUrl();

    if (villainClientBaseUrl.startsWith(STORK_PREFIX)) {
      this.webTargetProvider = new StorkWebTargetProvider(villainClientBaseUrl.replace(STORK_PREFIX, ""));
    }
    else {
      this.webTargetProvider = new DefaultWebTargetProvider(villainClientBaseUrl);
    }
	}

	/**
	 * Gets a Villain from the Villain service wrapped with a recovery on a {@code 404} error. Also wrapped in a {@link CircuitBreaker}.
	 * @return The Villain
	 */
	@CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 2, delayUnit = ChronoUnit.SECONDS)
	@CircuitBreakerName("findRandomVillain")
  CompletionStage<Villain> getRandomVillain() {
    // Want the 404 handling to be part of the circuit breaker
    // This means that the 404 responses aren't considered errors by the circuit breaker
    return this.webTargetProvider.getWebTarget("/random")
      .flatMap(webTarget ->
        webTarget.request(MediaType.APPLICATION_JSON_TYPE)
          .rx(UniInvoker.class)
          .get(Villain.class)
          .onFailure(Is404Exception.IS_404).recoverWithNull()
      )
      .subscribeAsCompletionStage();
  }

	/**
	 * Finds a random {@link Villain}. The retry logic is applied to the result of the {@link CircuitBreaker}, meaning that retries that return failures could trigger the breaker to open.
	 * @return A random {@link Villain}
	 */
	public Uni<Villain> findRandomVillain() {
		// The CompletionState is important so that on retry the Uni re-subscribes to a new
		// CompletionStage rather than the original one (which has already completed)
		return Uni.createFrom().completionStage(this::getRandomVillain)
			.onFailure().retry().withBackOff(Duration.ofMillis(200)).atMost(3);
	}

  /**
   * Calls hello on the Villains service.
   * @return A "hello" from Villains
   */
  public Uni<String> helloVillains() {
    return this.webTargetProvider.getWebTarget("/hello")
      .flatMap(webTarget ->
          webTarget.request(MediaType.TEXT_PLAIN_TYPE)
            .rx(UniInvoker.class)
            .get(String.class)
      );
  }

  private static abstract class WebTargetProvider {
    protected abstract Uni<WebTarget> getWebTarget(String path);
  }

  private static class DefaultWebTargetProvider extends WebTargetProvider {
    private final WebTarget webTarget;

    private DefaultWebTargetProvider(String baseUrl) {
      Log.debugf("Creating Default provider for baseUrl = %s", baseUrl);
      this.webTarget = ClientBuilder.newClient()
        .target(baseUrl)
        .path(VILLAINS_API_PATH);
    }

    @Override
    protected Uni<WebTarget> getWebTarget(String path) {
      return Uni.createFrom().item(this.webTarget.path(path));
    }
  }

  private static class StorkWebTargetProvider extends WebTargetProvider {
    private final Client villainClient = ClientBuilder.newClient();
    private final String storkServiceName;

    private StorkWebTargetProvider(String storkServiceName) {
      Log.debugf("Creating Stork provider for service name = %s", storkServiceName);
      this.storkServiceName = storkServiceName;
    }

    private Uni<ServiceInstance> getServiceInstance() {
      return Stork.getInstance()
        .getService(this.storkServiceName)
        .selectInstanceAndRecordStart(true);
    }

    private WebTarget createWebTarget(ServiceInstance serviceInstance, String path) {
      var url = String.format(
        "%s://%s:%d",
        serviceInstance.isSecure() ? "https" : "http",
        serviceInstance.getHost(),
        serviceInstance.getPort()
      );

      Log.debugf("Targeting Stork client for service with URL = %s", url);

      return this.villainClient.target(url)
        .path(VILLAINS_API_PATH)
        .path(path);
    }

    @Override
    protected Uni<WebTarget> getWebTarget(String path) {
      return getServiceInstance()
        .onItem().ifNotNull().transform(serviceInstance -> createWebTarget(serviceInstance, path))
        .onItem().ifNull().failWith(() -> new IllegalArgumentException(String.format("Can't determine a downstream service for service name '%s'. Is one configured?", this.storkServiceName)));
    }
  }
}
