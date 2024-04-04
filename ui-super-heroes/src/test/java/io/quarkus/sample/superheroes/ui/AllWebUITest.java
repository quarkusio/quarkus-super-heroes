package io.quarkus.sample.superheroes.ui;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import io.quarkiverse.quinoa.testing.QuinoaTestProfiles;

@QuarkusTest
@TestProfile(QuinoaTestProfiles.EnableAndRunTests.class)
class AllWebUITest {
  @Test
  void runTest() {
    // we don't need anything here, it will run the package.json "test"
  }
}
