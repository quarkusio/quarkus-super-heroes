package io.quarkus.sample.superheroes.hero.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.sample.superheroes.hero.Hero;
import io.quarkus.sample.superheroes.hero.mapping.HeroFullUpdateMapper;
import io.quarkus.sample.superheroes.hero.mapping.HeroPartialUpdateMapper;
import io.quarkus.sample.superheroes.hero.repository.HeroRepository;

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

	public Uni<List<Hero>> findAllHeroes() {
		return this.heroRepository.listAll();
	}

  public Uni<List<Hero>> findAllHeroesHavingName(String name) {
    return this.heroRepository.listAllWhereNameLike(name);
  }

	public Uni<Hero> findHeroById(Long id) {
		return this.heroRepository.findById(id);
	}

	public Uni<Hero> findRandomHero() {
		return this.heroRepository.findRandom();
	}

	@ReactiveTransactional
	public Uni<Hero> persistHero(@NotNull @Valid Hero hero) {
		return this.heroRepository.persist(hero);
	}

	@ReactiveTransactional
	public Uni<Hero> replaceHero(@NotNull @Valid Hero hero) {
		return this.heroRepository.findById(hero.getId())
			.onItem().ifNotNull().transform(h -> {
				this.heroFullUpdateMapper.mapFullUpdate(hero, h);
				return h;
			});
	}

	@ReactiveTransactional
	public Uni<Hero> partialUpdateHero(@NotNull Hero hero) {
		return this.heroRepository.findById(hero.getId())
			.onItem().ifNotNull().transform(h -> {
				this.heroPartialUpdateMapper.mapPartialUpdate(hero, h);
				return h;
			})
			.onItem().ifNotNull().transform(this::validatePartialUpdate);
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

	@ReactiveTransactional
	public Uni<Void> deleteAllHeroes() {
		return this.heroRepository.listAll()
			.onItem().transformToMulti(list -> Multi.createFrom().iterable(list))
			.map(Hero::getId)
			.onItem().transformToUniAndMerge(this::deleteHero)
			.collect().asList()
			.replaceWithVoid();
	}

	@ReactiveTransactional
	public Uni<Void> deleteHero(Long id) {
		return this.heroRepository.deleteById(id).replaceWithVoid();
	}
}
