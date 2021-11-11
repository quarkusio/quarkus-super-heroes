package io.quarkus.sample.superheroes.villain.service;

import static javax.transaction.Transactional.TxType.*;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.quarkus.sample.superheroes.villain.Villain;
import io.quarkus.sample.superheroes.villain.config.VillainConfig;

@ApplicationScoped
@Transactional(REQUIRED)
public class VillainService {
	private final VillainConfig villainConfig;

	public VillainService(VillainConfig villainConfig) {
		this.villainConfig = villainConfig;
	}

	@Transactional(SUPPORTS)
	public List<Villain> findAllVillains() {
		return Villain.listAll();
	}

	@Transactional(SUPPORTS)
	public Optional<Villain> findVillainById(Long id) {
		return Villain.findByIdOptional(id);
	}

	@Transactional(SUPPORTS)
	public Villain findRandomVillain() {
		Villain randomVillain = null;

		while (randomVillain == null) {
			randomVillain = Villain.findRandom();
		}

		return randomVillain;
	}

	public Villain persistVillain(@NotNull @Valid Villain villain) {
		villain.level = (int) Math.round(villain.level * this.villainConfig.level().multiplier());
		Villain.persist(villain);

		return villain;
	}

	public Villain updateVillain(@NotNull @Valid Villain villain) {
		Villain entity = Villain.findById(villain.id);
		entity.name = villain.name;
		entity.otherName = villain.otherName;
		entity.level = villain.level;
		entity.picture = villain.picture;
		entity.powers = villain.powers;

		return entity;
	}

	public void deleteVillain(Long id) {
		Villain.deleteById(id);
	}
}
