package io.quarkus.sample.superheroes.villain.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.ParameterizedTest.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.sample.superheroes.villain.Villain;
import io.quarkus.sample.superheroes.villain.config.VillainConfig;
import io.quarkus.sample.superheroes.villain.mapping.VillainFullUpdateMapper;
import io.quarkus.sample.superheroes.villain.mapping.VillainPartialUpdateMapper;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;

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

	@InjectSpy
	VillainPartialUpdateMapper villainPartialUpdateMapper;

	@InjectSpy
	VillainFullUpdateMapper villainFullUpdateMapper;

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

  @ParameterizedTest(name = DISPLAY_NAME_PLACEHOLDER + "[" + INDEX_PLACEHOLDER + "] (" + ARGUMENTS_WITH_NAMES_PLACEHOLDER + ")")
  @ValueSource(strings = { "name" })
  @NullSource
  public void findAllVillainsHavingNameNoneFound(String name) {
    PanacheMock.mock(Villain.class);
    when(Villain.listAllWhereNameLike(eq(name))).thenReturn(List.of());

    assertThat(this.villainService.findAllVillainsHavingName(name))
      .isNotNull()
      .isEmpty();

    PanacheMock.verify(Villain.class).listAllWhereNameLike(eq(name));
    PanacheMock.verifyNoMoreInteractions(Villain.class);
  }

  @Test
  public void findAllVillainsHavingName() {
    PanacheMock.mock(Villain.class);
    when(Villain.listAllWhereNameLike(eq("name"))).thenReturn(List.of(createDefaultVillian()));

    assertThat(this.villainService.findAllVillainsHavingName("name"))
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

    PanacheMock.verify(Villain.class).listAllWhereNameLike(eq("name"));
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
	public void findRandomVillainNotFound() {
		PanacheMock.mock(Villain.class);
		when(Villain.findRandom()).thenReturn(Optional.empty());

		assertThat(this.villainService.findRandomVillain())
			.isNotNull()
			.isEmpty();

		PanacheMock.verify(Villain.class).findRandom();
		PanacheMock.verifyNoMoreInteractions(Villain.class);
	}

	@Test
	public void findRandomVillainFound() {
		PanacheMock.mock(Villain.class);
		when(Villain.findRandom()).thenReturn(Optional.of(createDefaultVillian()));

		assertThat(this.villainService.findRandomVillain())
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
	public void fullyUpdateNullVillain() {
		PanacheMock.mock(Villain.class);
		var cve = catchThrowableOfType(() -> this.villainService.replaceVillain(null), ConstraintViolationException.class);

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
		Mockito.verifyNoInteractions(this.villainFullUpdateMapper, this.villainPartialUpdateMapper);
	}

	@Test
	public void fullyUpdateInvalidVillain() {
		PanacheMock.mock(Villain.class);
		var villain = createDefaultVillian();
		villain.name = null;

		var cve = catchThrowableOfType(() -> this.villainService.replaceVillain(villain), ConstraintViolationException.class);

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
		Mockito.verifyNoInteractions(this.villainFullUpdateMapper, this.villainPartialUpdateMapper);
	}

	@Test
	public void fullyUpdateNotFoundVillain() {
		PanacheMock.mock(Villain.class);
		when(Villain.findByIdOptional(eq(DEFAULT_ID))).thenReturn(Optional.empty());

		assertThat(this.villainService.replaceVillain(createUpdatedVillain()))
			.isNotNull()
			.isNotPresent();

		PanacheMock.verify(Villain.class).findByIdOptional(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Villain.class);
		Mockito.verifyNoInteractions(this.villainPartialUpdateMapper, this.villainFullUpdateMapper);
	}

	@Test
	public void fullyUpdateVillain() {
		PanacheMock.mock(Villain.class);
		when(Villain.findByIdOptional(eq(DEFAULT_ID))).thenReturn(Optional.of(createDefaultVillian()));

		assertThat(this.villainService.replaceVillain(createUpdatedVillain()))
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
				UPDATED_NAME,
				UPDATED_OTHER_NAME,
				UPDATED_LEVEL,
				UPDATED_PICTURE,
				UPDATED_POWERS
			);

		PanacheMock.verify(Villain.class).findByIdOptional(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Villain.class);
		Mockito.verify(this.villainFullUpdateMapper).mapFullUpdate(any(Villain.class), any(Villain.class));
		Mockito.verifyNoInteractions(this.villainPartialUpdateMapper);
	}

	@Test
	public void partiallyUpdateNullVillain() {
		PanacheMock.mock(Villain.class);
		var cve = catchThrowableOfType(() -> this.villainService.partialUpdateVillain(null), ConstraintViolationException.class);

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
		Mockito.verifyNoInteractions(this.villainFullUpdateMapper, this.villainPartialUpdateMapper);
	}

	@Test
	public void partiallyUpdateInvalidVillain() {
		PanacheMock.mock(Villain.class);
		when(Villain.findByIdOptional(eq(DEFAULT_ID))).thenReturn(Optional.of(createDefaultVillian()));
		var villain = createDefaultVillian();
		villain.name = "a";

		var cve = catchThrowableOfType(() -> this.villainService.partialUpdateVillain(villain), ConstraintViolationException.class);

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

		PanacheMock.verify(Villain.class).findByIdOptional(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Villain.class);
		Mockito.verify(this.villainPartialUpdateMapper).mapPartialUpdate(any(Villain.class), any(Villain.class));
		Mockito.verifyNoInteractions(this.villainFullUpdateMapper);
	}

	@Test
	public void partiallyUpdateNotFoundVillain() {
		PanacheMock.mock(Villain.class);
		when(Villain.findByIdOptional(eq(DEFAULT_ID))).thenReturn(Optional.empty());

		assertThat(this.villainService.partialUpdateVillain(createPartialUpdatedVillain()))
			.isNotNull()
			.isNotPresent();

		PanacheMock.verify(Villain.class).findByIdOptional(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Villain.class);
		Mockito.verifyNoInteractions(this.villainFullUpdateMapper, this.villainPartialUpdateMapper);
	}

	@Test
	public void partiallyUpdateVillain() {
		PanacheMock.mock(Villain.class);
		when(Villain.findByIdOptional(eq(DEFAULT_ID))).thenReturn(Optional.of(createDefaultVillian()));

		assertThat(this.villainService.partialUpdateVillain(createPartialUpdatedVillain()))
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
				UPDATED_PICTURE,
				UPDATED_POWERS
			);

		PanacheMock.verify(Villain.class).findByIdOptional(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Villain.class);
		Mockito.verify(this.villainPartialUpdateMapper).mapPartialUpdate(any(Villain.class), any(Villain.class));
		Mockito.verifyNoInteractions(this.villainFullUpdateMapper);
	}

	@Test
	public void deleteVillain() {
		PanacheMock.mock(Villain.class);
		when(Villain.deleteById(eq(DEFAULT_ID))).thenReturn(true);

		this.villainService.deleteVillain(DEFAULT_ID);

		PanacheMock.verify(Villain.class).deleteById(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Villain.class);
	}

	@Test
	public void deleteAllVillains() {
		var v1 = createDefaultVillian();
		var v2 = createUpdatedVillain();
		v2.id = v1.id + 1;

		PanacheMock.mock(Villain.class);
		when(Villain.deleteById(anyLong())).thenReturn(true);
		when(Villain.listAll()).thenReturn(List.of(v1, v2));

		this.villainService.deleteAllVillains();

		PanacheMock.verify(Villain.class).listAll();
		PanacheMock.verify(Villain.class).deleteById(eq(v1.id));
		PanacheMock.verify(Villain.class).deleteById(eq(v2.id));
		PanacheMock.verifyNoMoreInteractions(Villain.class);
	}

  @Test
  public void replaceAllVillains() {
    var v1 = createDefaultVillian();
		var v2 = createUpdatedVillain();
		v2.id = v1.id + 1;

    var villains = List.of(createDefaultVillian(), createPartialUpdatedVillain());
    villains.forEach(v -> v.id = null);

		PanacheMock.mock(Villain.class);
		when(Villain.deleteById(anyLong())).thenReturn(true);
		when(Villain.listAll()).thenReturn(List.of(v1, v2));
    PanacheMock.doNothing().when(Villain.class).persist(anyIterable());

    this.villainService.replaceAllVillains(villains);

    PanacheMock.verify(Villain.class).listAll();
		PanacheMock.verify(Villain.class).deleteById(eq(v1.id));
		PanacheMock.verify(Villain.class).deleteById(eq(v2.id));
		PanacheMock.verify(Villain.class).persist(anyIterable());
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

	public static Villain createPartialUpdatedVillain() {
		Villain villain = createDefaultVillian();
		villain.picture = UPDATED_PICTURE;
		villain.powers = UPDATED_POWERS;

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
