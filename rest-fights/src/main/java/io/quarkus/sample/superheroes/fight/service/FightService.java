package io.quarkus.sample.superheroes.fight.service;

import java.time.Instant;
import java.util.List;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.HeroClient;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.sample.superheroes.fight.client.VillainClient;
import io.quarkus.sample.superheroes.fight.config.FightConfig;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;

@ApplicationScoped
public class FightService {
	private final Logger logger;
	private final HeroClient heroClient;
	private final VillainClient villainClient;
	private final MutinyEmitter<Fight> emitter;
	private final FightConfig fightConfig;
	private final Random random = new Random();

	public FightService(Logger logger, HeroClient heroClient, VillainClient villainClient, @Channel("fights") MutinyEmitter<Fight> emitter, FightConfig fightConfig) {
		this.logger = logger;
		this.heroClient = heroClient;
		this.villainClient = villainClient;
		this.emitter = emitter;
		this.fightConfig = fightConfig;
	}

	public Uni<List<Fight>> findAllFights() {
		return Fight.listAll();
	}

	public Uni<Fight> findFightById(Long id) {
		return Fight.findById(id);
	}

	public Uni<Fighters> findRandomFighters() {
		Uni<Hero> hero = findRandomHero();
		Uni<Villain> villain = findRandomVillain();

		return Uni.combine()
			.all()
			.unis(hero, villain)
			.combinedWith(Fighters::new);
	}

	@Fallback(fallbackMethod = "fallbackRandomHero")
	Uni<Hero> findRandomHero() {
		return this.heroClient.findRandomHero()
			.invoke(hero -> this.logger.debugf("Got random hero: %s", hero));
	}

	@Fallback(fallbackMethod = "fallbackRandomVillain")
	Uni<Villain> findRandomVillain() {
		return this.villainClient.findRandomVillain()
			.invoke(villain -> this.logger.debugf("Got random villain: %s", villain));
	}

	Uni<Hero> fallbackRandomHero() {
		this.logger.warn("Falling back on Hero");

		return Uni.createFrom().item(
			new Hero(
				this.fightConfig.hero().fallback().name(),
				this.fightConfig.hero().fallback().level(),
				this.fightConfig.hero().fallback().picture(),
				this.fightConfig.hero().fallback().powers()
			)
		);
	}

	Uni<Villain> fallbackRandomVillain() {
		this.logger.warn("Falling back on Villain");

		return Uni.createFrom().item(
			new Villain(
				this.fightConfig.villain().fallback().name(),
				this.fightConfig.villain().fallback().level(),
				this.fightConfig.villain().fallback().picture(),
				this.fightConfig.villain().fallback().powers()
			)
		);
	}

	@ReactiveTransactional
	public Uni<Fight> persistFight(Fighters fighters) {
		// Amazingly fancy logic to determine the winner...
		Fight fight;

		int heroAdjust = this.random.nextInt(20);
		int villainAdjust = this.random.nextInt(20);

		if ((fighters.getHero().getLevel() + heroAdjust) > (fighters.getVillain().getLevel() + villainAdjust)) {
			fight = heroWon(fighters);
		}
		else if (fighters.getHero().getLevel() < fighters.getVillain().getLevel()) {
			fight = villainWon(fighters);
		}
		else {
			fight = this.random.nextBoolean() ? heroWon(fighters) : villainWon(fighters);
		}

		fight.fightDate = Instant.now();

		return Fight.persist(fight)
			.replaceWith(fight)
			.chain(this.emitter::send)
			.replaceWith(fight);
	}

	private Fight heroWon(Fighters fighters) {
		this.logger.info("Yes, Hero won :o)");

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

	private Fight villainWon(Fighters fighters) {
		this.logger.info("Gee, Villain won :o(");

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
}
