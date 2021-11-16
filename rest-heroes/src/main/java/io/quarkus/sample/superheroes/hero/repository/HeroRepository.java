package io.quarkus.sample.superheroes.hero.repository;

import java.util.Random;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.sample.superheroes.hero.Hero;

import io.smallrye.mutiny.Uni;

/**
 * Repository class for managing data operations on a {@link Hero}.
 */
@ApplicationScoped
public class HeroRepository implements PanacheRepository<Hero> {
	public Uni<Hero> findRandom() {
		return count()
			.map(count -> (count > 0) ? count : null)
			.onItem().ifNotNull().transform(count -> new Random().nextInt(count.intValue()))
			.onItem().ifNotNull().transformToUni(randomHero -> findAll().page(randomHero, 1).firstResult());
	}
}
