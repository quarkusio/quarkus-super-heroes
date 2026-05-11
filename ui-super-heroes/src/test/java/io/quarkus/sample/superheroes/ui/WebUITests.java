package io.quarkus.sample.superheroes.ui;

import static io.restassured.RestAssured.get;
import static io.restassured.http.ContentType.HTML;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;
import static org.hamcrest.Matchers.matchesPattern;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import jakarta.ws.rs.HttpMethod;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.GetByRoleOptions;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkiverse.quinoa.testing.QuinoaTestProfiles;
import io.restassured.RestAssured;

/**
 * Tests the served javascript application.
 * By default, the Web UI is not build/served in @QuarkusTest. The goal is to be able to test
 * your api without having to wait for the Web UI build.
 * The `Enable` test profile enables the Web UI (build and serve).
 */
@QuarkusTest
@TestProfile(QuinoaTestProfiles.Enable.class)
@WithPlaywright(recordVideoDir = "target/playwright", slowMo = 500)
class WebUITests {
	private static final String IMAGE_LOCATION_TEMPLATE = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/%s";

	@InjectPlaywright
	BrowserContext context;

	@Test
	void webApplicationEndpoint() {
		get("/").then()
      .statusCode(OK.getStatusCode())
      .contentType(HTML)
      .body(matchesPattern(Pattern.compile(".*<div id=\"root\">.*", Pattern.DOTALL)));
	}

	@Test
	void pageWorks() {
		var page = loadPage();

		fighterCardsOk(page);
		locationCardsOk(page);
		buttonsOk(page, false);
		var table = tableOk(page);

    // Now click the fight button
    getFightButton(page).click();

		page.waitForResponse(
			successfulPostResponse("/api/fights"),
			() -> performFightOk(page)
		);
	}

	private static Predicate<Response> successfulPostResponse(String uriPath) {
		return response -> response.url().contains(uriPath) &&
			(response.status() == OK.getStatusCode()) &&
			HttpMethod.POST.equals(response.request().method());
	}

	private Page loadPage() {
		var page = this.context.newPage();
		var response = page.navigate("%s:%d".formatted(RestAssured.baseURI, RestAssured.port));

		assertThat(response).isNotNull()
      .extracting(Response::status)
      .isEqualTo(OK.getStatusCode());

		page.waitForLoadState(LoadState.NETWORKIDLE);

		PlaywrightAssertions.assertThat(page)
      .hasTitle("SuperHeroes");

		return page;
	}

	private Locator getTable(Page page) {
		var table = page.getByRole(AriaRole.GRID);

		assertThat(table)
      .isNotNull();

		table.scrollIntoViewIfNeeded();

		PlaywrightAssertions.assertThat(table)
      .isVisible();

		return table;
	}

	private Locator getTableAndVerifyTable(Page page) {
		var table = getTable(page);

		assertThat(table.getByRole(AriaRole.ROW).count())
      .isEqualTo(1);

		return table;
	}

	private Locator tableOk(Page page) {
		var table = getTableAndVerifyTable(page);
    var tableCells = table.getByRole(AriaRole.ROW)
                          .all()
                          .getFirst()
                          .getByRole(AriaRole.CELL)
                          .all();

    assertThat(tableCells).isNotNull()
      .hasSize(5);

    // For the ID & Date fields, just test that it's there
    assertThat(tableCells.get(0).textContent())
      .isNotNull()
      .isNotEmpty();

    assertThat(tableCells.get(1).textContent())
      .isNotNull()
      .isNotEmpty();

    // For the Winner, Loser, & Location fields, assert the values
    var textValues = List.of(
      tableCells.get(2).textContent().strip(),
      tableCells.get(3).textContent().strip(),
      tableCells.get(4).textContent().strip(),
      tableCells.get(4).getByRole(AriaRole.LINK).getAttribute("href")
    );

    assertThat(textValues)
      .satisfies(winner -> assertThat(winner).isEqualTo("Luke Skywalker"), atIndex(0))
      .satisfies(loser -> assertThat(loser).isEqualTo("Darth Vader"), atIndex(1))
      .satisfies(location -> assertThat(location).isEqualTo("Gotham City"), atIndex(2))
      .satisfies(locationUrl -> assertThat(locationUrl).isEqualTo(IMAGE_LOCATION_TEMPLATE.formatted("locations/gotham_city.jpg")), atIndex(3));

		return table;
  }

	private void fighterCardsOk(Page page) {
		PlaywrightAssertions.assertThat(page.getByRole(AriaRole.HEADING, new GetByRoleOptions().setName("Luke Skywalker (Click for")))
		                    .isVisible();

		var lukePicture = page.getByRole(AriaRole.IMG, new GetByRoleOptions().setName("the hero"));
		PlaywrightAssertions.assertThat(lukePicture)
		                    .isVisible();

		assertThat(lukePicture.getAttribute("src"))
      .isEqualTo(IMAGE_LOCATION_TEMPLATE.formatted("luke-skywalker-2563509063968639219.jpg"));

		PlaywrightAssertions.assertThat(page.locator(".hero-winner-card"))
		                    .not().isVisible();

		PlaywrightAssertions.assertThat(page.getByRole(AriaRole.HEADING, new GetByRoleOptions().setName("Darth Vader (Click for")))
		                    .isVisible();

		var darthPicture = page.getByRole(AriaRole.IMG, new GetByRoleOptions().setName("the villain"));
		PlaywrightAssertions.assertThat(darthPicture)
		                    .isVisible();

		assertThat(darthPicture.getAttribute("src"))
      .isEqualTo(IMAGE_LOCATION_TEMPLATE.formatted("anakin-skywalker--8429855148488965479.jpg"));

		PlaywrightAssertions.assertThat(page.locator(".villain-winner-card"))
		                    .not().isVisible();
	}

	private void buttonsOk(Page page, boolean shouldNarrateBeVisible) {
		PlaywrightAssertions.assertThat(page.getByText("NEW FIGHTERS"))
		                    .isVisible();

		PlaywrightAssertions.assertThat(page.getByText("NEW LOCATION"))
		                    .isVisible();

		PlaywrightAssertions.assertThat(getFightButton(page))
		                    .isVisible();

		var narrateButton = getNarrateButton(page);
		assertThat(narrateButton)
			.isNotNull();

		if (shouldNarrateBeVisible) {
			PlaywrightAssertions.assertThat(narrateButton)
			                    .isVisible();
		}
		else {
			PlaywrightAssertions.assertThat(narrateButton)
				.not().isVisible();
		}
	}

  private Locator getFightButton(Page page) {
    return page.getByText("FIGHT !");
  }

	private Locator getNarrateButton(Page page) {
		return page.getByText("NARRATE THE FIGHT");
	}

	private void locationCardsOk(Page page) {
		var locationPicture = page.getByRole(AriaRole.IMG, new GetByRoleOptions().setName("Location"));
		PlaywrightAssertions.assertThat(locationPicture)
		                    .isVisible();

		assertThat(locationPicture.getAttribute("src"))
      .isEqualTo(IMAGE_LOCATION_TEMPLATE.formatted("locations/gotham_city.jpg"));

		var locationName = page.getByTestId("location-name");
		PlaywrightAssertions.assertThat(locationName)
		                    .isVisible();

		assertThat(locationName.textContent())
      .isEqualTo("Gotham City: ");
	}

	private void performFightOk(Page page) {
		PlaywrightAssertions.assertThat(page.getByText("Winner is Luke Skywalker!"))
				                    .isVisible();

		// Verify the narrate button is there now
		buttonsOk(page, true);

		var heroWinner = page.locator("div.hero-winner-card:not(.card-pf-body)");
		PlaywrightAssertions.assertThat(heroWinner)
		                    .isVisible();

		assertThat(heroWinner.count())
			.isEqualTo(1);

		var narrateButton = getNarrateButton(page);
		narrateButton.scrollIntoViewIfNeeded();
		narrateButton.click();

		page.waitForResponse(
			successfulPostResponse("/api/fights/narrate"),
			() -> fightNarrationOk(page)
		);
	}

	private void fightNarrationOk(Page page) {
		PlaywrightAssertions.assertThat(page.getByText("This is the narration for the fight"))
						                    .isVisible();

		var generateNarrationImageButton = page.getByRole(AriaRole.BUTTON, new GetByRoleOptions().setName("GENERATE NARRATION IMAGE"));
		generateNarrationImageButton.scrollIntoViewIfNeeded();

		PlaywrightAssertions.assertThat(generateNarrationImageButton)
		                    .isVisible();

		generateNarrationImageButton.click();

		var narrationImage = page.waitForSelector("img[alt='Generated fight']");
		narrationImageGenerationOk(page, narrationImage);
	}

	private void narrationImageGenerationOk(Page page, ElementHandle narrationImage) {
		var generatedCaption = page.getByText("Generated Image Caption");
		generatedCaption.scrollIntoViewIfNeeded();

		PlaywrightAssertions.assertThat(generatedCaption)
		                    .isVisible();

		narrationImage.scrollIntoViewIfNeeded();

		assertThat(narrationImage)
			.isNotNull()
			.extracting(
				ElementHandle::isVisible,
				element -> element.getAttribute("src")
			)
			.containsExactly(
				true,
				"https://dummyimage.com/240x320/1e8fff/ffffff&text=Fallback+Image"
			);
	}
}
