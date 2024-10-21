package io.quarkus.sample.superheroes.villain.rest;

import static org.assertj.core.api.Assertions.*;

import java.net.URL;
import java.util.List;

import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;

import io.quarkus.sample.superheroes.villain.Villain;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;

@QuarkusTest
@WithPlaywright
@VirtualThreadUnit
@ShouldNotPin
class UIResourceTests {
  private static final int NB_VILLAINS = 100;
  private static final Villain DARTH_VADER = getDarthVader();

  @InjectPlaywright
  BrowserContext browserContext;

  @TestHTTPResource("/")
  URL index;

  @Test
  void indexLoads() {
    var page = loadPage();

    assertThat(page.title())
      .isNotNull()
      .isEqualTo("Villains List");
  }

  @Test
  void correctTable() {
    var table = getAndVerifyTable(NB_VILLAINS);

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
    getAndVerifyTable(page, NB_VILLAINS);

    // Fill in the filter
    page.getByPlaceholder("Filter by name").fill(DARTH_VADER.name);

    // Click the filter button
    page.getByText("Filter Villains").click();

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
      .satisfies(name -> assertThat(name).isEqualTo(DARTH_VADER.name), atIndex(0))
      .satisfies(picture -> assertThat(picture).isEqualTo(DARTH_VADER.picture), atIndex(1))
      .satisfies(otherName -> assertThat(otherName).isEqualTo(DARTH_VADER.otherName), atIndex(2))
      .satisfies(level -> assertThat(level).isEqualTo(String.valueOf(DARTH_VADER.level)), atIndex(3))
      .satisfies(powers -> assertThat(powers).contains(DARTH_VADER.powers), atIndex(4));
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

  private static Villain getDarthVader() {
    var vader = new Villain();
    vader.name = "Darth Vader";
    vader.otherName = "Anakin Skywalker";
    vader.picture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/anakin-skywalker--8429855148488965479.jpg";
    vader.powers = "Afterimage Creation, Attack Reflection, Awakened Power, Berserk Mode, Clairvoyance, Cloaking, Danger Sense, Darkforce Manipulation, Durability, Electricity Absorption, Emotional Power Up, Endurance, Energy Absorption, Energy Manipulation, Enhanced Senses, Fear Inducement, Force Fields, Force Ghost, Force Speed, Heal, Heat Resistance, Homing Attack, Illusions, Intelligence, Jump, Levitation, Light Control, Longevity, Master Tactician, Memory Manipulation, Mind Control, Power Sense, Power Suit, Precognition, Psionic Powers, Psychometry, Rage Power, Reflexes, Robotic Engineering, Spatial Awareness, Stamina, Stealth, Subatomic Manipulation, Super Speed, Super Strength, Technopath/Cyberpath, Telekinesis, Telepathy, The Force, Toxin and Disease Resistance, Vehicular Mastery, Weapons Master, Absorption, Acrobatics, Attack Negation, Bloodlust, Body Puppetry, Chi Manipulation, Cold Resistance, Energy Blasts, Energy Resistance, Fire Resistance, Indomitable Will, Insanity, Marksmanship, Master Martial Artist, Mind Control Resistance, Non-Physical Interaction, Power Absorption Immunity, Power Augmentation, Radiation Immunity, Telepathy Resistance, Teleportation, Vision - Night, Vision - Thermal, Wind Control, Accelerated Healing, Agility, Animal Control, Aura, Black Hole Manipulation, Death Manipulation, Dexterity, Electricity Resistance, Empathy, Enhanced Sight, Immortality, Invisibility, Matter Manipulation, Molecular Manipulation, Postcognition, Vitakinesis, Weapon-based Powers";
    vader.level = 79000;

    return vader;
  }
}
