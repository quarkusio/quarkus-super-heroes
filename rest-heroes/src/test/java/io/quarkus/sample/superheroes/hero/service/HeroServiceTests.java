package io.quarkus.sample.superheroes.hero.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.ParameterizedTest.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.sample.superheroes.hero.Hero;
import io.quarkus.sample.superheroes.hero.mapping.HeroFullUpdateMapper;
import io.quarkus.sample.superheroes.hero.mapping.HeroPartialUpdateMapper;
import io.quarkus.sample.superheroes.hero.repository.HeroRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@QuarkusTest
class HeroServiceTests {
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
  private static final Long DEFAULT_ID = 1L;

  @Inject
  HeroService heroService;

  @InjectMock
  HeroRepository heroRepository;

  @InjectSpy
  HeroPartialUpdateMapper heroPartialUpdateMapper;

  @InjectSpy
  HeroFullUpdateMapper heroFullUpdateMapper;

  @Test
  public void findAllHeroesNoneFound() {
    when(this.heroRepository.listAll())
      .thenReturn(Uni.createFrom().item(List.of()));

    var allHeroes = this.heroService.findAllHeroes()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(allHeroes)
      .isNotNull()
      .isEmpty();

    verify(this.heroRepository).listAll();
    verifyNoMoreInteractions(this.heroRepository);
  }

  @Test
  public void findAllHeroes() {
    when(this.heroRepository.listAll())
      .thenReturn(Uni.createFrom().item(List.of(createDefaultHero())));

    var allHeroes = this.heroService.findAllHeroes()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(allHeroes)
      .isNotNull()
      .isNotEmpty()
      .singleElement()
      .extracting(
        Hero::getId,
        Hero::getName,
        Hero::getOtherName,
        Hero::getLevel,
        Hero::getPicture,
        Hero::getPowers
      )
      .containsExactly(
        DEFAULT_ID,
        DEFAULT_NAME,
        DEFAULT_OTHER_NAME,
        DEFAULT_LEVEL,
        DEFAULT_PICTURE,
        DEFAULT_POWERS
      );

    verify(this.heroRepository).listAll();
    verifyNoMoreInteractions(this.heroRepository);
  }

  @ParameterizedTest(name = DISPLAY_NAME_PLACEHOLDER + "[" + INDEX_PLACEHOLDER + "] (" + ARGUMENTS_WITH_NAMES_PLACEHOLDER + ")")
  @ValueSource(strings = { "name" })
  @NullSource
  public void findAllHeroesHavingNameNoneFound(String name) {
    when(this.heroRepository.listAllWhereNameLike(eq(name))).thenReturn(Uni.createFrom().item(List.of()));

    var allHeroes = this.heroService.findAllHeroesHavingName(name)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(allHeroes)
      .isNotNull()
      .isEmpty();

    verify(this.heroRepository).listAllWhereNameLike(eq(name));
    verifyNoMoreInteractions(this.heroRepository);
  }

  @Test
  public void findAllHeroesHavingName() {
    when(this.heroRepository.listAllWhereNameLike(eq("name"))).thenReturn(Uni.createFrom().item(List.of(createDefaultHero())));

    var allHeroes = this.heroService.findAllHeroesHavingName("name")
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(allHeroes)
      .isNotNull()
      .isNotEmpty()
      .hasSize(1)
      .extracting(
        Hero::getId,
        Hero::getName,
        Hero::getOtherName,
        Hero::getLevel,
        Hero::getPicture,
        Hero::getPowers
      )
      .containsExactly(
        tuple(
          DEFAULT_ID,
          DEFAULT_NAME,
          DEFAULT_OTHER_NAME,
          DEFAULT_LEVEL,
          DEFAULT_PICTURE,
          DEFAULT_POWERS
        )
      );

    verify(this.heroRepository).listAllWhereNameLike(eq("name"));
    verifyNoMoreInteractions(this.heroRepository);
  }

  @Test
  public void findHeroByIdFound() {
    when(this.heroRepository.findById(eq(DEFAULT_ID)))
      .thenReturn(Uni.createFrom().item(createDefaultHero()));

    var hero = this.heroService.findHeroById(DEFAULT_ID)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(hero)
      .isNotNull()
      .extracting(
        Hero::getId,
        Hero::getName,
        Hero::getOtherName,
        Hero::getLevel,
        Hero::getPicture,
        Hero::getPowers
      )
      .containsExactly(
        DEFAULT_ID,
        DEFAULT_NAME,
        DEFAULT_OTHER_NAME,
        DEFAULT_LEVEL,
        DEFAULT_PICTURE,
        DEFAULT_POWERS
      );

    verify(this.heroRepository).findById(eq(DEFAULT_ID));
    verifyNoMoreInteractions(this.heroRepository);
  }

  @Test
  public void findHeroByIdNotFound() {
    when(this.heroRepository.findById(eq(DEFAULT_ID)))
      .thenReturn(Uni.createFrom().nullItem());

    var hero = this.heroService.findHeroById(DEFAULT_ID)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(hero)
      .isNull();

    verify(this.heroRepository).findById(eq(DEFAULT_ID));
    verifyNoMoreInteractions(this.heroRepository);
  }

  @Test
  public void findRandomHeroNotFound() {
    when(this.heroRepository.findRandom())
      .thenReturn(Uni.createFrom().nullItem());

    var randomHero = this.heroService.findRandomHero()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(randomHero)
      .isNull();

    verify(this.heroRepository).findRandom();
    verifyNoMoreInteractions(this.heroRepository);
  }

  @Test
  public void findRandomHeroFound() {
    when(this.heroRepository.findRandom())
      .thenReturn(Uni.createFrom().item(createDefaultHero()));

    var randomHero = this.heroService.findRandomHero()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(randomHero)
      .isNotNull()
      .extracting(
        Hero::getId,
        Hero::getName,
        Hero::getOtherName,
        Hero::getLevel,
        Hero::getPicture,
        Hero::getPowers
      )
      .containsExactly(
        DEFAULT_ID,
        DEFAULT_NAME,
        DEFAULT_OTHER_NAME,
        DEFAULT_LEVEL,
        DEFAULT_PICTURE,
        DEFAULT_POWERS
      );

    verify(this.heroRepository).findRandom();
    verifyNoMoreInteractions(this.heroRepository);
  }

  @Test
  @RunOnVertxContext
  public void persistNullHero(UniAsserter asserter) {
    asserter.assertFailedWith(
      () -> this.heroService.persistHero(null),
      cve -> {
        assertThat(cve)
          .isNotNull()
          .isInstanceOf(ConstraintViolationException.class);

        var violations = ((ConstraintViolationException) cve).getConstraintViolations();

        assertThat(violations)
          .isNotNull()
          .singleElement()
          .isNotNull()
          .extracting(
            ConstraintViolation::getInvalidValue,
            ConstraintViolation::getMessage
          )
          .containsExactly(
            null,
            "must not be null"
          );

        verifyNoInteractions(this.heroRepository);
      }
    );
  }

  @Test
  @RunOnVertxContext
  public void persistInvalidHero(UniAsserter asserter) {
    var hero = createDefaultHero();
    hero.setName("a");

    asserter.assertFailedWith(
      () -> this.heroService.persistHero(hero),
      cve -> {
        assertThat(cve)
          .isNotNull()
          .isInstanceOf(ConstraintViolationException.class);

        var violations = ((ConstraintViolationException) cve).getConstraintViolations();

        assertThat(violations)
          .isNotNull()
          .singleElement()
          .isNotNull()
          .extracting(
            ConstraintViolation::getInvalidValue,
            ConstraintViolation::getMessage
          )
          .containsExactly(
            "a",
            "size must be between 3 and 50"
          );

        verifyNoInteractions(this.heroRepository);
      }
    );
  }

  @Test
  @RunOnVertxContext
  public void persistHero(UniAsserter asserter) {
    when(this.heroRepository.persist(any(Hero.class)))
      .thenReturn(Uni.createFrom().item(createDefaultHero()));

    var heroToPersist = createDefaultHero();
    heroToPersist.setId(null);

    asserter.assertThat(
      () -> this.heroService.persistHero(heroToPersist),
      persistedHero -> {
        assertThat(persistedHero)
          .isNotNull()
          .extracting(
            Hero::getId,
            Hero::getName,
            Hero::getOtherName,
            Hero::getLevel,
            Hero::getPicture,
            Hero::getPowers
          )
          .containsExactly(
            DEFAULT_ID,
            DEFAULT_NAME,
            DEFAULT_OTHER_NAME,
            DEFAULT_LEVEL,
            DEFAULT_PICTURE,
            DEFAULT_POWERS
          );

        verify(this.heroRepository).persist(any(Hero.class));
        verifyNoMoreInteractions(this.heroRepository);
      }
    );
  }

  @Test
  @RunOnVertxContext
  public void fullyUpdateNullHero(UniAsserter asserter) {
    asserter.assertFailedWith(
      () -> this.heroService.replaceHero(null),
      cve -> {
        assertThat(cve)
          .isNotNull()
          .isInstanceOf(ConstraintViolationException.class);

        var violations = ((ConstraintViolationException) cve).getConstraintViolations();

        assertThat(violations)
          .isNotNull()
          .singleElement()
          .isNotNull()
          .extracting(
            ConstraintViolation::getInvalidValue,
            ConstraintViolation::getMessage
          )
          .containsExactly(
            null,
            "must not be null"
          );

        verifyNoInteractions(this.heroRepository, this.heroFullUpdateMapper, this.heroPartialUpdateMapper);
      }
    );
  }

  @Test
  @RunOnVertxContext
  public void fullyUpdateInvalidHero(UniAsserter asserter) {
    var hero = createDefaultHero();
    hero.setName(null);

    asserter.assertFailedWith(
      () -> this.heroService.replaceHero(hero),
      cve -> {
        assertThat(cve)
          .isNotNull()
          .isInstanceOf(ConstraintViolationException.class);

        var violations = ((ConstraintViolationException) cve).getConstraintViolations();

        assertThat(violations)
          .isNotNull()
          .singleElement()
          .isNotNull()
          .extracting(
            ConstraintViolation::getInvalidValue,
            ConstraintViolation::getMessage
          )
          .containsExactly(
            null,
            "must not be null"
          );

        verifyNoInteractions(this.heroRepository, this.heroFullUpdateMapper, this.heroPartialUpdateMapper);
      }
    );
  }

  @Test
  @RunOnVertxContext
  public void fullyUpdateNotFoundHero(UniAsserter asserter) {
    when(this.heroRepository.findById(eq(DEFAULT_ID)))
      .thenReturn(Uni.createFrom().nullItem());

    asserter.assertThat(
      () -> this.heroService.replaceHero(createUpdatedHero()),
      hero -> {
        assertThat(hero)
          .isNull();

        verify(this.heroRepository).findById(eq(DEFAULT_ID));
        verifyNoMoreInteractions(this.heroRepository);
        verifyNoInteractions(this.heroPartialUpdateMapper, this.heroFullUpdateMapper);
      }
    );
  }

  @Test
  @RunOnVertxContext
  public void fullyUpdateHero(UniAsserter asserter) {
    when(this.heroRepository.findById(eq(DEFAULT_ID)))
      .thenReturn(Uni.createFrom().item(createDefaultHero()));

    asserter.assertThat(
      () -> this.heroService.replaceHero(createUpdatedHero()),
      replacedHero -> {
        assertThat(replacedHero)
          .isNotNull()
          .extracting(
            Hero::getId,
            Hero::getName,
            Hero::getOtherName,
            Hero::getLevel,
            Hero::getPicture,
            Hero::getPowers
          )
          .containsExactly(
            DEFAULT_ID,
            UPDATED_NAME,
            UPDATED_OTHER_NAME,
            UPDATED_LEVEL,
            UPDATED_PICTURE,
            UPDATED_POWERS
          );

        verify(this.heroRepository).findById(eq(DEFAULT_ID));
        verifyNoMoreInteractions(this.heroRepository);
        verify(this.heroFullUpdateMapper).mapFullUpdate(any(Hero.class), any(Hero.class));
        verifyNoInteractions(this.heroPartialUpdateMapper);
      }
    );
  }

  @Test
  @RunOnVertxContext
  public void partiallyUpdateNullHero(UniAsserter asserter) {
    asserter.assertFailedWith(
      () -> this.heroService.partialUpdateHero(null),
      cve -> {
        assertThat(cve)
          .isNotNull()
          .isInstanceOf(ConstraintViolationException.class);

        var violations = ((ConstraintViolationException) cve).getConstraintViolations();

        assertThat(violations)
          .isNotNull()
          .singleElement()
          .isNotNull()
          .extracting(
            ConstraintViolation::getInvalidValue,
            ConstraintViolation::getMessage
          )
          .containsExactly(
            null,
            "must not be null"
          );

        verifyNoInteractions(this.heroRepository, this.heroFullUpdateMapper, this.heroPartialUpdateMapper);
      }
    );
  }

  @Test
  @RunOnVertxContext
  public void partiallyUpdateInvalidHero(UniAsserter asserter) {
    when(this.heroRepository.findById(eq(DEFAULT_ID)))
      .thenReturn(Uni.createFrom().item(createDefaultHero()));

    var hero = createDefaultHero();
    hero.setName("a");

    asserter.assertFailedWith(
      () -> this.heroService.partialUpdateHero(hero),
      cve -> {
        assertThat(cve)
          .isNotNull()
          .isInstanceOf(ConstraintViolationException.class);

        var violations = ((ConstraintViolationException) cve).getConstraintViolations();

        assertThat(violations)
          .isNotNull()
          .singleElement()
          .isNotNull()
          .extracting(
            ConstraintViolation::getInvalidValue,
            ConstraintViolation::getMessage
          )
          .containsExactly(
            "a",
            "size must be between 3 and 50"
          );

        verify(this.heroRepository).findById(eq(DEFAULT_ID));
        verifyNoMoreInteractions(this.heroRepository);
        verify(this.heroPartialUpdateMapper).mapPartialUpdate(any(Hero.class), any(Hero.class));
        verifyNoInteractions(this.heroFullUpdateMapper);
      }
    );
  }

  @Test
  @RunOnVertxContext
  public void partiallyUpdateNotFoundHero(UniAsserter asserter) {
    when(this.heroRepository.findById(eq(DEFAULT_ID)))
      .thenReturn(Uni.createFrom().nullItem());

    asserter.assertThat(
      () -> this.heroService.partialUpdateHero(createPartialUpdatedHero()),
      hero -> {
        assertThat(hero)
          .isNull();

        verify(this.heroRepository).findById(eq(DEFAULT_ID));
        verifyNoMoreInteractions(this.heroRepository);
        verifyNoInteractions(this.heroFullUpdateMapper, this.heroPartialUpdateMapper);
      }
    );
  }

  @Test
  @RunOnVertxContext
  public void partiallyUpdateHero(UniAsserter asserter) {
    when(this.heroRepository.findById(eq(DEFAULT_ID)))
      .thenReturn(Uni.createFrom().item(createDefaultHero()));

    asserter.assertThat(
      () -> this.heroService.partialUpdateHero(createPartialUpdatedHero()),
      hero -> {
        assertThat(hero)
          .isNotNull()
          .extracting(
            Hero::getId,
            Hero::getName,
            Hero::getOtherName,
            Hero::getLevel,
            Hero::getPicture,
            Hero::getPowers
          )
          .containsExactly(
            DEFAULT_ID,
            DEFAULT_NAME,
            DEFAULT_OTHER_NAME,
            DEFAULT_LEVEL,
            UPDATED_PICTURE,
            UPDATED_POWERS
          );

        verify(this.heroRepository).findById(eq(DEFAULT_ID));
        verifyNoMoreInteractions(this.heroRepository);
        verify(this.heroPartialUpdateMapper).mapPartialUpdate(any(Hero.class), any(Hero.class));
        verifyNoInteractions(this.heroFullUpdateMapper);
      }
    );
  }

  @Test
  @RunOnVertxContext
  public void deleteHero(UniAsserter asserter) {
    when(this.heroRepository.deleteById(eq(DEFAULT_ID)))
      .thenReturn(Uni.createFrom().item(true));

    asserter.assertThat(
      () -> this.heroService.deleteHero(DEFAULT_ID),
      v -> {
        verify(this.heroRepository).deleteById(eq(DEFAULT_ID));
        verifyNoMoreInteractions(this.heroRepository);
      }
    );
  }

  @Test
  @RunOnVertxContext
  public void deleteAllHeroes(UniAsserter asserter) {
    var h1 = createDefaultHero();
    var h2 = createUpdatedHero();
    h2.setId(h1.getId() + 1);

    when(this.heroRepository.deleteById(anyLong()))
      .thenReturn(Uni.createFrom().item(true));

    when(this.heroRepository.listAll())
      .thenReturn(Uni.createFrom().item(List.of(h1, h2)));

    asserter.assertThat(
      () -> this.heroService.deleteAllHeroes(),
      v -> {
        verify(this.heroRepository).listAll();
        verify(this.heroRepository).deleteById(eq(h1.getId()));
        verify(this.heroRepository).deleteById(eq(h2.getId()));
        verifyNoMoreInteractions(this.heroRepository);
      }
    );
  }

  @Test
  @RunOnVertxContext
  public void replaceAllHeroes(UniAsserter asserter) {
    var h1 = createDefaultHero();
    var h2 = createUpdatedHero();
    h2.setId(h1.getId() + 1);

    var heroes = List.of(createDefaultHero(), createPartialUpdatedHero());
    heroes.forEach(h -> h.setId(null));

    when(this.heroRepository.deleteById(anyLong()))
      .thenReturn(Uni.createFrom().item(true));

    when(this.heroRepository.listAll())
      .thenReturn(Uni.createFrom().item(List.of(h1, h2)));

    when(this.heroRepository.persist(anyIterable()))
      .thenReturn(Uni.createFrom().voidItem());

    asserter.assertThat(
      () -> this.heroService.replaceAllHeroes(heroes),
      v -> {
        verify(this.heroRepository).listAll();
        verify(this.heroRepository).deleteById(eq(h1.getId()));
        verify(this.heroRepository).deleteById(eq(h2.getId()));
        verify(this.heroRepository).persist(anyIterable());
        verifyNoMoreInteractions(this.heroRepository);
      }
    );
  }

  private static Hero createDefaultHero() {
    Hero hero = new Hero();
    hero.setId(DEFAULT_ID);
    hero.setName(DEFAULT_NAME);
    hero.setOtherName(DEFAULT_OTHER_NAME);
    hero.setPicture(DEFAULT_PICTURE);
    hero.setPowers(DEFAULT_POWERS);
    hero.setLevel(DEFAULT_LEVEL);

    return hero;
  }

  public static Hero createUpdatedHero() {
    Hero hero = createDefaultHero();
    hero.setName(UPDATED_NAME);
    hero.setOtherName(UPDATED_OTHER_NAME);
    hero.setPicture(UPDATED_PICTURE);
    hero.setPowers(UPDATED_POWERS);
    hero.setLevel(UPDATED_LEVEL);

    return hero;
  }

  public static Hero createPartialUpdatedHero() {
    Hero hero = createDefaultHero();
    hero.setPicture(UPDATED_PICTURE);
    hero.setPowers(UPDATED_POWERS);

    return hero;
  }
}
