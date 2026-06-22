package io.quarkus.sample.superheroes.narration.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.FightImage;
import io.quarkus.sample.superheroes.narration.ImageGenerationRequest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class NarrationMapperTests {
	@Inject
	NarrationMapper narrationMapper;

	@Test
	void toFight() {
		var apiModel = new io.quarkus.sample.superheroes.narration.api.model.Fight()
			.winnerName("Chewbacca")
			.winnerLevel(5)
			.winnerPowers("Big, hairy, strong")
			.winnerTeam("heroes")
			.loserName("Wanderer")
			.loserLevel(3)
			.loserPowers("Not strong")
			.loserTeam("villains")
			.location(new io.quarkus.sample.superheroes.narration.api.model.FightLocation()
				.name("Gotham City")
				.description("An American city"));

		var fight = this.narrationMapper.toFight(apiModel);

		assertThat(fight)
			.isNotNull()
			.extracting(Fight::winnerName, Fight::winnerLevel, Fight::winnerPowers, Fight::winnerTeam,
				Fight::loserName, Fight::loserLevel, Fight::loserPowers, Fight::loserTeam)
			.containsExactly("Chewbacca", 5, "Big, hairy, strong", "heroes",
				"Wanderer", 3, "Not strong", "villains");

		assertThat(fight.location())
			.isNotNull()
			.extracting(Fight.FightLocation::name, Fight.FightLocation::description)
			.containsExactly("Gotham City", "An American city");
	}

	@Test
	void toFightNull() {
		assertThat(this.narrationMapper.toFight(null))
			.isNull();
	}

	@Test
	void toApiFightImage() {
		var fightImage = new FightImage("https://example.com/image.png");
		var apiModel = this.narrationMapper.toApiFightImage(fightImage);

		assertThat(apiModel)
			.isNotNull()
			.extracting(io.quarkus.sample.superheroes.narration.api.model.FightImage::getImageUrl)
			.isEqualTo("https://example.com/image.png");
	}

	@Test
	void toApiFightImageNull() {
		assertThat(this.narrationMapper.toApiFightImage(null))
			.isNull();
	}

	@Test
	void toImageGenerationRequest() {
		var apiModel = new io.quarkus.sample.superheroes.narration.api.model.ImageGenerationRequest()
			.narration("A fierce battle unfolded")
			.winnerPictureUrl("https://example.com/winner.jpg")
			.loserPictureUrl("https://example.com/loser.jpg");

		var request = this.narrationMapper.toImageGenerationRequest(apiModel);

		assertThat(request)
			.isNotNull()
			.extracting(ImageGenerationRequest::narration, ImageGenerationRequest::winnerPictureUrl, ImageGenerationRequest::loserPictureUrl)
			.containsExactly("A fierce battle unfolded", "https://example.com/winner.jpg", "https://example.com/loser.jpg");
	}

	@Test
	void toImageGenerationRequestNull() {
		assertThat(this.narrationMapper.toImageGenerationRequest(null))
			.isNull();
	}
}
