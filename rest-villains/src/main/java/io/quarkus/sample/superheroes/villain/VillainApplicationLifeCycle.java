package io.quarkus.sample.superheroes.villain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;

/**
 * Demonstrates how to use Quarkus {@link StartupEvent}s and {@link ShutdownEvent}s as well as how to gain access to the {@link ProfileManager}.
 */
@ApplicationScoped
public class VillainApplicationLifeCycle {
	private static final Logger LOGGER = Logger.getLogger(VillainApplicationLifeCycle.class);

	void onStart(@Observes StartupEvent ev) {
		LOGGER.info(" __     ___ _ _       _             _    ____ ___ ");
		LOGGER.info(" \\ \\   / (_) | | __ _(_)_ __       / \\  |  _ \\_ _|");
		LOGGER.info("  \\ \\ / /| | | |/ _` | | '_ \\     / _ \\ | |_) | | ");
		LOGGER.info("   \\ V / | | | | (_| | | | | |   / ___ \\|  __/| | ");
		LOGGER.info("    \\_/  |_|_|_|\\__,_|_|_| |_|  /_/   \\_\\_|  |___|");
		LOGGER.info("The application VILLAIN is starting with profile " + ProfileManager.getActiveProfile());
	}

	void onStop(@Observes ShutdownEvent ev) {
		LOGGER.info("The application VILLAIN is stopping...");
	}
}
