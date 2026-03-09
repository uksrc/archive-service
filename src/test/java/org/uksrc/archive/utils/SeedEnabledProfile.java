package org.uksrc.archive.utils;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * This implementation of the QuarkusTestProfile interface overrides configuration properties
 * to enable test data seeding during testing. The configuration property "testdata.seed.enabled"
 * is explicitly set to "true", allowing tests to leverage seeded data where applicable.
 * Used so that testing this one process does not affect other tests which are based on a specific number
 * of resources.
 * <p>
 * The {@code SeedEnabledProfile} class is typically used to ensure consistent test behaviour
 * when test scenarios rely on pre-seeded datasets.
 */
public class SeedEnabledProfile implements QuarkusTestProfile {

    //Make sure that the seed resources are in place for only the tests that need them.
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("testdata.seed.enabled", "true");
    }
}
