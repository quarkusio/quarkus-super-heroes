package io.quarkus.sample.superheroes.statistics.listener;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.sample.superheroes.statistics.domain.Fight;

public class FightDeserializer extends ObjectMapperDeserializer<Fight> {
	public FightDeserializer() {
		super(Fight.class);
	}
}
