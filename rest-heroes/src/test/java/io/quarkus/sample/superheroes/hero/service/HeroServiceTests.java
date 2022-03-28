package io.quarkus.sample.superheroes.hero.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.ParameterizedTest.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.List;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.sample.superheroes.hero.Hero;
import io.quarkus.sample.superheroes.hero.mapping.HeroFullUpdateMapper;
import io.quarkus.sample.superheroes.hero.mapping.HeroPartialUpdateMapper;
import io.quarkus.sample.superheroes.hero.repository.HeroRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.junit.mockito.InjectSpy;

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
		when(this.heroRepository.listAll()).thenReturn(Uni.createFrom().item(List.of()));

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
		when(this.heroRepository.listAll()).thenReturn(Uni.createFrom().item(List.of(createDefaultHero())));

		var allHeroes = this.heroService.findAllHeroes()
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
		when(this.heroRepository.findById(eq(DEFAULT_ID))).thenReturn(Uni.createFrom().nullItem());

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
		when(this.heroRepository.findRandom()).thenReturn(Uni.createFrom().nullItem());

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
		when(this.heroRepository.findRandom()).thenReturn(Uni.createFrom().item(createDefaultHero()));

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
	public void persistNullHero() {
		var cve = (ConstraintViolationException) this.heroService.persistHero(null)
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitFailure(Duration.ofSeconds(5))
			.assertFailedWith(ConstraintViolationException.class)
			.getFailure();

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.hasSize(1);

		assertThat(violations.stream().findFirst())
			.isNotNull()
			.isPresent()
			.get()
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

	@Test
	public void persistInvalidHero() {
		var hero = createDefaultHero();
		hero.setName("a");

		var cve = (ConstraintViolationException) this.heroService.persistHero(hero)
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitFailure(Duration.ofSeconds(5))
			.assertFailedWith(ConstraintViolationException.class)
			.getFailure();

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.hasSize(1);

		assertThat(violations.stream().findFirst())
			.isNotNull()
			.isPresent()
			.get()
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

	@Test
	public void persistHero() {
		when(this.heroRepository.persist(any(Hero.class))).thenReturn(Uni.createFrom().item(createDefaultHero()));

		var heroToPersist = createDefaultHero();
		heroToPersist.setId(null);

		var persistedHero = this.heroService.persistHero(heroToPersist)
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

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

	@Test
	public void fullyUpdateNullHero() {
		var cve = (ConstraintViolationException) this.heroService.replaceHero(null)
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitFailure(Duration.ofSeconds(5))
			.assertFailedWith(ConstraintViolationException.class)
			.getFailure();

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.hasSize(1);

		assertThat(violations.stream().findFirst())
			.isNotNull()
			.isPresent()
			.get()
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

	@Test
	public void fullyUpdateInvalidHero() {
		var hero = createDefaultHero();
		hero.setName(null);

		var cve = (ConstraintViolationException) this.heroService.replaceHero(hero)
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitFailure(Duration.ofSeconds(5))
			.assertFailedWith(ConstraintViolationException.class)
			.getFailure();

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.hasSize(1);

		assertThat(violations.stream().findFirst())
			.isNotNull()
			.isPresent()
			.get()
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

	@Test
	public void fullyUpdateNotFoundHero() {
		when(this.heroRepository.findById(eq(DEFAULT_ID))).thenReturn(Uni.createFrom().nullItem());

		var hero = this.heroService.replaceHero(createUpdatedHero())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(hero)
			.isNull();

		verify(this.heroRepository).findById(eq(DEFAULT_ID));
		verifyNoMoreInteractions(this.heroRepository);
		verifyNoInteractions(this.heroPartialUpdateMapper, this.heroFullUpdateMapper);
	}

	@Test
	public void fullyUpdateHero() {
		when(this.heroRepository.findById(eq(DEFAULT_ID))).thenReturn(Uni.createFrom().item(createDefaultHero()));

		var replacedHero = this.heroService.replaceHero(createUpdatedHero())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

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

	@Test
	public void partiallyUpdateNullHero() {
		var cve = (ConstraintViolationException) this.heroService.partialUpdateHero(null)
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitFailure(Duration.ofSeconds(5))
			.assertFailedWith(ConstraintViolationException.class)
			.getFailure();

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.hasSize(1);

		assertThat(violations.stream().findFirst())
			.isNotNull()
			.isPresent()
			.get()
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

	@Test
	public void partiallyUpdateInvalidHero() {
		when(this.heroRepository.findById(eq(DEFAULT_ID))).thenReturn(Uni.createFrom().item(createDefaultHero()));
		var hero = createDefaultHero();
		hero.setName("a");

		var cve = (ConstraintViolationException) this.heroService.partialUpdateHero(hero)
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitFailure(Duration.ofSeconds(5))
			.assertFailedWith(ConstraintViolationException.class)
			.getFailure();

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.hasSize(1);

		assertThat(violations.stream().findFirst())
			.isNotNull()
			.isPresent()
			.get()
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

	@Test
	public void partiallyUpdateNotFoundHero() {
		when(this.heroRepository.findById(eq(DEFAULT_ID))).thenReturn(Uni.createFrom().nullItem());

		var hero = this.heroService.partialUpdateHero(createPartialUpdatedHero())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(hero)
			.isNull();

		verify(this.heroRepository).findById(eq(DEFAULT_ID));
		verifyNoMoreInteractions(this.heroRepository);
		verifyNoInteractions(this.heroFullUpdateMapper, this.heroPartialUpdateMapper);
	}

	@Test
	public void partiallyUpdateHero() {
		when(this.heroRepository.findById(eq(DEFAULT_ID))).thenReturn(Uni.createFrom().item(createDefaultHero()));

		var hero = this.heroService.partialUpdateHero(createPartialUpdatedHero())
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
				UPDATED_PICTURE,
				UPDATED_POWERS
			);

		verify(this.heroRepository).findById(eq(DEFAULT_ID));
		verifyNoMoreInteractions(this.heroRepository);
		verify(this.heroPartialUpdateMapper).mapPartialUpdate(any(Hero.class), any(Hero.class));
		verifyNoInteractions(this.heroFullUpdateMapper);
	}

	@Test
	public void deleteHero() {
		when(this.heroRepository.deleteById(eq(DEFAULT_ID))).thenReturn(Uni.createFrom().item(true));

		this.heroService.deleteHero(DEFAULT_ID)
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5));

		verify(this.heroRepository).deleteById(eq(DEFAULT_ID));
		verifyNoMoreInteractions(this.heroRepository);
	}

	@Test
	public void deleteAllHeroes() {
		var h1 = createDefaultHero();
		var h2 = createUpdatedHero();
		h2.setId(h1.getId() + 1);

		when(this.heroRepository.deleteById(anyLong())).thenReturn(Uni.createFrom().item(true));
		when(this.heroRepository.listAll()).thenReturn(Uni.createFrom().item(List.of(h1, h2)));

		this.heroService.deleteAllHeroes()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		verify(this.heroRepository).listAll();
		verify(this.heroRepository).deleteById(eq(h1.getId()));
		verify(this.heroRepository).deleteById(eq(h2.getId()));
		verifyNoMoreInteractions(this.heroRepository);
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
