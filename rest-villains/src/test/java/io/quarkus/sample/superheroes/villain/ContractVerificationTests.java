package io.quarkus.sample.superheroes.villain;

import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;

@QuarkusTest
@Provider("rest-villains")
@PactFolder("pacts")
// You could comment out the @PactFolder annotations
// if you'd like to use a Pact broker. You'd also un-comment the following 2 annotations
//@PactBroker(url = "https://quarkus-super-heroes.pactflow.io")
//@EnabledIfSystemProperty(named = "pactbroker.auth.token", matches = ".+", disabledReason = "pactbroker.auth.token system property not set")
public class ContractVerificationTests {
  private static final String NO_RANDOM_VILLAIN_FOUND_STATE = "No random villain found";

  @ConfigProperty(name = "quarkus.http.test-port")
  int quarkusPort;

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void pactVerificationTestTemplate(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @BeforeEach
  void beforeEach(PactVerificationContext context) {
    context.setTarget(new HttpTestTarget("localhost", this.quarkusPort));

    // Have to do this here because the CDI context doesn't seem to be available
    // in the @State method below
    var isNoRandomVillainFoundState = Optional.ofNullable(context.getInteraction().getProviderStates())
      .orElseGet(List::of)
      .stream()
      .filter(state -> NO_RANDOM_VILLAIN_FOUND_STATE.equals(state.getName()))
      .count() > 0;

    if (isNoRandomVillainFoundState) {
      PanacheMock.mock(Villain.class);

		  when(Villain.findRandom())
        .thenReturn(Optional.empty());
    }
  }

  @PactBrokerConsumerVersionSelectors
  public static SelectorBuilder consumerVersionSelectors() {
    return new SelectorBuilder()
      .branch(System.getProperty("pactbroker.consumer.branch", "main"));
  }

  @State(NO_RANDOM_VILLAIN_FOUND_STATE)
  public void clearData() {
    // Already handled in beforeEach
  }
}
