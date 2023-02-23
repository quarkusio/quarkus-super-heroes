package io.quarkus.sample.superheroes.hero.service;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.hero.Hero;
import io.quarkus.sample.superheroes.hero.mapping.HeroFullUpdateMapper;
import io.quarkus.sample.superheroes.hero.mapping.HeroPartialUpdateMapper;
import io.quarkus.sample.superheroes.hero.repository.HeroRepository;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Service class containing business methods for the application.
 */
@ApplicationScoped
public class HeroService {
  private final HeroRepository heroRepository;
  private final Validator validator;
  private final HeroPartialUpdateMapper heroPartialUpdateMapper;
  private final HeroFullUpdateMapper heroFullUpdateMapper;

  public HeroService(HeroRepository heroRepository, Validator validator, HeroPartialUpdateMapper heroPartialUpdateMapper, HeroFullUpdateMapper heroFullUpdateMapper) {
    this.heroRepository = heroRepository;
    this.validator = validator;
    this.heroPartialUpdateMapper = heroPartialUpdateMapper;
    this.heroFullUpdateMapper = heroFullUpdateMapper;
  }

  @WithSpan("HeroService.findAllHeroes")
  public Uni<List<Hero>> findAllHeroes() {
    Log.debug("Getting all heroes");
    return this.heroRepository.listAll();
  }

  @WithSpan("HeroService.findAllHeroesHavingName")
  public Uni<List<Hero>> findAllHeroesHavingName(@SpanAttribute("arg.name") String name) {
    Log.debugf("Finding all heroes having name = %s", name);
    return this.heroRepository.listAllWhereNameLike(name);
  }

  @WithSpan("HeroService.findHeroById")
  public Uni<Hero> findHeroById(@SpanAttribute("arg.id") Long id) {
    Log.debugf("Finding hero by id = %d", id);
    return this.heroRepository.findById(id);
  }

  @WithSpan("HeroService.findRandomHero")
  public Uni<Hero> findRandomHero() {
    Log.debug("Finding a random hero");
    return this.heroRepository.findRandom();
  }

  @WithSpan("HeroService.persistHero")
  @WithTransaction
  public Uni<Hero> persistHero(@SpanAttribute("arg.hero") @NotNull @Valid Hero hero) {
    Log.debugf("Persisting hero: %s", hero);
    return this.heroRepository.persist(hero);
  }

  @WithSpan("HeroService.replaceHero")
  @WithTransaction
  public Uni<Hero> replaceHero(@SpanAttribute("arg.hero") @NotNull @Valid Hero hero) {
    Log.debugf("Replacing hero: %s", hero);
    return this.heroRepository.findById(hero.getId())
      .onItem().ifNotNull().transform(h -> {
        this.heroFullUpdateMapper.mapFullUpdate(hero, h);
        return h;
      });
  }

  @WithSpan("HeroService.partialUpdateHero")
  @WithTransaction
  public Uni<Hero> partialUpdateHero(@SpanAttribute("arg.hero") @NotNull Hero hero) {
    Log.infof("Partially updating hero: %s", hero);
    return this.heroRepository.findById(hero.getId())
      .onItem().ifNotNull().transform(h -> {
        this.heroPartialUpdateMapper.mapPartialUpdate(hero, h);
        return h;
      })
      .onItem().ifNotNull().transform(this::validatePartialUpdate);
  }

  @WithSpan("HeroService.replaceAllHeroes")
  @WithTransaction
  public Uni<Void> replaceAllHeroes(@SpanAttribute("arg.heroes") List<Hero> heroes) {
    Log.debug("Replacing all heroes");
    return deleteAllHeroes()
      .replaceWith(this.heroRepository.persist(heroes));
  }

  /**
   * Validates a {@link Hero} for partial update according to annotation validation rules on the {@link Hero} object.
   * @param hero The {@link Hero}
   * @return The same {@link Hero} that was passed in, assuming it passes validation. The return is used as a convenience so the method can be called in a functional pipeline.
   * @throws ConstraintViolationException If validation fails
   */
  private Hero validatePartialUpdate(Hero hero) {
    var violations = this.validator.validate(hero);

    if ((violations != null) && !violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }

    return hero;
  }

  @WithSpan("HeroService.deleteAllHeroes")
  @WithTransaction
  public Uni<Void> deleteAllHeroes() {
    Log.debug("Deleting all heroes");
    return this.heroRepository.listAll()
      .onItem().transformToMulti(list -> Multi.createFrom().iterable(list))
      .map(Hero::getId)
      .onItem().transformToUniAndMerge(this::deleteHero)
      .collect().asList()
      .replaceWithVoid();
  }

  @WithSpan("HeroService.deleteHero")
  @WithTransaction
  public Uni<Void> deleteHero(@SpanAttribute("arg.id") Long id) {
    Log.debugf("Deleting hero by id = %d", id);
    return this.heroRepository.deleteById(id).replaceWithVoid();
  }
}
