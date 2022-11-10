The [Pact](https://pact.io) contracts in this directory would generally **NOT** be checked into source control like this. Instead, they would likely be pushed to a [Pact Broker](https://docs.pact.io/pact_broker) and then automatically discovered in the provider verification tests.

One of the main goals of the Superheroes application is to be super simple and just "work" by anyone who may clone this repo. That being said, we can't make any assumptions about where a Pact broker may be or any of the credentials required to access it.

It would be simple enough to modify [`ContractVerificationTests.java`](../../java/io/quarkus/sample/superheroes/villain/ContractVerificationTests.java) to use a Pact broker:
1. Remove the `@PactFolder("pacts")` annotation
2. Remove the `@EnabledIfSystemProperty` annotation
    - This is only here because, currently, pact contract verification does not work in Quarkus dev mode/continuous testing, but does work if running the Maven's ```test`/`verify` goals.
3. Place a `@PactBroker` annotation with a `url` parameter containing the location of the Pact broker.
4. When you run the tests, specify the `pactbroker.auth.token` property so that the test can authenticate with the broker.
5. _Optionally_, you could also add an `@EnabledIfSystemProperty(named = "pactbroker.auth.token", matches = ".+", disabledReason = "pactbroker.auth.token system property not set")` annotation.
    - This would make sure that the verification tests **ONLY** ran if the `pactbroker.auth.token` property was set.
    - If you run the verification tests and don't provide the token, the tests will fail.
