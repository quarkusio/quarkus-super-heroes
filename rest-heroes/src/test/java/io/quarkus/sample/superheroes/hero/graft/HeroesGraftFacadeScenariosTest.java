package io.quarkus.sample.superheroes.hero.graft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.sample.superheroes.hero.model.HeroDto;
import io.quarkus.sample.superheroes.hero.model.HeroPatchDto;
import io.quarkus.sample.superheroes.hero.service.HeroService;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
class HeroesGraftFacadeScenariosTest {

  private static final int DEFAULT_ORDER = 0;
  private static final String DEFAULT_NAME = "Super Chocolatine";
  private static final String UPDATED_NAME = DEFAULT_NAME + " (updated)";
  private static final String DEFAULT_OTHER_NAME = "Super Chocolatine chocolate in";
  private static final String UPDATED_OTHER_NAME = DEFAULT_OTHER_NAME + " (updated)";
  private static final String DEFAULT_PICTURE = "super_chocolatine.png";
  private static final String UPDATED_PICTURE = "super_chocolatine_updated.png";
  private static final String DEFAULT_POWERS = "does not eat pain au chocolat";
  private static final String UPDATED_POWERS = DEFAULT_POWERS + " (updated)";
  private static final int DEFAULT_LEVEL = 42;
  private static final int UPDATED_LEVEL = DEFAULT_LEVEL + 1;

  private static final int NB_HEROES = 100;
  private static Long heroId;

  @Inject
  HeroesGraftFacade facade;

  @Test
  @Order(DEFAULT_ORDER)
  void hello() {
    assertThat(facade.hello()).isEqualTo(HeroService.HELLO_MESSAGE);
  }

  @Test
  @Order(DEFAULT_ORDER)
  void shouldNotGetUnknownHero() {
    assertThat(facade.getHero(new Random().nextLong())).isEmpty();
  }

  @Test
  @Order(DEFAULT_ORDER)
  void shouldGetRandomHeroFound() {
    assertThat(facade.getRandomHero()).isPresent();
  }

  @Test
  @Order(DEFAULT_ORDER)
  void shouldNotAddInvalidItem() {
    var hero = new HeroDto()
      .setName(null)
      .setOtherName(DEFAULT_OTHER_NAME)
      .setPicture(DEFAULT_PICTURE)
      .setPowers(DEFAULT_POWERS)
      .setLevel(0);

    assertThatThrownBy(() -> facade.createHero(hero))
      .isInstanceOf(ConstraintViolationException.class);
  }

  @Test
  @Order(DEFAULT_ORDER)
  void shouldNotFullyUpdateInvalidItem() {
    var hero = new HeroDto()
      .setId(1L)
      .setName(null)
      .setOtherName(UPDATED_OTHER_NAME)
      .setPicture(UPDATED_PICTURE)
      .setPowers(UPDATED_PICTURE)
      .setLevel(0);

    assertThatThrownBy(() -> facade.replaceHero(hero.getId(), hero))
      .isInstanceOf(ConstraintViolationException.class);
  }

  @Test
  @Order(DEFAULT_ORDER)
  void shouldNotPartiallyUpdateInvalidItem() {
    var patch = new HeroPatchDto()
      .setId(50L)
      .setName(null)
      .setOtherName(UPDATED_OTHER_NAME)
      .setPicture(UPDATED_PICTURE)
      .setPowers(UPDATED_PICTURE)
      .setLevel(0);

    assertThatThrownBy(() -> facade.partialUpdateHero(patch.getId(), patch))
      .isInstanceOf(ConstraintViolationException.class);
  }

  @Test
  @Order(DEFAULT_ORDER)
  void shouldNotFullyUpdateNotFoundItem() {
    var hero = new HeroDto()
      .setId(-1L)
      .setName(UPDATED_NAME)
      .setOtherName(UPDATED_OTHER_NAME)
      .setPicture(UPDATED_PICTURE)
      .setPowers(UPDATED_POWERS)
      .setLevel(UPDATED_LEVEL);

    assertThat(facade.replaceHero(-1L, hero)).isFalse();
  }

  @Test
  @Order(DEFAULT_ORDER)
  void shouldNotPartiallyUpdateNotFoundItem() {
    var patch = new HeroPatchDto()
      .setPicture(DEFAULT_PICTURE)
      .setPowers(DEFAULT_POWERS);

    assertThat(facade.partialUpdateHero(-1L, patch)).isEmpty();
  }

  @Test
  @Order(DEFAULT_ORDER)
  void shouldNotGetAnyHeroesThatDontMatchFilterCriteria() {
    assertThat(facade.getAllHeroes("iooi90904890358349 8890re9ierkjlk;sdf098w459idxflkjdfjoiio4ue"))
      .isEmpty();
  }

  @Test
  @Order(DEFAULT_ORDER)
  void shouldGetHeroesThatMatchFilterCriteria() {
    var heroes = facade.getAllHeroes("spid");

    assertThat(heroes)
      .hasSize(2)
      .extracting(HeroDto::getName)
      .containsExactly("Spider-Man", "Spidey");
  }

  @Test
  @Order(DEFAULT_ORDER + 1)
  void shouldGetInitialItems() {
    assertThat(facade.getAllHeroes(null)).hasSize(NB_HEROES);
  }

  @Test
  @Order(DEFAULT_ORDER + 2)
  void shouldAddAnItem() {
    var hero = createDefaultHeroDto();

    heroId = facade.createHero(hero);
    assertThat(heroId).isNotNull();

    var returnedHero = facade.getHero(heroId).orElseThrow();
    assertThat(returnedHero)
      .usingRecursiveComparison()
      .ignoringFields("id")
      .isEqualTo(hero);

    assertThat(facade.getAllHeroes(null)).hasSize(NB_HEROES + 1);
  }

  @Test
  @Order(DEFAULT_ORDER + 3)
  void shouldFullyUpdateAnItem() {
    var hero = new HeroDto()
      .setId(heroId)
      .setName(UPDATED_NAME)
      .setOtherName(UPDATED_OTHER_NAME)
      .setPicture(UPDATED_PICTURE)
      .setPowers(UPDATED_POWERS)
      .setLevel(UPDATED_LEVEL);

    assertThat(facade.replaceHero(heroId, hero)).isTrue();
    assertThat(facade.getAllHeroes(null)).hasSize(NB_HEROES + 1);

    var updatedHero = facade.getHero(heroId).orElseThrow();
    assertThat(updatedHero)
      .usingRecursiveComparison()
      .isEqualTo(hero);
  }

  @Test
  @Order(DEFAULT_ORDER + 4)
  void shouldPartiallyUpdateAnItem() {
    var patch = new HeroPatchDto()
      .setPicture(DEFAULT_PICTURE)
      .setPowers(DEFAULT_POWERS);

    var expectedHero = new HeroDto()
      .setName(UPDATED_NAME)
      .setOtherName(UPDATED_OTHER_NAME)
      .setPicture(patch.getPicture())
      .setPowers(patch.getPowers())
      .setLevel(UPDATED_LEVEL)
      .setId(heroId);

    var patchedHero = facade.partialUpdateHero(heroId, patch).orElseThrow();

    assertThat(patchedHero)
      .usingRecursiveComparison()
      .isEqualTo(expectedHero);

    assertThat(facade.getAllHeroes(null)).hasSize(NB_HEROES + 1);
  }

  @Test
  @Order(DEFAULT_ORDER + 5)
  void shouldDeleteHero() {
    facade.deleteHero(heroId);
    assertThat(facade.getAllHeroes(null)).hasSize(NB_HEROES);
  }

  @Test
  @Order(DEFAULT_ORDER + 6)
  void shouldDeleteAllHeros() {
    facade.deleteAllHeroes();
    assertThat(facade.getAllHeroes(null)).isEmpty();
  }

  @Test
  @Order(DEFAULT_ORDER + 7)
  void shouldGetRandomHeroNotFound() {
    assertThat(facade.getRandomHero()).isEmpty();
  }

  @Test
  @Order(DEFAULT_ORDER + 8)
  void shouldReplaceAllHeroes() {
    var h1 = createDefaultHeroDto();
    var h2 = createUpdatedHeroDto();

    facade.createHero(h1);
    assertThat(facade.getAllHeroes(null)).hasSize(1);

    facade.replaceAllHeroes(List.of(h1, h2));

    var heroes = facade.getAllHeroes(null);
    assertThat(heroes)
      .hasSize(2)
      .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
      .containsExactly(createDefaultHeroDto(), createUpdatedHeroDto());
  }

  private static HeroDto createDefaultHeroDto() {
    return new HeroDto()
      .setName(DEFAULT_NAME)
      .setOtherName(DEFAULT_OTHER_NAME)
      .setLevel(DEFAULT_LEVEL)
      .setPicture(DEFAULT_PICTURE)
      .setPowers(DEFAULT_POWERS);
  }

  private static HeroDto createUpdatedHeroDto() {
    return new HeroDto()
      .setName(UPDATED_NAME)
      .setOtherName(UPDATED_OTHER_NAME)
      .setLevel(UPDATED_LEVEL)
      .setPicture(UPDATED_PICTURE)
      .setPowers(UPDATED_POWERS);
  }
}
