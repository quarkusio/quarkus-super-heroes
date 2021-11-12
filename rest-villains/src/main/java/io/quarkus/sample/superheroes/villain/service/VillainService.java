package io.quarkus.sample.superheroes.villain.service;

import static javax.transaction.Transactional.TxType.*;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import io.quarkus.sample.superheroes.villain.Villain;
import io.quarkus.sample.superheroes.villain.config.VillainConfig;
import io.quarkus.sample.superheroes.villain.mapping.VillainFullUpdateMapper;
import io.quarkus.sample.superheroes.villain.mapping.VillainPartialUpdateMapper;

@ApplicationScoped
@Transactional(REQUIRED)
public class VillainService {
	private final Validator validator;
	private final VillainConfig villainConfig;
	private final VillainPartialUpdateMapper villainPartialUpdateMapper;
	private final VillainFullUpdateMapper villainFullUpdateMapper;

	public VillainService(Validator validator, VillainConfig villainConfig, VillainPartialUpdateMapper villainPartialUpdateMapper, VillainFullUpdateMapper villainFullUpdateMapper) {
		this.validator = validator;
		this.villainConfig = villainConfig;
		this.villainPartialUpdateMapper = villainPartialUpdateMapper;
		this.villainFullUpdateMapper = villainFullUpdateMapper;
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

	public Optional<Villain> replaceVillain(@NotNull @Valid Villain villain) {
		return Villain.findByIdOptional(villain.id)
			.map(Villain.class::cast)
			.map(v -> {
				this.villainFullUpdateMapper.mapFullUpdate(villain, v);
				return v;
			});
	}

	public Optional<Villain> partialUpdateVillain(@NotNull Villain villain) {
		return Villain.findByIdOptional(villain.id)
			.map(Villain.class::cast)
			.map(v -> {
				this.villainPartialUpdateMapper.mapPartialUpdate(villain, v);
				return v;
			})
			.map(this::validatePartialUpdate);
	}

	private Villain validatePartialUpdate(Villain villain) {
		var violations = this.validator.validate(villain);

		if ((violations != null) && !violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}

		return villain;
	}

	public void deleteVillain(Long id) {
		Villain.deleteById(id);
	}
}
