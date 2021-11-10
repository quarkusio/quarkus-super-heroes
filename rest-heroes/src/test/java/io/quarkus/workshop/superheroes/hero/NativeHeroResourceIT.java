package io.quarkus.workshop.superheroes.hero;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
public class NativeHeroResourceIT extends HeroResourceTest {

    // Execute the same tests but in native mode.
}