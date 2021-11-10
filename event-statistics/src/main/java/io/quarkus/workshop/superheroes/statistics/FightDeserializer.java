package io.quarkus.workshop.superheroes.statistics;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class FightDeserializer extends ObjectMapperDeserializer<Fight> {

    public FightDeserializer() {
        super(Fight.class);
    }
}
