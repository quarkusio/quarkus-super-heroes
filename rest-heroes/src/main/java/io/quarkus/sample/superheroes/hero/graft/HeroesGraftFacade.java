package io.quarkus.sample.superheroes.hero.graft;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.sample.superheroes.hero.model.HeroDto;
import io.quarkus.sample.superheroes.hero.model.HeroPatchDto;
import io.quarkus.sample.superheroes.hero.service.HeroService;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;

import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.ContextInternal;
import io.vertx.mutiny.core.Vertx;

/**
 * Intentional Graftcode callable surface for hero operations (sync POJOs, no HTTP or reactive types).
 */
@ApplicationScoped
public class HeroesGraftFacade {

  private final HeroService heroService;
  private final Vertx vertx;

  public HeroesGraftFacade(HeroService heroService, Vertx vertx) {
    this.heroService = heroService;
    this.vertx = vertx;
  }

  /** Ping hello (same text as former {@code GET /api/heroes/hello}). */
  public String hello() {
    return HeroService.HELLO_MESSAGE;
  }

  /** Returns a random hero, or empty if the database has no heroes. */
  public Optional<HeroDto> getRandomHero() {
    return awaitOnVertx(() -> heroService.findRandomHeroApi());
  }

  /**
   * Returns all heroes, optionally filtered by name (case-insensitive substring match).
   *
   * @param nameFilter optional filter; {@code null} returns all heroes
   */
  public List<HeroDto> getAllHeroes(String nameFilter) {
    return awaitOnVertx(() -> heroService.listHeroes(nameFilter));
  }

  /** Returns a hero by id, or empty if not found. */
  public Optional<HeroDto> getHero(Long id) {
    return awaitOnVertx(() -> heroService.findHeroApiById(id));
  }

  /** Persists a new hero and returns its generated id. */
  public Long createHero(HeroDto hero) {
    return awaitOnVertx(() -> heroService.createHero(hero)).getId();
  }

  /**
   * Fully replaces an existing hero.
   *
   * @return {@code true} if the hero existed and was updated, {@code false} if not found
   */
  public boolean replaceHero(Long id, HeroDto hero) {
    return awaitOnVertx(() -> heroService.replaceHero(id, hero));
  }

  /**
   * Partially updates a hero.
   *
   * @return the updated hero, or empty if not found
   */
  public Optional<HeroDto> partialUpdateHero(Long id, HeroPatchDto patch) {
    return awaitOnVertx(() -> heroService.partialUpdateHero(id, patch));
  }

  /** Deletes a hero by id (no-op if id does not exist). */
  public void deleteHero(Long id) {
    awaitOnVertx(() -> heroService.deleteHero(id));
  }

  /** Deletes all heroes. */
  public void deleteAllHeroes() {
    awaitOnVertx(() -> heroService.deleteAllHeroes());
  }

  /** Replaces the entire hero table with the given list. */
  public void replaceAllHeroes(List<HeroDto> heroes) {
    awaitOnVertx(() -> heroService.replaceAllHeroesFromDtos(heroes));
  }

  private <T> T awaitOnVertx(Supplier<Uni<T>> uniSupplier) {
    var result = new CompletableFuture<T>();
    var duplicated = ((ContextInternal) vertx.getDelegate().getOrCreateContext()).duplicate();
    duplicated.runOnContext(v -> {
      VertxContextSafetyToggle.setCurrentContextSafe(true);
      uniSupplier.get().subscribe().with(result::complete, result::completeExceptionally);
    });

    try {
      return result.get(2, TimeUnit.MINUTES);
    } catch (Exception e) {
      var cause = e.getCause() != null ? e.getCause() : e;
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException(cause);
    }
  }
}
