package io.quarkus.sample.superheroes.ui;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

/**
 * Tests the resource layer ({@link EnvResource}).
 */
@QuarkusIntegrationTest
@TestProfile(ITTestProfile.class)
class EnvResourceIT extends EnvResourceTests {

}
