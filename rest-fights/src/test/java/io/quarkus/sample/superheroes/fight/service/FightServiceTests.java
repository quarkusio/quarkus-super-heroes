package io.quarkus.sample.superheroes.fight.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.List;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.InternalServerErrorException;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.client.FightToNarrate;
import io.quarkus.sample.superheroes.fight.client.HeroClient;
import io.quarkus.sample.superheroes.fight.client.NarrationClient;
import io.quarkus.sample.superheroes.fight.client.VillainClient;
import io.quarkus.sample.superheroes.fight.mapping.FightMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;

/**
 * Tests for the service layer ({@link FightService}).
 * <p>
 *   Uses an {@link io.smallrye.reactive.messaging.memory.InMemoryConnector} to represent the Kafka instance.
 * </p>
 */
@QuarkusTest
class FightServiceTests extends FightServiceTestsBase {
  private static final String FIGHTS_CHANNEL_NAME = "fights";
  private static final String FALLBACK_NARRATION = """
                                                   High above a bustling city, a symbol of hope and justice soared through the sky, while chaos reigned below, with malevolent laughter echoing through the streets.
                                                   With unwavering determination, the figure swiftly descended, effortlessly evading explosive attacks, closing the gap, and delivering a decisive blow that silenced the wicked laughter.
                                                   
                                                   In the end, the battle concluded with a clear victory for the forces of good, as their commitment to peace triumphed over the chaos and villainy that had threatened the city.
                                                   The people knew that their protector had once again ensured their safety.
                                                   """;

	@InjectMock
  HeroClient heroClient;

	@InjectMock
  VillainClient villainClient;

  @InjectMock
  @RestClient
  NarrationClient narrationClient;

  @Inject
  @Any
  InMemoryConnector emitterConnector;

  @Inject
  FightMapper fightMapper;

  @BeforeEach
  public void beforeEach() {
    // Clear the emitter sink between tests
    this.emitterConnector.sink(FIGHTS_CHANNEL_NAME).clear();
  }

	@Test
	public void findAllFightsNoneFound() {
		PanacheMock.mock(Fight.class);
		when(Fight.listAll())
			.thenReturn(Uni.createFrom().item(List.of()));

		var allFights = this.fightService.findAllFights()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(allFights)
			.isNotNull()
			.isEmpty();

		PanacheMock.verify(Fight.class).listAll();
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	public void findAllFights() {
		PanacheMock.mock(Fight.class);
		when(Fight.listAll())
			.thenReturn(Uni.createFrom().item(List.of(createFightHeroWon())));

		var allFights = this.fightService.findAllFights()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(allFights)
			.isNotNull()
			.isNotEmpty()
			.hasSize(1)
			.extracting(
				"id",
				"fightDate",
				"winnerName",
				"winnerLevel",
				"winnerPicture",
        "winnerPowers",
				"loserName",
				"loserLevel",
				"loserPicture",
        "loserPowers",
				"winnerTeam",
				"loserTeam"
			)
			.containsExactly(
				tuple(
					DEFAULT_FIGHT_ID,
					DEFAULT_FIGHT_DATE,
					DEFAULT_HERO_NAME,
					DEFAULT_HERO_LEVEL,
					DEFAULT_HERO_PICTURE,
          DEFAULT_HERO_POWERS,
					DEFAULT_VILLAIN_NAME,
					DEFAULT_VILLAIN_LEVEL,
					DEFAULT_VILLAIN_PICTURE,
          DEFAULT_VILLAIN_POWERS,
					HEROES_TEAM_NAME,
					VILLAINS_TEAM_NAME
				)
			);

		PanacheMock.verify(Fight.class).listAll();
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	public void findFightByIdFound() {
		PanacheMock.mock(Fight.class);
		when(Fight.findById(eq(DEFAULT_FIGHT_ID)))
			.thenReturn(Uni.createFrom().item(createFightHeroWon()));

		var fight = this.fightService.findFightById(DEFAULT_FIGHT_ID.toString())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNotNull()
			.extracting(
				"id",
				"fightDate",
				"winnerName",
				"winnerLevel",
				"winnerPicture",
        "winnerPowers",
				"loserName",
				"loserLevel",
				"loserPicture",
        "loserPowers",
				"winnerTeam",
				"loserTeam"
			)
			.containsExactly(
				DEFAULT_FIGHT_ID,
				DEFAULT_FIGHT_DATE,
				DEFAULT_HERO_NAME,
				DEFAULT_HERO_LEVEL,
				DEFAULT_HERO_PICTURE,
        DEFAULT_HERO_POWERS,
				DEFAULT_VILLAIN_NAME,
				DEFAULT_VILLAIN_LEVEL,
				DEFAULT_VILLAIN_PICTURE,
        DEFAULT_VILLAIN_POWERS,
				HEROES_TEAM_NAME,
				VILLAINS_TEAM_NAME
			);

		PanacheMock.verify(Fight.class).findById(eq(DEFAULT_FIGHT_ID));
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	public void findFightByIdNotFound() {
		PanacheMock.mock(Fight.class);
		when(Fight.findById(eq(DEFAULT_FIGHT_ID)))
			.thenReturn(Uni.createFrom().nullItem());

		var fight = this.fightService.findFightById(DEFAULT_FIGHT_ID.toString())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNull();

		PanacheMock.verify(Fight.class).findById(eq(DEFAULT_FIGHT_ID));
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	public void findRandomFightersHeroError() {
		PanacheMock.mock(Fight.class);
		when(this.heroClient.findRandomHero())
			.thenReturn(Uni.createFrom().failure(InternalServerErrorException::new));

		when(this.villainClient.findRandomVillain())
			.thenReturn(Uni.createFrom().item(createDefaultVillain()));

		var fighters = this.fightService.findRandomFighters()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fighters)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(new Fighters(createFallbackHero(), createDefaultVillain()));

		verify(this.heroClient).findRandomHero();
		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
		verify(this.fightService).fallbackRandomHero();
		verify(this.fightService).addDelay(any(Uni.class));
		verify(this.fightService, never()).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void findRandomFightersVillainError() {
		PanacheMock.mock(Fight.class);
		when(this.heroClient.findRandomHero())
			.thenReturn(Uni.createFrom().item(createDefaultHero()));

		when(this.villainClient.findRandomVillain())
			.thenReturn(Uni.createFrom().failure(InternalServerErrorException::new));

		var fighters = this.fightService.findRandomFighters()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fighters)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(new Fighters(createDefaultHero(), createFallbackVillain()));

		verify(this.heroClient).findRandomHero();
		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
		verify(this.fightService, never()).fallbackRandomHero();
		verify(this.fightService).fallbackRandomVillain();
		verify(this.fightService).addDelay(any(Uni.class));
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void findRandomFightersHeroVillainError() {
		PanacheMock.mock(Fight.class);
		when(this.heroClient.findRandomHero())
			.thenReturn(Uni.createFrom().failure(InternalServerErrorException::new));

		when(this.villainClient.findRandomVillain())
			.thenReturn(Uni.createFrom().failure(InternalServerErrorException::new));

		var fighters = this.fightService.findRandomFighters()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fighters)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(new Fighters(createFallbackHero(), createFallbackVillain()));

		verify(this.heroClient).findRandomHero();
		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
		verify(this.fightService).fallbackRandomHero();
		verify(this.fightService).fallbackRandomVillain();
		verify(this.fightService).addDelay(any(Uni.class));
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void findRandomHeroDelayTriggersFallback() {
		PanacheMock.mock(Fight.class);
		when(this.heroClient.findRandomHero())
			.thenReturn(
				Uni.createFrom().item(createDefaultHero())
					.onItem()
					.delayIt().by(Duration.ofSeconds(3))
			);

		var hero = this.fightService.findRandomHero()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(hero)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(createFallbackHero());

		verify(this.heroClient).findRandomHero();
		verify(this.fightService).fallbackRandomHero();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void findRandomVillainDelayTriggersFallback() {
		PanacheMock.mock(Fight.class);
		when(this.villainClient.findRandomVillain())
			.thenReturn(
				Uni.createFrom().item(createDefaultVillain())
					.onItem()
					.delayIt().by(Duration.ofSeconds(3))
			);

		var villain = this.fightService.findRandomVillain()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(villain)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(createFallbackVillain());

		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void findRandomFightersAddDelayTriggersTimeout() {
		PanacheMock.mock(Fight.class);

		// Mock the addDelay method so it returns a 5 second delay on whatever argument (a Uni<Fighters>) was passed into it
		// This should trigger the timeout on findRandomFighters
		doAnswer(invocation -> ((Uni<Fighters>) invocation.getArgument(0)).onItem().delayIt().by(Duration.ofSeconds(5)))
			.when(this.fightService)
			.addDelay(any(Uni.class));

    var fighters = this.fightService.findRandomFighters()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

		assertThat(fighters)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(createFallbackFighters());

		verify(this.fightService).addDelay(any(Uni.class));
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
    verify(this.fightService).fallbackRandomFighters();
    verify(this.fightService, never()).fallbackRandomHero();
		verify(this.fightService, never()).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void findRandomFightersDelayedFindHeroAndVillainTriggersTimeout() {
		PanacheMock.mock(Fight.class);

		// Add a delay to the call to findRandomHero()
		doReturn(Uni.createFrom().item(createDefaultHero()).onItem().delayIt().by(Duration.ofSeconds(5)))
			.when(this.fightService)
			.findRandomHero();

		// Add a delay to the call to findRandomVillain
		doReturn(Uni.createFrom().item(createDefaultVillain()).onItem().delayIt().by(Duration.ofSeconds(5)))
			.when(this.fightService)
			.findRandomVillain();

		// The 2 delays should trigger the timeout on findRandomFighters
		var fighters = this.fightService.findRandomFighters()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

		assertThat(fighters)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(createFallbackFighters());

		verify(this.fightService).addDelay(any(Uni.class));
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
    verify(this.fightService).fallbackRandomFighters();
		verify(this.fightService, never()).fallbackRandomHero();
		verify(this.fightService, never()).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void performFightNullFighters() {
		PanacheMock.mock(Fight.class);

		var cve = catchThrowableOfType(
			() -> this.fightService.performFight(null),
			ConstraintViolationException.class);

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

		verify(this.fightService, never()).determineWinner(any(Fighters.class));
		verify(this.fightService, never()).shouldHeroWin(any(Fighters.class));
		verify(this.fightService, never()).shouldVillainWin(any(Fighters.class));
		verify(this.fightService, never()).getRandomWinner(any(Fighters.class));
		verify(this.fightService, never()).heroWonFight(any(Fighters.class));
		verify(this.fightService, never()).villainWonFight(any(Fighters.class));
		verify(this.fightService, never()).persistFight(any(Fight.class));
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void performFightInvalidFighters() {
		PanacheMock.mock(Fight.class);

		var cve = catchThrowableOfType(
			() -> this.fightService.performFight(new Fighters(null, createDefaultVillain())),
			ConstraintViolationException.class);

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

		verify(this.fightService, never()).determineWinner(any(Fighters.class));
		verify(this.fightService, never()).shouldHeroWin(any(Fighters.class));
		verify(this.fightService, never()).shouldVillainWin(any(Fighters.class));
		verify(this.fightService, never()).getRandomWinner(any(Fighters.class));
		verify(this.fightService, never()).heroWonFight(any(Fighters.class));
		verify(this.fightService, never()).villainWonFight(any(Fighters.class));
		verify(this.fightService, never()).persistFight(any(Fight.class));
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
		PanacheMock.verifyNoInteractions(Fight.class);
	}

  @Test
  public void narrateFightNarrationTimesOut() {
    var fightToNarrate = createFightToNarrateHeroWon();
    var fightToNarrateMatcher = fightToNarrateMatcher(fightToNarrate);

    when(this.narrationClient.narrate(argThat(fightToNarrateMatcher)))
      .thenReturn(
        Uni.createFrom().item(DEFAULT_NARRATION)
          .onItem().delayIt().by(Duration.ofSeconds(11)
          )
      );

    var narration = this.fightService.narrateFight(fightToNarrate)
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(45))
			.getItem();

    assertThat(narration)
      .isNotNull()
      .isEqualTo(FALLBACK_NARRATION);

    verify(this.fightService).fallbackNarrateFight(argThat(fightToNarrateMatcher));
  }

	@Test
	public void performFightHeroShouldWin() {
    var fightOutcome = createFightHeroWon();
		var fightMatcher = fightMatcher(fightOutcome);
		var fightersMatcher = fightersMatcher(createDefaultFighters());

		PanacheMock.mock(Fight.class);
		PanacheMock.doReturn(Uni.createFrom().voidItem())
			.when(Fight.class)
			.persist(argThat(fightMatcher), any());

		doReturn(true)
      .when(this.fightService)
      .shouldHeroWin(argThat(fightersMatcher));

		doReturn(fightOutcome)
      .when(this.fightService)
      .heroWonFight(argThat(fightersMatcher));

		var fight = this.fightService.performFight(createDefaultFighters())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNotNull()
      .usingRecursiveComparison()
			.isEqualTo(fightOutcome);

		var emittedMessages = this.emitterConnector.sink(FIGHTS_CHANNEL_NAME).received();

		assertThat(emittedMessages)
			.isNotNull()
			.singleElement()
			.extracting(Message::getPayload)
      .usingRecursiveComparison()
			.isEqualTo(this.fightMapper.toSchema(fightOutcome));

		verify(this.fightService).determineWinner(argThat(fightersMatcher));
		verify(this.fightService).persistFight(argThat(fightMatcher));
		verify(this.fightService).shouldHeroWin(argThat(fightersMatcher));
		verify(this.fightService).heroWonFight(argThat(fightersMatcher));
		verify(this.fightService, never()).shouldVillainWin(any(Fighters.class));
		verify(this.fightService, never()).villainWonFight(any(Fighters.class));
		verify(this.fightService, never()).getRandomWinner(any(Fighters.class));
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
    verify(this.fightService, never()).fallbackNarrateFight(any(FightToNarrate.class));
		PanacheMock.verify(Fight.class).persist(argThat(fightMatcher), any());
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	public void performFightVillainShouldWin() {
    var fightOutcome = createFightVillainWon();
    var fightMatcher = fightMatcher(fightOutcome);
		var fightersMatcher = fightersMatcher(createDefaultFighters());

		PanacheMock.mock(Fight.class);
		PanacheMock.doReturn(Uni.createFrom().voidItem())
			.when(Fight.class)
			.persist(argThat(fightMatcher), any());

		doReturn(false)
      .when(this.fightService)
      .shouldHeroWin(argThat(fightersMatcher));

		doReturn(true)
      .when(this.fightService)
      .shouldVillainWin(argThat(fightersMatcher));

		doReturn(fightOutcome)
      .when(this.fightService)
      .villainWonFight(argThat(fightersMatcher));

		var fight = this.fightService.performFight(createDefaultFighters())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(fightOutcome);

		var emittedMessages = this.emitterConnector.sink(FIGHTS_CHANNEL_NAME).received();

		assertThat(emittedMessages)
			.isNotNull()
			.singleElement()
			.extracting(Message::getPayload)
			.usingRecursiveComparison()
			.isEqualTo(this.fightMapper.toSchema(fightOutcome));

		verify(this.fightService).determineWinner(argThat(fightersMatcher));
		verify(this.fightService).persistFight(argThat(fightMatcher));
		verify(this.fightService).shouldVillainWin(argThat(fightersMatcher));
		verify(this.fightService).shouldHeroWin(argThat(fightersMatcher));
		verify(this.fightService).villainWonFight(argThat(fightersMatcher));
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
    verify(this.fightService, never()).fallbackNarrateFight(any(FightToNarrate.class));
		verify(this.fightService, never()).heroWonFight(any(Fighters.class));
		verify(this.fightService, never()).getRandomWinner(any(Fighters.class));
		PanacheMock.verify(Fight.class).persist(argThat(fightMatcher), any());
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	public void performFightRandomWinner() {
    var fightOutcome = createFightVillainWon();
    var fightMatcher = fightMatcher(fightOutcome);
		var fightersMatcher = fightersMatcher(createDefaultFighters());

		PanacheMock.mock(Fight.class);
		PanacheMock.doReturn(Uni.createFrom().voidItem())
			.when(Fight.class)
			.persist(argThat(fightMatcher), any());

		doReturn(false)
      .when(this.fightService)
      .shouldHeroWin(argThat(fightersMatcher));

		doReturn(false)
      .when(this.fightService)
      .shouldVillainWin(argThat(fightersMatcher));

		doReturn(fightOutcome)
      .when(this.fightService)
      .getRandomWinner(argThat(fightersMatcher));

		var fight = this.fightService.performFight(createDefaultFighters())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(fightOutcome);

		var emittedMessages = this.emitterConnector.sink(FIGHTS_CHANNEL_NAME).received();

		assertThat(emittedMessages)
			.isNotNull()
			.singleElement()
			.extracting(Message::getPayload)
			.usingRecursiveComparison()
			.isEqualTo(this.fightMapper.toSchema(fightOutcome));

		verify(this.fightService).determineWinner(argThat(fightersMatcher));
		verify(this.fightService).persistFight(argThat(fightMatcher));
		verify(this.fightService).shouldVillainWin(argThat(fightersMatcher));
		verify(this.fightService).shouldHeroWin(argThat(fightersMatcher));
		verify(this.fightService).getRandomWinner(argThat(fightersMatcher));
		verify(this.fightService, never()).villainWonFight(any(Fighters.class));
		verify(this.fightService, never()).heroWonFight(any(Fighters.class));
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
    verify(this.fightService, never()).fallbackNarrateFight(any(FightToNarrate.class));
		PanacheMock.verify(Fight.class).persist(argThat(fightMatcher), any());
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	public void didHeroWinTrue() {
		var fighters = createDefaultFighters();
		fighters.getHero().setLevel(Integer.MAX_VALUE - this.fightConfig.hero().adjustBound());
		fighters.getVillain().setLevel(Integer.MIN_VALUE);

		assertThat(this.fightService.shouldHeroWin(fighters))
			.isTrue();
	}

	@Test
	public void didHeroWinFalse() {
		var fighters = createDefaultFighters();
		fighters.getHero().setLevel(Integer.MIN_VALUE);
		fighters.getVillain().setLevel(Integer.MAX_VALUE- this.fightConfig.hero().adjustBound());

		assertThat(this.fightService.shouldHeroWin(fighters))
			.isFalse();
	}

	@Test
	public void didVillainWinTrue() {
		var fighters = createDefaultFighters();
		fighters.getHero().setLevel(Integer.MIN_VALUE);
		fighters.getVillain().setLevel(Integer.MAX_VALUE);

		assertThat(this.fightService.shouldVillainWin(fighters))
			.isTrue();
	}

	@Test
	public void didVillainWinFalse() {
		var fighters = createDefaultFighters();
		fighters.getHero().setLevel(Integer.MAX_VALUE);
		fighters.getVillain().setLevel(Integer.MIN_VALUE);

		assertThat(this.fightService.shouldVillainWin(fighters))
			.isFalse();
	}

  @Test
  public void helloHeroesFallback() {
    when(this.fightService.fallbackHelloHeroes())
	    .thenReturn(Uni.createFrom().item("fallback"));

    when(this.heroClient.helloHeroes())
	    .thenReturn(
				Uni.createFrom().item("hello")
					.onItem()
					.delayIt().by(Duration.ofSeconds(6))
	    );

    var message = this.fightService.helloHeroes()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo("fallback");

    verify(this.heroClient).helloHeroes();
    verify(this.fightService).helloHeroes();
    verify(this.fightService).fallbackHelloHeroes();
    verifyNoMoreInteractions(this.heroClient);
    verifyNoInteractions(this.villainClient);
  }

  @Test
  public void helloHeroesFailure() {
    when(this.fightService.fallbackHelloHeroes())
	    .thenReturn(Uni.createFrom().item("fallback"));

    when(this.heroClient.helloHeroes())
	    .thenReturn(Uni.createFrom().failure(InternalServerErrorException::new));

    var message = this.fightService.helloHeroes()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo("fallback");

    verify(this.heroClient).helloHeroes();
    verify(this.fightService).helloHeroes();
    verify(this.fightService).fallbackHelloHeroes();
    verifyNoMoreInteractions(this.heroClient);
    verifyNoInteractions(this.villainClient);
  }

  @Test
  public void helloVillainsFallback() {
    when(this.fightService.fallbackHelloVillains())
	    .thenReturn(Uni.createFrom().item("fallback"));

    when(this.villainClient.helloVillains())
	    .thenReturn(
				Uni.createFrom().item("hello")
					.onItem()
					.delayIt().by(Duration.ofSeconds(6))
	    );

    var message = this.fightService.helloVillains()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo("fallback");

    verify(this.villainClient).helloVillains();
    verify(this.fightService).helloVillains();
    verify(this.fightService).fallbackHelloVillains();
    verifyNoMoreInteractions(this.villainClient);
    verifyNoInteractions(this.heroClient);
  }

  @Test
  public void helloVillainsFailure() {
    when(this.fightService.fallbackHelloVillains())
	    .thenReturn(Uni.createFrom().item("fallback"));

    when(this.villainClient.helloVillains())
	    .thenReturn(Uni.createFrom().failure(InternalServerErrorException::new));

    var message = this.fightService.helloVillains()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo("fallback");

    verify(this.villainClient).helloVillains();
    verify(this.fightService).helloVillains();
    verify(this.fightService).fallbackHelloVillains();
    verifyNoMoreInteractions(this.villainClient);
    verifyNoInteractions(this.heroClient);
  }

  @Test
  public void helloNarrationFallback() {
    when(this.fightService.fallbackHelloNarration())
	    .thenReturn(Uni.createFrom().item("fallback"));

    when(this.narrationClient.hello())
      .thenReturn(
        Uni.createFrom().item("hello")
          .onItem()
          .delayIt().by(Duration.ofSeconds(6))
      );

    var message = this.fightService.helloNarration()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo("fallback");

    verify(this.narrationClient).hello();
    verify(this.fightService).helloNarration();
    verify(this.fightService).fallbackHelloNarration();
    verifyNoMoreInteractions(this.narrationClient);
  }

  @Test
  public void helloNarrationFailure() {
    when(this.fightService.fallbackHelloNarration())
	    .thenReturn(Uni.createFrom().item("fallback"));

    when(this.narrationClient.hello())
      .thenReturn(Uni.createFrom().failure(InternalServerErrorException::new));

    var message = this.fightService.helloNarration()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo("fallback");

    verify(this.narrationClient).hello();
    verify(this.fightService).helloNarration();
    verify(this.fightService).fallbackHelloNarration();
    verifyNoMoreInteractions(this.narrationClient);
  }

  private Fighters createFallbackFighters() {
    return new Fighters(createFallbackHero(), createFallbackVillain());
  }

  private static Fight createFightHeroWon() {
		var fight = new Fight();
		fight.id = DEFAULT_FIGHT_ID;
		fight.fightDate = DEFAULT_FIGHT_DATE;
		fight.winnerName = DEFAULT_HERO_NAME;
		fight.winnerLevel = DEFAULT_HERO_LEVEL;
		fight.winnerPicture = DEFAULT_HERO_PICTURE;
    fight.winnerPowers = DEFAULT_HERO_POWERS;
		fight.loserName = DEFAULT_VILLAIN_NAME;
		fight.loserLevel = DEFAULT_VILLAIN_LEVEL;
		fight.loserPicture = DEFAULT_VILLAIN_PICTURE;
    fight.loserPowers = DEFAULT_VILLAIN_POWERS;
		fight.winnerTeam = HEROES_TEAM_NAME;
		fight.loserTeam = VILLAINS_TEAM_NAME;

		return fight;
	}

	private static Fight createFightVillainWon() {
		var fight = new Fight();
		fight.id = DEFAULT_FIGHT_ID;
		fight.fightDate = DEFAULT_FIGHT_DATE;
		fight.winnerName = DEFAULT_VILLAIN_NAME;
		fight.winnerLevel = DEFAULT_VILLAIN_LEVEL;
		fight.winnerPicture = DEFAULT_VILLAIN_PICTURE;
    fight.winnerPowers = DEFAULT_VILLAIN_POWERS;
		fight.winnerTeam = VILLAINS_TEAM_NAME;
		fight.loserName = DEFAULT_HERO_NAME;
		fight.loserLevel = DEFAULT_HERO_LEVEL;
		fight.loserPicture = DEFAULT_HERO_PICTURE;
    fight.loserPowers = DEFAULT_HERO_POWERS;
		fight.loserTeam = HEROES_TEAM_NAME;

		return fight;
	}
}
