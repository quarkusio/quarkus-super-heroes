package io.quarkus.sample.superheroes.location

import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junit5.PluginTestTarget
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder
import io.mockk.every
import io.quarkiverse.test.junit.mockk.InjectSpy
import io.quarkus.sample.superheroes.location.repository.LocationRepository
import io.quarkus.test.junit.QuarkusTest
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith

@QuarkusTest
@Provider("grpc-locations")
@PactFolder("pacts")
// You could comment out the @PactFolder annotations
// if you'd like to use a Pact broker. You'd also un-comment the following 2 annotations
//@PactBroker(url = "https://quarkus-super-heroes.pactflow.io")
//@EnabledIfSystemProperty(named = "pactbroker.auth.token", matches = ".+", disabledReason = "pactbroker.auth.token system property not set")
class ContractVerificationTests(@ConfigProperty(name = "quarkus.grpc.server.test-port") val quarkusPort: Int) {
  @InjectSpy
	lateinit var locationRepository: LocationRepository

  companion object {
    const val NO_RANDOM_LOCATION_FOUND_STATE = "No random location found"

    @JvmStatic
    @PactBrokerConsumerVersionSelectors
    fun consumerVersionSelectors() =
      SelectorBuilder().branch(System.getProperty("pactbroker.consumer.branch", "main"))
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider::class)
  fun pactVerificationTestTemplate(context: PactVerificationContext) {
    context.verifyInteraction()
  }

  @BeforeEach
  fun beforeEach(context: PactVerificationContext) {
    context.target = PluginTestTarget(mutableMapOf(
      "host" to "localhost",
      "port" to quarkusPort,
      "transport" to "grpc"
    ))

    // Have to do this here because the CDI context doesn't seem to be available
    // in the @State method below
    val isNoRandomLocationFoundState = context
      .interaction
      .providerStates
      .find { it.name == NO_RANDOM_LOCATION_FOUND_STATE } != null

    if (isNoRandomLocationFoundState) {
      every { locationRepository.findRandom() } returns null
    }
  }

  @State(NO_RANDOM_LOCATION_FOUND_STATE)
  fun clearData() {}
}
