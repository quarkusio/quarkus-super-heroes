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
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.FightImage;
import io.quarkus.sample.superheroes.fight.FightRequest;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.ShorterTimeoutsProfile;
import io.quarkus.sample.superheroes.fight.client.FightToNarrate;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.HeroClient;
import io.quarkus.sample.superheroes.fight.client.LocationClient;
import io.quarkus.sample.superheroes.fight.client.NarrationClient;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.sample.superheroes.fight.client.VillainClient;
import io.quarkus.sample.superheroes.fight.config.FightConfig;
import io.quarkus.sample.superheroes.fight.mapping.FightMapper;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
@TestProfile(ShorterTimeoutsProfile.class)
class FightServiceTests extends FightServiceTestsBase {
  private static final String FIGHTS_CHANNEL_NAME = "fights";
  private static final String FALLBACK_NARRATION = """
                                                   High above a bustling city, a symbol of hope and justice soared through the sky, while chaos reigned below, with malevolent laughter echoing through the streets.
                                                   With unwavering determination, the figure swiftly descended, effortlessly evading explosive attacks, closing the gap, and delivering a decisive blow that silenced the wicked laughter.
                                                   
                                                   In the end, the battle concluded with a clear victory for the forces of good, as their commitment to peace triumphed over the chaos and villainy that had threatened the city.
                                                   The people knew that their protector had once again ensured their safety.
                                                   """;

  private static final FightImage IMAGE = new FightImage("https://somewhere.com/someImage.png", "Fallback image");

  @InjectMock
  HeroClient heroClient;

	@InjectMock
  VillainClient villainClient;

	@InjectMock
	LocationClient locationClient;

  @InjectMock
  @RestClient
  NarrationClient narrationClient;

  @Inject
  @Any
  InMemoryConnector emitterConnector;

  @Inject
  FightMapper fightMapper;

  @Inject
  FightConfig fightConfig;

  @BeforeEach
  void beforeEach() {
    // Clear the emitter sink between tests
    this.emitterConnector.sink(FIGHTS_CHANNEL_NAME).clear();
  }

	@Test
	void findAllFightsNoneFound() {
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
	void findAllFights() {
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
      .singleElement()
      .usingRecursiveComparison()
      .isEqualTo(createFightHeroWon());

		PanacheMock.verify(Fight.class).listAll();
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	void findFightByIdFound() {
		PanacheMock.mock(Fight.class);
		when(Fight.findById(DEFAULT_FIGHT_ID))
			.thenReturn(Uni.createFrom().item(createFightHeroWon()));

		var fight = this.fightService.findFightById(DEFAULT_FIGHT_ID.toString())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(createFightHeroWon());

		PanacheMock.verify(Fight.class).findById(eq(DEFAULT_FIGHT_ID));
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	void findFightByIdNotFound() {
		PanacheMock.mock(Fight.class);
		when(Fight.findById(DEFAULT_FIGHT_ID))
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
	void findRandomFightersHeroError() {
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
	void findRandomFightersVillainError() {
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
	void findRandomFightersHeroVillainError() {
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
  void findRandomLocationDelayTriggersFallback() {
    PanacheMock.mock(Fight.class);
    when(this.locationClient.findRandomLocation())
      .thenReturn(
        Uni.createFrom().item(createDefaultFightLocation())
          .onItem()
          .delayIt().by(Duration.ofSeconds(ShorterTimeoutsProfile.FIND_RANDOM_OVERRIDDEN_TIMEOUT + 1))
      );

    var location = this.fightService.findRandomLocation()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

    assertThat(location)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(createFallbackLocation());

    verify(this.locationClient).findRandomLocation();
    verify(this.fightService).fallbackRandomLocation();
    PanacheMock.verifyNoInteractions(Fight.class);
  }

	@Test
	void findRandomHeroDelayTriggersFallback() {
		PanacheMock.mock(Fight.class);
		when(this.heroClient.findRandomHero())
			.thenReturn(
				Uni.createFrom().item(createDefaultHero())
					.onItem()
					.delayIt().by(Duration.ofSeconds(ShorterTimeoutsProfile.FIND_RANDOM_OVERRIDDEN_TIMEOUT + 1))
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
	void findRandomVillainDelayTriggersFallback() {
		PanacheMock.mock(Fight.class);
		when(this.villainClient.findRandomVillain())
			.thenReturn(
				Uni.createFrom().item(createDefaultVillain())
					.onItem()
					.delayIt().by(Duration.ofSeconds(ShorterTimeoutsProfile.FIND_RANDOM_OVERRIDDEN_TIMEOUT + 1))
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
	void findRandomFightersAddDelayTriggersTimeout() {
		PanacheMock.mock(Fight.class);

		// Mock the addDelay method so it returns a 5 second delay on whatever argument (a Uni<Fighters>) was passed into it
		// This should trigger the timeout on findRandomFighters
		doAnswer(invocation -> ((Uni<Fighters>) invocation.getArgument(0)).onItem().delayIt().by(Duration.ofSeconds(ShorterTimeoutsProfile.FIND_RANDOM_FIGHTERS_OVERRIDDEN_TIMEOUT + 1)))
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
	void findRandomFightersDelayedFindHeroAndVillainTriggersTimeout() {
		PanacheMock.mock(Fight.class);

		// Add a delay to the call to findRandomHero()
		doReturn(Uni.createFrom().item(createDefaultHero()).onItem().delayIt().by(Duration.ofSeconds(ShorterTimeoutsProfile.FIND_RANDOM_FIGHTERS_OVERRIDDEN_TIMEOUT + 1)))
			.when(this.fightService)
			.findRandomHero();

		// Add a delay to the call to findRandomVillain
		doReturn(Uni.createFrom().item(createDefaultVillain()).onItem().delayIt().by(Duration.ofSeconds(ShorterTimeoutsProfile.FIND_RANDOM_FIGHTERS_OVERRIDDEN_TIMEOUT + 1)))
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
	void performFightNullFighters() {
		PanacheMock.mock(Fight.class);

		var cve = catchThrowableOfType(
			ConstraintViolationException.class,
			() -> this.fightService.performFight(null)
		);

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

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

		verify(this.fightService, never()).determineWinner(any(FightRequest.class));
		verify(this.fightService, never()).shouldHeroWin(any(FightRequest.class));
		verify(this.fightService, never()).shouldVillainWin(any(FightRequest.class));
		verify(this.fightService, never()).getRandomWinner(any(FightRequest.class));
		verify(this.fightService, never()).heroWonFight(any(FightRequest.class));
		verify(this.fightService, never()).villainWonFight(any(FightRequest.class));
		verify(this.fightService, never()).persistFight(any(Fight.class));
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	void performFightInvalidFighters() {
		PanacheMock.mock(Fight.class);

		var cve = catchThrowableOfType(
			ConstraintViolationException.class,
			() -> this.fightService.performFight(new FightRequest(null, createDefaultVillain(), createDefaultFightLocation()))
		);

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

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

		verify(this.fightService, never()).determineWinner(any(FightRequest.class));
		verify(this.fightService, never()).shouldHeroWin(any(FightRequest.class));
		verify(this.fightService, never()).shouldVillainWin(any(FightRequest.class));
		verify(this.fightService, never()).getRandomWinner(any(FightRequest.class));
		verify(this.fightService, never()).heroWonFight(any(FightRequest.class));
		verify(this.fightService, never()).villainWonFight(any(FightRequest.class));
		verify(this.fightService, never()).persistFight(any(Fight.class));
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
		PanacheMock.verifyNoInteractions(Fight.class);
	}

  @Test
  void generateFightImageFromNarrationTimesOut() {
    var timeout = Duration.ofSeconds(ShorterTimeoutsProfile.NARRATION_OVERRIDDEN_TIMEOUT + 1);

    when(this.narrationClient.generateImageFromNarration(DEFAULT_NARRATION))
      .thenReturn(
        Uni.createFrom().item(IMAGE)
          .onItem().delayIt().by(timeout)
      );

    var image = this.fightService.generateImageFromNarration(DEFAULT_NARRATION)
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(timeout.multipliedBy(4))
			.getItem();

    assertThat(image)
      .isNotNull()
      .usingRecursiveAssertion()
      .isEqualTo(getFallbackImage());

    verify(this.fightService).generateImageFromNarration(DEFAULT_NARRATION);
    verify(this.fightService).fallbackGenerateImageFromNarration(DEFAULT_NARRATION);
  }

  @Test
  void generateFightFromNarrationError() {
    doThrow(new RuntimeException())
      .when(this.narrationClient)
      .generateImageFromNarration(DEFAULT_NARRATION);

    var image = this.fightService.generateImageFromNarration(DEFAULT_NARRATION)
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(ShorterTimeoutsProfile.NARRATION_OVERRIDDEN_TIMEOUT + 1).multipliedBy(4))
			.getItem();

    assertThat(image)
      .isNotNull()
      .usingRecursiveAssertion()
      .isEqualTo(getFallbackImage());

    verify(this.fightService).generateImageFromNarration(DEFAULT_NARRATION);
    verify(this.fightService).fallbackGenerateImageFromNarration(DEFAULT_NARRATION);
  }

  @Test
  void narrateFightNarrationTimesOut() {
    var fightToNarrate = createFightToNarrateHeroWon();
    var timeout = Duration.ofSeconds(ShorterTimeoutsProfile.NARRATION_OVERRIDDEN_TIMEOUT + 1);

    when(this.narrationClient.narrate(fightToNarrate))
      .thenReturn(
        Uni.createFrom().item(DEFAULT_NARRATION)
          .onItem().delayIt().by(timeout)
      );

    var narration = this.fightService.narrateFight(fightToNarrate)
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(timeout.multipliedBy(4))
			.getItem();

    assertThat(narration)
      .isNotNull()
      .isEqualTo(FALLBACK_NARRATION);

    verify(this.fightService).fallbackNarrateFight(fightToNarrate);
  }

	@Test
	void performFightHeroShouldWin() {
    var fightOutcome = createFightHeroWon();
		var fightMatcher = fightMatcher(fightOutcome);
    var defaultFightRequest = createDefaultFightRequest();

		PanacheMock.mock(Fight.class);
		PanacheMock.doReturn(Uni.createFrom().voidItem())
			.when(Fight.class)
			.persist(eq(defaultFightRequest), any());

		doReturn(true)
      .when(this.fightService)
      .shouldHeroWin(defaultFightRequest);

		doReturn(fightOutcome)
      .when(this.fightService)
      .heroWonFight(defaultFightRequest);

		var fight = this.fightService.performFight(createDefaultFightRequest())
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

		verify(this.fightService).determineWinner(defaultFightRequest);
		verify(this.fightService).persistFight(argThat(fightMatcher));
		verify(this.fightService).shouldHeroWin(defaultFightRequest);
		verify(this.fightService).heroWonFight(defaultFightRequest);
		verify(this.fightService, never()).shouldVillainWin(any(FightRequest.class));
		verify(this.fightService, never()).villainWonFight(any(FightRequest.class));
		verify(this.fightService, never()).getRandomWinner(any(FightRequest.class));
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
    verify(this.fightService, never()).fallbackNarrateFight(any(FightToNarrate.class));
		PanacheMock.verify(Fight.class).persist(argThat(fightMatcher), any());
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	void performFightVillainShouldWin() {
    var fightOutcome = createFightVillainWon();
    var fightMatcher = fightMatcher(fightOutcome);
    var defaultFightRequest = createDefaultFightRequest();

		PanacheMock.mock(Fight.class);
		PanacheMock.doReturn(Uni.createFrom().voidItem())
			.when(Fight.class)
			.persist(argThat(fightMatcher), any());

		doReturn(false)
      .when(this.fightService)
      .shouldHeroWin(defaultFightRequest);

		doReturn(true)
      .when(this.fightService)
      .shouldVillainWin(defaultFightRequest);

		doReturn(fightOutcome)
      .when(this.fightService)
      .villainWonFight(defaultFightRequest);

		var fight = this.fightService.performFight(createDefaultFightRequest())
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

		verify(this.fightService).determineWinner(defaultFightRequest);
		verify(this.fightService).persistFight(argThat(fightMatcher));
		verify(this.fightService).shouldVillainWin(defaultFightRequest);
		verify(this.fightService).shouldHeroWin(defaultFightRequest);
		verify(this.fightService).villainWonFight(defaultFightRequest);
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
    verify(this.fightService, never()).fallbackNarrateFight(any(FightToNarrate.class));
		verify(this.fightService, never()).heroWonFight(any(FightRequest.class));
		verify(this.fightService, never()).getRandomWinner(any(FightRequest.class));
		PanacheMock.verify(Fight.class).persist(argThat(fightMatcher), any());
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	void performFightRandomWinner() {
    var fightOutcome = createFightVillainWon();
    var fightMatcher = fightMatcher(fightOutcome);
		var defaultFightRequest = createDefaultFightRequest();

		PanacheMock.mock(Fight.class);
		PanacheMock.doReturn(Uni.createFrom().voidItem())
			.when(Fight.class)
			.persist(argThat(fightMatcher), any());

		doReturn(false)
      .when(this.fightService)
      .shouldHeroWin(defaultFightRequest);

		doReturn(false)
      .when(this.fightService)
      .shouldVillainWin(defaultFightRequest);

		doReturn(fightOutcome)
      .when(this.fightService)
      .getRandomWinner(defaultFightRequest);

		var fight = this.fightService.performFight(createDefaultFightRequest())
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

		verify(this.fightService).determineWinner(defaultFightRequest);
		verify(this.fightService).persistFight(argThat(fightMatcher));
		verify(this.fightService).shouldVillainWin(defaultFightRequest);
		verify(this.fightService).shouldHeroWin(defaultFightRequest);
		verify(this.fightService).getRandomWinner(defaultFightRequest);
		verify(this.fightService, never()).villainWonFight(any(FightRequest.class));
		verify(this.fightService, never()).heroWonFight(any(FightRequest.class));
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
    verify(this.fightService, never()).fallbackNarrateFight(any(FightToNarrate.class));
		PanacheMock.verify(Fight.class).persist(argThat(fightMatcher), any());
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	void didHeroWinTrue() {
		var f = createDefaultFightRequest();
    var h = new Hero(f.hero().name(), (Integer.MAX_VALUE - this.fightConfig.hero().adjustBound()), f.hero().picture(), f.hero().powers());
    var v = new Villain(f.villain().name(), Integer.MIN_VALUE, f.villain().picture(), f.villain().powers());
    var fightRequest = new FightRequest(h, v, f.location());

		assertThat(this.fightService.shouldHeroWin(fightRequest))
			.isTrue();
	}

	@Test
	void didHeroWinFalse() {
		var f = createDefaultFightRequest();
    var h = new Hero(f.hero().name(), Integer.MIN_VALUE, f.hero().picture(), f.hero().powers());
    var v = new Villain(f.villain().name(), Integer.MAX_VALUE - this.fightConfig.hero().adjustBound(), f.villain().picture(), f.villain().powers());
    var fightRequest = new FightRequest(h, v, f.location());

		assertThat(this.fightService.shouldHeroWin(fightRequest))
			.isFalse();
	}

	@Test
	void didVillainWinTrue() {
		var f = createDefaultFightRequest();
    var h = new Hero(f.hero().name(), Integer.MIN_VALUE, f.hero().picture(), f.hero().powers());
    var v = new Villain(f.villain().name(), Integer.MAX_VALUE, f.villain().picture(), f.villain().powers());
    var fightRequest = new FightRequest(h, v, f.location());

		assertThat(this.fightService.shouldVillainWin(fightRequest))
			.isTrue();
	}

	@Test
	void didVillainWinFalse() {
		var f = createDefaultFightRequest();
    var h = new Hero(f.hero().name(), Integer.MAX_VALUE, f.hero().picture(), f.hero().powers());
    var v = new Villain(f.villain().name(), Integer.MIN_VALUE, f.villain().picture(), f.villain().powers());
    var fightRequest = new FightRequest(h, v, f.location());

		assertThat(this.fightService.shouldVillainWin(fightRequest))
			.isFalse();
	}

  @Test
  void helloHeroesFallback() {
    when(this.fightService.fallbackHelloHeroes())
	    .thenReturn(Uni.createFrom().item("fallback"));

    when(this.heroClient.helloHeroes())
	    .thenReturn(
				Uni.createFrom().item("hello")
					.onItem()
					.delayIt().by(Duration.ofSeconds(ShorterTimeoutsProfile.HELLO_OVERRIDDEN_TIMEOUT + 1))
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
  void helloHeroesFailure() {
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
  void helloLocationsFallback() {
    when(this.fightService.fallbackHelloLocations())
	    .thenReturn(Uni.createFrom().item("fallback"));

    when(this.locationClient.helloLocations())
	    .thenReturn(
				Uni.createFrom().item("hello")
					.onItem()
					.delayIt().by(Duration.ofSeconds(ShorterTimeoutsProfile.HELLO_OVERRIDDEN_TIMEOUT + 1))
	    );

    var message = this.fightService.helloLocations()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo("fallback");

    verify(this.locationClient).helloLocations();
    verify(this.fightService).helloLocations();
    verify(this.fightService).fallbackHelloLocations();
    verifyNoMoreInteractions(this.locationClient);
  }

	@Test
	void helloLocationsFailure() {
		when(this.fightService.fallbackHelloLocations())
			.thenReturn(Uni.createFrom().item("fallback"));

		when(this.locationClient.helloLocations())
			.thenReturn(Uni.createFrom().failure(() -> new StatusRuntimeException(Status.UNAVAILABLE)));

		var message = this.fightService.helloLocations()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

		assertThat(message)
      .isNotNull()
      .isEqualTo("fallback");

		verify(this.locationClient).helloLocations();
		verify(this.fightService).helloLocations();
		verify(this.fightService).fallbackHelloLocations();
		verifyNoMoreInteractions(this.locationClient);
	}

  @Test
  void helloVillainsFallback() {
    when(this.fightService.fallbackHelloVillains())
	    .thenReturn(Uni.createFrom().item("fallback"));

    when(this.villainClient.helloVillains())
	    .thenReturn(
				Uni.createFrom().item("hello")
					.onItem()
					.delayIt().by(Duration.ofSeconds(ShorterTimeoutsProfile.HELLO_OVERRIDDEN_TIMEOUT + 1))
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
  void helloVillainsFailure() {
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
  void helloNarrationFallback() {
    when(this.fightService.fallbackHelloNarration())
	    .thenReturn(Uni.createFrom().item("fallback"));

    when(this.narrationClient.hello())
      .thenReturn(
        Uni.createFrom().item("hello")
          .onItem()
          .delayIt().by(Duration.ofSeconds(ShorterTimeoutsProfile.HELLO_OVERRIDDEN_TIMEOUT + 1))
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
  void helloNarrationFailure() {
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

  private FightImage getFallbackImage() {
    var image = this.fightConfig.narration().fallbackImageGeneration();
    return new FightImage(image.imageUrl(), image.imageNarration());
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
    fight.location = createDefaultFightLocation();

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
    fight.location = createDefaultFightLocation();

		return fight;
	}
}
