package io.quarkus.sample.superheroes.villain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestTransaction
class VillainTests {
	private static final String DEFAULT_NAME = "Super Chocolatine";
	private static final String DEFAULT_OTHER_NAME = "Super Chocolatine chocolate in";
	private static final String DEFAULT_PICTURE = "super_chocolatine.png";
	private static final String DEFAULT_POWERS = "does not eat pain au chocolat";
	private static final int DEFAULT_LEVEL = 42;

	@Test
	public void findRandomFound() {
		Villain villain = new Villain();
		villain.name = DEFAULT_NAME;
		villain.otherName = DEFAULT_OTHER_NAME;
		villain.picture = DEFAULT_PICTURE;
		villain.powers = DEFAULT_POWERS;
		villain.level = DEFAULT_LEVEL;

		Villain.deleteAll();
		Villain.persist(villain);

		assertThat(Villain.count())
			.isEqualTo(1L);

		assertThat(villain)
			.isNotNull()
			.extracting(
				"name",
				"otherName",
				"level",
				"picture",
				"powers"
			)
			.containsExactly(
				DEFAULT_NAME,
				DEFAULT_OTHER_NAME,
				DEFAULT_LEVEL,
				DEFAULT_PICTURE,
				DEFAULT_POWERS
			);
	}
}
