package io.quarkus.sample.superheroes.fight.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.FightImage;
import io.quarkus.sample.superheroes.fight.FightLocation;
import io.quarkus.sample.superheroes.fight.FightRequest;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.client.FightToNarrate;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.HeroClient;
import io.quarkus.sample.superheroes.fight.client.LocationClient;
import io.quarkus.sample.superheroes.fight.client.NarrationClient;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.sample.superheroes.fight.client.VillainClient;
import io.quarkus.sample.superheroes.fight.config.FightConfig;
import io.quarkus.sample.superheroes.fight.mapping.FightMapper;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.TracingMetadata;

/**
 * Business logic for the Fight service
 */
@ApplicationScoped
public class FightService {
	private final HeroClient heroClient;
	private final VillainClient villainClient;
  private final NarrationClient narrationClient;
	private final LocationClient locationClient;
	private final MutinyEmitter<io.quarkus.sample.superheroes.fight.schema.Fight> emitter;
	private final FightConfig fightConfig;
  private final FightMapper fightMapper;
	private final Random random = new Random();

	public FightService(HeroClient heroClient, VillainClient villainClient, @RestClient NarrationClient narrationClient, LocationClient locationClient, @Channel("fights") MutinyEmitter<io.quarkus.sample.superheroes.fight.schema.Fight> emitter, FightConfig fightConfig, FightMapper fightMapper) {
		this.heroClient = heroClient;
		this.villainClient = villainClient;
    this.narrationClient = narrationClient;
		this.locationClient = locationClient;
		this.emitter = emitter;
		this.fightConfig = fightConfig;
    this.fightMapper = fightMapper;
  }

	/**
	 * Adds a pre-configured delay to a response. Can be used to showcase/demo how to add delays as well as how to use fault tolerance.
	 * @param fighters The {@link Fighters}.
	 * @return A {@link Uni} with a configured delay added
	 * @see FightConfig#process()
	 */
	Uni<Fighters> addDelay(Uni<Fighters> fighters) {
		long delayMillis = this.fightConfig.process().delayMillis();

		return (delayMillis > 0) ?
		       fighters.onItem().delayIt().by(Duration.ofMillis(delayMillis))
			       .invoke(() -> Log.debugf("Adding delay of %d millis to request", delayMillis)) :
		       fighters;
	}

  @WithSpan("FightService.findAllFights")
	public Uni<List<Fight>> findAllFights() {
    Log.debug("Getting all fights");
		return Fight.listAll();
	}

  @WithSpan("FightService.findFightById")
	public Uni<Fight> findFightById(@SpanAttribute("arg.id") String id) {
    Log.debugf("Finding fight by id = %s", id);
		return Fight.findById(new ObjectId(id));
	}

	@Timeout(value = 4, unit = ChronoUnit.SECONDS)
  @Fallback(fallbackMethod = "fallbackRandomFighters")
  @WithSpan("FightService.findRandomFighters")
	public Uni<Fighters> findRandomFighters() {
    Log.debug("Finding random fighters");

    var villain = findRandomVillain()
      .onItem().ifNull().continueWith(this::createFallbackVillain);

		var hero = findRandomHero()
      .onItem().ifNull().continueWith(this::createFallbackHero);

		return addDelay(Uni.combine()
			.all()
			.unis(hero, villain)
      .with((h, v) -> new Fighters(h, v))
		);
	}

  @Timeout(value = 2, unit = ChronoUnit.SECONDS)
  @Fallback(fallbackMethod = "fallbackRandomLocation")
  public Uni<FightLocation> findRandomLocation() {
    Log.debug("Finding a random location");
    return this.locationClient.findRandomLocation()
      .onItem().ifNull().continueWith(this::createFallbackLocation)
      .invoke(location -> Log.debugf("Got random location: %s", location));
  }

	@Timeout(value = 2, unit = ChronoUnit.SECONDS)
	@Fallback(fallbackMethod = "fallbackRandomHero")
	Uni<Hero> findRandomHero() {
    Log.debug("Finding a random hero");
		return this.heroClient.findRandomHero()
			.invoke(hero -> Log.debugf("Got random hero: %s", hero));
	}

	@Timeout(value = 2, unit = ChronoUnit.SECONDS)
	@Fallback(fallbackMethod = "fallbackRandomVillain")
	Uni<Villain> findRandomVillain() {
    Log.debug("Finding a random villain");
		return this.villainClient.findRandomVillain()
			.invoke(villain -> Log.debugf("Got random villain: %s", villain));
	}

  @Timeout(value = 5, unit = ChronoUnit.SECONDS)
  @Fallback(fallbackMethod = "fallbackHelloHeroes")
  @WithSpan("FightService.helloHeroes")
  public Uni<String> helloHeroes() {
    Log.debug("Pinging heroes service");
    return this.heroClient.helloHeroes()
      .invoke(hello -> Log.debugf("Got %s from the Heroes microservice", hello));
  }

  @Timeout(value = 5, unit = ChronoUnit.SECONDS)
  @Fallback(fallbackMethod = "fallbackHelloNarration")
  @WithSpan("FightService.helloNarration")
  public Uni<String> helloNarration() {
    Log.debug("Pinging narration service");
    return this.narrationClient.hello()
      .invoke(hello -> Log.debugf("Got %s back from the Narration microservice", hello));
  }

	@Timeout(value = 5, unit = ChronoUnit.SECONDS)
	@Fallback(fallbackMethod = "fallbackHelloLocations")
	@WithSpan("FightService.helloLocations")
	public Uni<String> helloLocations() {
		Log.debug("Pinging location service");
		return this.locationClient.helloLocations()
			.invoke(hello -> Log.debugf("Got %s back from the Locations microservice", hello));
	}

	Uni<String> fallbackHelloLocations() {
		return Uni.createFrom().item("Could not invoke the Locations microservice")
			.invoke(message -> Log.warn(message));
	}

  Uni<String> fallbackHelloNarration() {
    return Uni.createFrom().item("Could not invoke the Narration microservice")
      .invoke(message -> Log.warn(message));
  }

  Uni<Fighters> fallbackRandomFighters() {
    return Uni.createFrom().item(new Fighters(createFallbackHero(), createFallbackVillain()))
      .invoke(() -> Log.warn("Falling back on finding random fighters"));
  }

  Uni<String> fallbackHelloHeroes() {
    return Uni.createFrom().item("Could not invoke the Heroes microservice")
      .invoke(message -> Log.warn(message));
  }

  @Timeout(value = 5, unit = ChronoUnit.SECONDS)
  @Fallback(fallbackMethod = "fallbackHelloVillains")
  @WithSpan("FightService.helloVillains")
  public Uni<String> helloVillains() {
    Log.debug("Pinging villains service");
    return this.villainClient.helloVillains()
      .invoke(hello -> Log.debugf("Got %s from the Villains microservice", hello));
  }

  Uni<String> fallbackHelloVillains() {
    return Uni.createFrom().item("Could not invoke the Villains microservice")
      .invoke(message -> Log.warn(message));
  }

	Uni<Hero> fallbackRandomHero() {
		return Uni.createFrom().item(this::createFallbackHero)
			.invoke(h -> Log.warn("Falling back on Hero"));
	}

	private Hero createFallbackHero() {
		return new Hero(
			this.fightConfig.hero().fallback().name(),
			this.fightConfig.hero().fallback().level(),
			this.fightConfig.hero().fallback().picture(),
			this.fightConfig.hero().fallback().powers()
		);
	}

	Uni<Villain> fallbackRandomVillain() {
		return Uni.createFrom().item(this::createFallbackVillain)
			.invoke(v -> Log.warn("Falling back on Villain"));
	}

  Uni<FightLocation> fallbackRandomLocation() {
    return Uni.createFrom().item(this::createFallbackLocation)
      .invoke(l -> Log.warn("Falling back on Location"));
  }

  private FightLocation createFallbackLocation() {
    return new FightLocation(
      this.fightConfig.location().fallback().name(),
      this.fightConfig.location().fallback().description(),
      this.fightConfig.location().fallback().picture()
    );
  }

  Uni<String> fallbackNarrateFight(FightToNarrate fight) {
    return Uni.createFrom().item(this.fightConfig.narration().fallbackNarration())
      .invoke(n -> Log.warn("Falling back on Narration"));
  }

  Uni<FightImage> fallbackGenerateImageFromNarration(String narration) {
    var fallbackImageGeneration = this.fightConfig.narration().fallbackImageGeneration();

    return Uni.createFrom().item(new FightImage(fallbackImageGeneration.imageUrl(), fallbackImageGeneration.imageNarration()))
      .invoke(i -> Log.warn("Falling back on narration image generation"));
  }

	private Villain createFallbackVillain() {
		return new Villain(
			this.fightConfig.villain().fallback().name(),
			this.fightConfig.villain().fallback().level(),
			this.fightConfig.villain().fallback().picture(),
			this.fightConfig.villain().fallback().powers()
		);
	}

  @WithSpan("FightService.performFight")
	public Uni<Fight> performFight(@SpanAttribute("arg.fighters") @NotNull @Valid FightRequest fightRequest) {
    Log.debugf("Performing a fight with fighters: %s", fightRequest);
    return determineWinner(fightRequest)
      .chain(this::persistFight);
  }

  @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 2, delayUnit = ChronoUnit.SECONDS)
  @CircuitBreakerName("narrateFight")
  @Timeout(value = 30, unit = ChronoUnit.SECONDS)
  @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
	@Fallback(fallbackMethod = "fallbackNarrateFight")
  @WithSpan("FightService.narrateFight")
  public Uni<String> narrateFight(@SpanAttribute("arg.fight") FightToNarrate fight) {
    Log.debugf("Narrating fight: %s", fight);
    return this.narrationClient.narrate(fight);
  }

  @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 2, delayUnit = ChronoUnit.SECONDS)
  @CircuitBreakerName("generateImageFromNarration")
  @Timeout(value = 30, unit = ChronoUnit.SECONDS)
  @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
	@Fallback(fallbackMethod = "fallbackGenerateImageFromNarration")
  @WithSpan("FightService.generateImageFromNarration")
  public Uni<FightImage> generateImageFromNarration(@SpanAttribute("arg.narration") String narration) {
    Log.debugf("Generating image for narration: %s", narration);
    return this.narrationClient.generateImageFromNarration(narration);
  }

  @WithSpan("FightService.persistFight")
	Uni<Fight> persistFight(@SpanAttribute("arg.fight") Fight fight) {
    Log.debugf("Persisting a fight: %s", fight);
		return Fight.persist(fight)
      .replaceWith(fight)
      .map(this.fightMapper::toSchema)
      .invoke(f -> this.emitter.sendMessageAndForget(Message.of(f, Metadata.of(TracingMetadata.withCurrent(Context.current())))))
      .replaceWith(fight);
	}

	Uni<Fight> determineWinner(@SpanAttribute("arg.fighters") FightRequest fightRequest) {
    Log.debugf("Determining winner between fighters: %s", fightRequest);

		// Amazingly fancy logic to determine the winner...
		return Uni.createFrom().item(() -> {
				Fight fight;

				if (shouldHeroWin(fightRequest)) {
					fight = heroWonFight(fightRequest);
				}
				else if (shouldVillainWin(fightRequest)) {
					fight = villainWonFight(fightRequest);
				}
				else {
					fight = getRandomWinner(fightRequest);
				}

				fight.fightDate = Instant.now();

				return fight;
			}
		);
	}

	boolean shouldHeroWin(FightRequest fightRequest) {
		int heroAdjust = this.random.nextInt(this.fightConfig.hero().adjustBound());
		int villainAdjust = this.random.nextInt(this.fightConfig.villain().adjustBound());

		return (fightRequest.hero().level() + heroAdjust) > (fightRequest.villain().level() + villainAdjust);
	}

	boolean shouldVillainWin(FightRequest fightRequest) {
		return fightRequest.hero().level() < fightRequest.villain().level();
	}

	Fight getRandomWinner(FightRequest fightRequest) {
		return this.random.nextBoolean() ?
		       heroWonFight(fightRequest) :
		       villainWonFight(fightRequest);
	}

	Fight heroWonFight(FightRequest fightRequest) {
		Log.infof("Yes, Hero %s won over %s :o)", fightRequest.hero().name(), fightRequest.villain().name());

		Fight fight = new Fight();
		fight.winnerName = fightRequest.hero().name();
		fight.winnerPicture = fightRequest.hero().picture();
		fight.winnerLevel = fightRequest.hero().level();
    fight.winnerPowers = fightRequest.hero().powers();
		fight.loserName = fightRequest.villain().name();
		fight.loserPicture = fightRequest.villain().picture();
		fight.loserLevel = fightRequest.villain().level();
    fight.loserPowers = fightRequest.villain().powers();
		fight.winnerTeam = this.fightConfig.hero().teamName();
		fight.loserTeam = this.fightConfig.villain().teamName();
    fight.location = fightRequest.location();

		return fight;
	}

	Fight villainWonFight(FightRequest fightRequest) {
		Log.infof("Gee, Villain %s won over %s :o(", fightRequest.villain().name(), fightRequest.hero().name());

		Fight fight = new Fight();
		fight.winnerName = fightRequest.villain().name();
		fight.winnerPicture = fightRequest.villain().picture();
		fight.winnerLevel = fightRequest.villain().level();
    fight.winnerPowers = fightRequest.villain().powers();
		fight.loserName = fightRequest.hero().name();
		fight.loserPicture = fightRequest.hero().picture();
		fight.loserLevel = fightRequest.hero().level();
    fight.loserPowers = fightRequest.hero().powers();
		fight.winnerTeam = this.fightConfig.villain().teamName();
		fight.loserTeam = this.fightConfig.hero().teamName();
    fight.location = fightRequest.location();

		return fight;
	}
}
