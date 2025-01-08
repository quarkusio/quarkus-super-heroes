package io.quarkus.sample.superheroes.hero.rest;

import static org.assertj.core.api.Assertions.*;

import java.net.URL;
import java.util.List;

import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

import io.quarkus.sample.superheroes.hero.Hero;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;

@QuarkusTest
@WithPlaywright(recordVideoDir = "target/playwright", slowMo = 500)
class UIResourceTests {
  private static final int NB_HEROES = 100;
  private static final Hero SPIDERMAN = getSpiderman();

  @InjectPlaywright
  BrowserContext browserContext;

  @TestHTTPResource("/")
  URL index;

  @Test
  void indexLoads() {
    var page = loadPage();

    assertThat(page.title())
      .isNotNull()
      .isEqualTo("Heroes List");
  }

  @Test
  void correctTable() {
    var table = getAndVerifyTable(NB_HEROES);

    assertThat(table)
      .isNotNull();

    var tableColumns = table.getByRole(AriaRole.COLUMNHEADER).all();
    assertThat(tableColumns)
      .isNotNull()
      .hasSize(6)
      .extracting(Locator::textContent)
      .containsExactly(
        "ID",
        "Name",
        "Picture",
        "Other Name",
        "Level",
        "Powers"
      );
  }

  @Test
  void tableFilters() {
    var page = loadPage();
    getAndVerifyTable(page, NB_HEROES);

    // Fill in the filter
    page.getByPlaceholder("Filter by name").fill(SPIDERMAN.getName());

    // Click the filter button
    page.getByText("Filter Heroes").click();

    // Get and verify the correct thing shows after the filter
    var table = getAndVerifyTable(page, 1);
    var tableRows = table.getByRole(AriaRole.ROW).all();

    assertThat(tableRows)
      .isNotNull()
      .hasSize(1);

    var tableCells = tableRows.get(0).getByRole(AriaRole.CELL).all();
    assertThat(tableCells)
      .isNotNull()
      .hasSize(6);

    // For the ID field, just test that it's there
    assertThat(tableCells.get(0).textContent())
      .isNotNull()
      .isNotEmpty();

    // For the Name, Picture, Other Name, Level, and powers fields, assert the values
    var textValues = List.of(
      tableCells.get(1).textContent(),
      tableCells.get(2).getByRole(AriaRole.LINK).getAttribute("href"),
      tableCells.get(3).textContent(),
      tableCells.get(4).textContent(),
      tableCells.get(5).textContent()
    );

    assertThat(textValues)
      .satisfies(name -> assertThat(name).isEqualTo(SPIDERMAN.getName()), atIndex(0))
      .satisfies(picture -> assertThat(picture).isEqualTo(SPIDERMAN.getPicture()), atIndex(1))
      .satisfies(otherName -> assertThat(otherName).isEqualTo(SPIDERMAN.getOtherName()), atIndex(2))
      .satisfies(level -> assertThat(level).isEqualTo(String.valueOf(SPIDERMAN.getLevel())), atIndex(3))
      .satisfies(powers -> assertThat(powers).contains(SPIDERMAN.getPowers()), atIndex(4));
  }

  private Page loadPage() {
    var page = this.browserContext.newPage();
    var response = page.navigate(this.index.toString());

    assertThat(response)
      .isNotNull()
      .extracting(Response::status)
      .isEqualTo(Status.OK.getStatusCode());

    return page;
  }

  private Locator getAndVerifyTable(Page page, int expectedNumRows) {
    var table = page.getByRole(AriaRole.GRID);

    assertThat(table)
      .isNotNull();

    var tableRowCount = table.getByRole(AriaRole.ROW).count();
    assertThat(tableRowCount)
      .isEqualTo(expectedNumRows);

    return table;
  }

  private Locator getAndVerifyTable(int expectedNumRows) {
    return getAndVerifyTable(loadPage(), expectedNumRows);
  }

  private static Hero getSpiderman() {
    var spiderman = new Hero();
    spiderman.setName("Spider-Man");
    spiderman.setOtherName("Peter Parker");
    spiderman.setPicture("https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/peter-parker--4751586759819899264.jpg");
    spiderman.setPowers("Acrobatics, Agility, Durability, Enhanced Senses, Intelligence, Reflexes, Stamina, Super Speed, Super Strength, Accelerated Healing, Animal Attributes, Animal Oriented Powers, Cold Resistance, Dexterity, Endurance, Enhanced Hearing, Enhanced Memory, Enhanced Sight, Enhanced Smell, Enhanced Touch, Gliding, Heat Resistance, Indomitable Will, Jump, Pressure Points, Stealth, Surface Scaling, Toxin and Disease Resistance, Wallcrawling, Weapon-based Powers, Web Creation, Danger Sense, Energy Blasts");
    spiderman.setLevel(93);

    return spiderman;
  }
}
