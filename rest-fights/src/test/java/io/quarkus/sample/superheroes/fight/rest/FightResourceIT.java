package io.quarkus.sample.superheroes.fight.rest;

import io.quarkus.sample.superheroes.fight.HeroesVillainsWiremockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@QuarkusTestResource(HeroesVillainsWiremockServer.class)
public class FightResourceIT {
}
