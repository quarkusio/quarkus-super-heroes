package io.quarkus.workshop.superheroes.fight.client;

import io.quarkus.test.Mock;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;

@Mock
@ApplicationScoped
@RestClient
public class MockVillainProxy implements VillainProxy {

    public static final String DEFAULT_VILLAIN_NAME = "Super Chocolatine";
    public static final String DEFAULT_VILLAIN_PICTURE = "super_chocolatine.png";
    public static final String DEFAULT_VILLAIN_POWERS = "does not eat pain au chocolat";
    public static final int DEFAULT_VILLAIN_LEVEL = 42;

    @Override
    public Villain findRandomVillain() {
        Villain villain = new Villain();
        villain.name = DEFAULT_VILLAIN_NAME;
        villain.picture = DEFAULT_VILLAIN_PICTURE;
        villain.powers = DEFAULT_VILLAIN_POWERS;
        villain.level = DEFAULT_VILLAIN_LEVEL;
        return villain;
    }
}
