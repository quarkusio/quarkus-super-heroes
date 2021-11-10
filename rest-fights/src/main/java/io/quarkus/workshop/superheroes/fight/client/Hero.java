package io.quarkus.workshop.superheroes.fight.client;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import javax.validation.constraints.NotNull;

@Schema(description="The hero fighting against the villain")
public class Hero {

    @NotNull
    public String name;
    @NotNull
    public int level;
    @NotNull
    public String picture;
    public String powers;

}
