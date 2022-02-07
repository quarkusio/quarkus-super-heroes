package io.quarkus.sample.superheroes.fight.mapping;

import org.mapstruct.Mapper;

import io.quarkus.sample.superheroes.fight.Fight;

@Mapper(componentModel = "cdi")
public interface FightMapper {
  /**
   * Maps all fields from {@code fight} to a {@link io.quarkus.sample.superheroes.fight.schema.Fight}
   * @param fight
   * @return
   */
  io.quarkus.sample.superheroes.fight.schema.Fight toSchema(Fight fight);
}
