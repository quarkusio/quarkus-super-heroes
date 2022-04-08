package io.quarkus.sample.superheroes.fight.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.reactive.messaging.Channel;

import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.HeroClient;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.sample.superheroes.fight.client.VillainClient;
import io.quarkus.sample.superheroes.fight.config.FightConfig;
import io.quarkus.sample.superheroes.fight.mapping.FightMapper;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;

/**
 * Business logic for the Fight service
 */
@ApplicationScoped
public class FightService {
	private final HeroClient heroClient;
	private final VillainClient villainClient;
	private final MutinyEmitter<io.quarkus.sample.superheroes.fight.schema.Fight> emitter;
	private final FightConfig fightConfig;
  private final FightMapper fightMapper;
	private final Random random = new Random();

	public FightService(HeroClient heroClient, VillainClient villainClient, @Channel("fights") MutinyEmitter<io.quarkus.sample.superheroes.fight.schema.Fight> emitter, FightConfig fightConfig, FightMapper fightMapper) {
		this.heroClient = heroClient;
		this.villainClient = villainClient;
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

	public Uni<List<Fight>> findAllFights() {
		return Fight.listAll();
	}

	public Uni<Fight> findFightById(String id) {
		return Fight.findById(new ObjectId(id));
	}

	@Timeout(value = 4, unit = ChronoUnit.SECONDS)
	public Uni<Fighters> findRandomFighters() {
		Uni<Hero> hero = findRandomHero()
			.onItem().ifNull().continueWith(this::createFallbackHero);

		Uni<Villain> villain = findRandomVillain()
			.onItem().ifNull().continueWith(this::createFallbackVillain);

		return addDelay(Uni.combine()
			.all()
			.unis(hero, villain)
			.combinedWith(Fighters::new)
		);
	}

	@Timeout(value = 2, unit = ChronoUnit.SECONDS)
	@Fallback(fallbackMethod = "fallbackRandomHero")
	Uni<Hero> findRandomHero() {
		return this.heroClient.findRandomHero()
			.invoke(hero -> Log.debugf("Got random hero: %s", hero));
	}

	@Timeout(value = 2, unit = ChronoUnit.SECONDS)
	@Fallback(fallbackMethod = "fallbackRandomVillain")
	Uni<Villain> findRandomVillain() {
		return this.villainClient.findRandomVillain()
			.invoke(villain -> Log.debugf("Got random villain: %s", villain));
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

	private Villain createFallbackVillain() {
		return new Villain(
			this.fightConfig.villain().fallback().name(),
			this.fightConfig.villain().fallback().level(),
			this.fightConfig.villain().fallback().picture(),
			this.fightConfig.villain().fallback().powers()
		);
	}

	public Uni<Fight> performFight(@NotNull @Valid Fighters fighters) {
		return determineWinner(fighters)
			.chain(this::persistFight);
	}

	Uni<Fight> persistFight(Fight fight) {
		return Fight.persist(fight)
      .replaceWith(fight)
      .map(this.fightMapper::toSchema)
      .chain(this.emitter::send)
			.replaceWith(fight);
	}

	Uni<Fight> determineWinner(Fighters fighters) {
		// Amazingly fancy logic to determine the winner...
		return Uni.createFrom().item(() -> {
				Fight fight;

				if (shouldHeroWin(fighters)) {
					fight = heroWonFight(fighters);
				}
				else if (shouldVillainWin(fighters)) {
					fight = villainWonFight(fighters);
				}
				else {
					fight = getRandomWinner(fighters);
				}

				fight.fightDate = Instant.now();

				return fight;
			}
		);
	}

	boolean shouldHeroWin(Fighters fighters) {
		int heroAdjust = this.random.nextInt(this.fightConfig.hero().adjustBound());
		int villainAdjust = this.random.nextInt(this.fightConfig.villain().adjustBound());

		return (fighters.getHero().getLevel() + heroAdjust) > (fighters.getVillain().getLevel() + villainAdjust);
	}

	boolean shouldVillainWin(Fighters fighters) {
		return fighters.getHero().getLevel() < fighters.getVillain().getLevel();
	}

	Fight getRandomWinner(Fighters fighters) {
		return this.random.nextBoolean() ?
		       heroWonFight(fighters) :
		       villainWonFight(fighters);
	}

	Fight heroWonFight(Fighters fighters) {
		Log.infof("Yes, Hero %s won over %s :o)", fighters.getHero().getName(), fighters.getVillain().getName());

		Fight fight = new Fight();
		fight.winnerName = fighters.getHero().getName();
		fight.winnerPicture = fighters.getHero().getPicture();
		fight.winnerLevel = fighters.getHero().getLevel();
		fight.loserName = fighters.getVillain().getName();
		fight.loserPicture = fighters.getVillain().getPicture();
		fight.loserLevel = fighters.getVillain().getLevel();
		fight.winnerTeam = this.fightConfig.hero().teamName();
		fight.loserTeam = this.fightConfig.villain().teamName();

		return fight;
	}

	Fight villainWonFight(Fighters fighters) {
		Log.infof("Gee, Villain %s won over %s :o(", fighters.getVillain().getName(), fighters.getHero().getName());

		Fight fight = new Fight();
		fight.winnerName = fighters.getVillain().getName();
		fight.winnerPicture = fighters.getVillain().getPicture();
		fight.winnerLevel = fighters.getVillain().getLevel();
		fight.loserName = fighters.getHero().getName();
		fight.loserPicture = fighters.getHero().getPicture();
		fight.loserLevel = fighters.getHero().getLevel();
		fight.winnerTeam = this.fightConfig.villain().teamName();
		fight.loserTeam = this.fightConfig.hero().teamName();
		return fight;
	}

  @Timeout(value = 5, unit = ChronoUnit.SECONDS)
  @Fallback(fallbackMethod = "fallbackHelloHeroes")
  public String helloHeroes() {
    Log.debug("Invokes the Heroes microservice");
    return this.heroClient.helloHeroes();
  }

  String fallbackHelloHeroes() {
    Log.warn("Could not invoke the Heroes microservice");
    return "Could not invoke the Heroes microservice";
  }

  @Timeout(value = 5, unit = ChronoUnit.SECONDS)
  @Fallback(fallbackMethod = "fallbackHelloVillains")
  public String helloVillains() {
    Log.debug("Invokes the Villains microservice");
    return this.villainClient.helloVillains();
  }

  String fallbackHelloVillains() {
    Log.warn("Could not invoke the Villains microservice");
    return "Could not invoke the Villains microservice";
  }
}
