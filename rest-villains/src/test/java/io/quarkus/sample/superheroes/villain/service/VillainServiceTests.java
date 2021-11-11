package io.quarkus.sample.superheroes.villain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.sample.superheroes.villain.Villain;
import io.quarkus.sample.superheroes.villain.config.VillainConfig;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;

import io.smallrye.config.SmallRyeConfig;

@QuarkusTest
class VillainServiceTests {
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
	private static final double DEFAULT_MULTIPLIER = 0.5;

	@Inject
	VillainService villainService;

	@Inject
	VillainConfig villainConfig;

	@Test
	public void findAllVillainsNoneFound() {
		PanacheMock.mock(Villain.class);
		when(Villain.listAll()).thenReturn(List.of());

		assertThat(this.villainService.findAllVillains())
			.isNotNull()
			.isEmpty();

		PanacheMock.verify(Villain.class).listAll();
		PanacheMock.verifyNoMoreInteractions(Villain.class);
	}

	@Test
	public void findAllVillains() {
		PanacheMock.mock(Villain.class);
		when(Villain.listAll()).thenReturn(List.of(createDefaultVillian()));

		assertThat(this.villainService.findAllVillains())
			.isNotNull()
			.isNotEmpty()
			.hasSize(1)
			.extracting(
				"id",
				"name",
				"otherName",
				"level",
				"picture",
				"powers"
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

		PanacheMock.verify(Villain.class).listAll();
		PanacheMock.verifyNoMoreInteractions(Villain.class);
	}

	@Test
	public void findVillainByIdFound() {
		PanacheMock.mock(Villain.class);
		when(Villain.findByIdOptional(eq(DEFAULT_ID)))
			.thenReturn(Optional.of(createDefaultVillian()));

		assertThat(this.villainService.findVillainById(DEFAULT_ID))
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				"id",
				"name",
				"otherName",
				"level",
				"picture",
				"powers"
			)
			.containsExactly(
				DEFAULT_ID,
				DEFAULT_NAME,
				DEFAULT_OTHER_NAME,
				DEFAULT_LEVEL,
				DEFAULT_PICTURE,
				DEFAULT_POWERS
			);

		PanacheMock.verify(Villain.class).findByIdOptional(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Villain.class);
	}

	@Test
	public void findVillainByIdNotFound() {
		PanacheMock.mock(Villain.class);
		when(Villain.findByIdOptional(eq(DEFAULT_ID))).thenReturn(Optional.empty());

		assertThat(this.villainService.findVillainById(DEFAULT_ID))
			.isNotNull()
			.isNotPresent();

		PanacheMock.verify(Villain.class).findByIdOptional(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Villain.class);
	}

	@Test
	public void findRandomVillain() {
		PanacheMock.mock(Villain.class);
		when(Villain.findRandom()).thenReturn(createDefaultVillian());

		assertThat(this.villainService.findRandomVillain())
			.isNotNull()
			.extracting(
				"id",
				"name",
				"otherName",
				"level",
				"picture",
				"powers"
			)
			.containsExactly(
				DEFAULT_ID,
				DEFAULT_NAME,
				DEFAULT_OTHER_NAME,
				DEFAULT_LEVEL,
				DEFAULT_PICTURE,
				DEFAULT_POWERS
			);

		PanacheMock.verify(Villain.class).findRandom();
		PanacheMock.verifyNoMoreInteractions(Villain.class);
	}

	@Test
	public void persistNullVillain() {
		PanacheMock.mock(Villain.class);
		var cve = catchThrowableOfType(() -> this.villainService.persistVillain(null), ConstraintViolationException.class);

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

		PanacheMock.verifyNoInteractions(Villain.class);
	}

	@Test
	public void persistInvalidVillain() {
		PanacheMock.mock(Villain.class);
		var villain = createDefaultVillian();
		villain.name = "a";

		var cve = catchThrowableOfType(() -> this.villainService.persistVillain(villain), ConstraintViolationException.class);

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

		PanacheMock.verifyNoInteractions(Villain.class);
	}

	@Test
	public void persistVillain() {
		PanacheMock.mock(Villain.class);
		PanacheMock.doNothing()
			.when(Villain.class).persist(any(Villain.class), any());
		when(this.villainConfig.level().multiplier()).thenReturn(DEFAULT_MULTIPLIER);

		assertThat(this.villainService.persistVillain(createDefaultVillian()))
			.isNotNull()
			.extracting(
				"id",
				"name",
				"otherName",
				"level",
				"picture",
				"powers"
			)
			.containsExactly(
				DEFAULT_ID,
				DEFAULT_NAME,
				DEFAULT_OTHER_NAME,
				(int) (DEFAULT_LEVEL * DEFAULT_MULTIPLIER),
				DEFAULT_PICTURE,
				DEFAULT_POWERS
			);

		PanacheMock.verify(Villain.class).persist(any(Villain.class), any());
		PanacheMock.verifyNoMoreInteractions(Villain.class);
	}

	@Test
	public void updateNullVillain() {
		PanacheMock.mock(Villain.class);
		var cve = catchThrowableOfType(() -> this.villainService.updateVillain(null), ConstraintViolationException.class);

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

		PanacheMock.verifyNoInteractions(Villain.class);
	}

	@Test
	public void updateInvalidVillain() {
		PanacheMock.mock(Villain.class);
		var villain = createDefaultVillian();
		villain.name = null;

		var cve = catchThrowableOfType(() -> this.villainService.updateVillain(villain), ConstraintViolationException.class);

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

		PanacheMock.verifyNoInteractions(Villain.class);
	}

	@Test
	public void updateVillain() {
		PanacheMock.mock(Villain.class);
		when(Villain.findById(eq(DEFAULT_ID))).thenReturn(createDefaultVillian());

		assertThat(this.villainService.updateVillain(createUpdatedVillain()))
			.isNotNull()
			.extracting(
				"id",
				"name",
				"otherName",
				"level",
				"picture",
				"powers"
			)
			.containsExactly(
				DEFAULT_ID,
				UPDATED_NAME,
				UPDATED_OTHER_NAME,
				UPDATED_LEVEL,
				UPDATED_PICTURE,
				UPDATED_POWERS
			);

		PanacheMock.verify(Villain.class).findById(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Villain.class);
	}

	@Test
	public void deleteVillain() {
		PanacheMock.mock(Villain.class);
		when(Villain.deleteById(eq(DEFAULT_ID))).thenReturn(true);

		this.villainService.deleteVillain(DEFAULT_ID);

		PanacheMock.verify(Villain.class).deleteById(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Villain.class);
	}

	private static Villain createDefaultVillian() {
		Villain villain = new Villain();
		villain.id = DEFAULT_ID;
		villain.name = DEFAULT_NAME;
		villain.otherName = DEFAULT_OTHER_NAME;
		villain.picture = DEFAULT_PICTURE;
		villain.powers = DEFAULT_POWERS;
		villain.level = DEFAULT_LEVEL;

		return villain;
	}

	public static Villain createUpdatedVillain() {
		Villain villain = createDefaultVillian();
		villain.name = UPDATED_NAME;
		villain.otherName = UPDATED_OTHER_NAME;
		villain.picture = UPDATED_PICTURE;
		villain.powers = UPDATED_POWERS;
		villain.level = UPDATED_LEVEL;

		return villain;
	}

	public static class VillainConfigMockProducer {
		@Inject
		Config config;

		@Produces
		@ApplicationScoped
		@Mock
		VillainConfig villainConfig() {
			var vc = this.config.unwrap(SmallRyeConfig.class)
				.getConfigMapping(VillainConfig.class);

			var vcSpy = spy(vc);
			when(vcSpy.level()).thenReturn(spy(vc.level()));

			return vcSpy;
		}
	}
}
