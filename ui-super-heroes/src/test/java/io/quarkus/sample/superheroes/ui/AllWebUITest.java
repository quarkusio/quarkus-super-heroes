package io.quarkus.sample.superheroes.ui;

import io.quarkiverse.quinoa.testing.QuinoaTestProfiles;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(QuinoaTestProfiles.EnableAndRunTests.class)
public class AllWebUITest {
  @Test
  public void runTest() {
    // we don't need anything here, it will run the package.json "test"
  }
}
