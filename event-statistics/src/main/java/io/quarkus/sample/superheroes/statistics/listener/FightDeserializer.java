package io.quarkus.sample.superheroes.statistics.listener;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.sample.superheroes.statistics.domain.Fight;

/**
 * {@link ObjectMapperDeserializer} class that will receive the Kafka records and create the {@link Fight} instances.
 */
public class FightDeserializer extends ObjectMapperDeserializer<Fight> {
	public FightDeserializer() {
		super(Fight.class);
	}
}
