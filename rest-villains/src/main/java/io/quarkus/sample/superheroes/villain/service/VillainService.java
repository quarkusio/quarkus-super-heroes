package io.quarkus.sample.superheroes.villain.service;

import static javax.transaction.Transactional.TxType.*;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import io.quarkus.sample.superheroes.villain.Villain;
import io.quarkus.sample.superheroes.villain.config.VillainConfig;
import io.quarkus.sample.superheroes.villain.mapping.VillainFullUpdateMapper;
import io.quarkus.sample.superheroes.villain.mapping.VillainPartialUpdateMapper;

/**
 * Service class containing business methods for the application.
 */
@ApplicationScoped
@Transactional(REQUIRED)
public class VillainService {
  @Inject
	Validator validator;

  @Inject
  VillainConfig villainConfig;

  @Inject
  VillainPartialUpdateMapper villainPartialUpdateMapper;

  @Inject
  VillainFullUpdateMapper villainFullUpdateMapper;

	@Transactional(SUPPORTS)
	public List<Villain> findAllVillains() {
		return Villain.listAll();
	}

	@Transactional(SUPPORTS)
	public Optional<Villain> findVillainById(Long id) {
		return Villain.findByIdOptional(id);
	}

	@Transactional(SUPPORTS)
	public Optional<Villain> findRandomVillain() {
		return Villain.findRandom();
	}

	public Villain persistVillain(@NotNull @Valid Villain villain) {
		villain.level = (int) Math.round(villain.level * this.villainConfig.level().multiplier());
		Villain.persist(villain);

		return villain;
	}

	public Optional<Villain> replaceVillain(@NotNull @Valid Villain villain) {
		return Villain.findByIdOptional(villain.id)
			.map(Villain.class::cast) // Only here for type erasure within the IDE
			.map(v -> {
				this.villainFullUpdateMapper.mapFullUpdate(villain, v);
				return v;
			});
	}

	public Optional<Villain> partialUpdateVillain(@NotNull Villain villain) {
		return Villain.findByIdOptional(villain.id)
			.map(Villain.class::cast) // Only here for type erasure within the IDE
			.map(v -> {
				this.villainPartialUpdateMapper.mapPartialUpdate(villain, v);
				return v;
			})
			.map(this::validatePartialUpdate);
	}

	/**
	 * Validates a {@link Villain} for a partial update according to annotated validation rules on the {@link Villain} object.
	 * @param villain The {@link Villain}
	 * @return The same {@link Villain} that was passed in, assuming it passes validation. The return is used as a convenience so the method can be called in a functional pipeline.
	 * @throws ConstraintViolationException If validation fails
	 */
	private Villain validatePartialUpdate(Villain villain) {
		var violations = this.validator.validate(villain);

		if ((violations != null) && !violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}

		return villain;
	}

	public void deleteAllVillains() {
		List<Villain> villains = Villain.listAll();
		villains.stream()
			.map(v -> v.id)
			.forEach(this::deleteVillain);
	}

	public void deleteVillain(Long id) {
		Villain.deleteById(id);
	}
}
