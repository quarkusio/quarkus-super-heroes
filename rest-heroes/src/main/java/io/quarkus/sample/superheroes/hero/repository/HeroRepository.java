package io.quarkus.sample.superheroes.hero.repository;

import java.util.List;
import java.util.Random;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.sample.superheroes.hero.Hero;

import io.smallrye.mutiny.Uni;

/**
 * Repository class for managing data operations on a {@link Hero}.
 */
@ApplicationScoped
@WithSession
public class HeroRepository implements PanacheRepository<Hero> {
	public Uni<Hero> findRandom() {
		return count()
			.map(count -> (count > 0) ? count : null)
			.onItem().ifNotNull().transform(count -> new Random().nextInt(count.intValue()))
			.onItem().ifNotNull().transformToUni(randomHero -> findAll().page(randomHero, 1).firstResult());
	}

  public Uni<List<Hero>> listAllWhereNameLike(String name) {
    return (name != null) ?
           list("LOWER(name) LIKE CONCAT('%', ?1, '%')", name.toLowerCase()) :
           Uni.createFrom().item(List::of);
  }
}
