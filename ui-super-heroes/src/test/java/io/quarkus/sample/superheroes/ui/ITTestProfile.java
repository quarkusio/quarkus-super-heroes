package io.quarkus.sample.superheroes.ui;

import java.util.HashMap;
import java.util.Map;

import io.quarkiverse.quinoa.testing.QuinoaTestProfiles;

public class ITTestProfile extends QuinoaTestProfiles.Enable {
	@Override
	public Map<String, String> getConfigOverrides() {
		var configOverrides = new HashMap<>(super.getConfigOverrides());
		configOverrides.put("api.base.url", "http://${quarkus.microcks.default.http.host}:${quarkus.microcks.default.http.port}/rest/Fights+API/1.0");

		return configOverrides;
	}
}
