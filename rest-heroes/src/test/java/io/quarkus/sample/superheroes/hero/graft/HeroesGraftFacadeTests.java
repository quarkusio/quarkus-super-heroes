package io.quarkus.sample.superheroes.hero.graft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.hero.model.HeroDto;
import io.quarkus.sample.superheroes.hero.service.HeroService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import io.smallrye.mutiny.Uni;

@QuarkusTest
class HeroesGraftFacadeTests {

  private static final long DEFAULT_ID = 1L;

  @Inject
  HeroesGraftFacade facade;

  @InjectMock
  HeroService heroService;

  @Test
  void helloDoesNotCallService() {
    assertThat(facade.hello()).isEqualTo(HeroService.HELLO_MESSAGE);
    verifyNoInteractions(heroService);
  }

  @Test
  void shouldNotGetUnknownHero() {
    when(heroService.findHeroApiById(DEFAULT_ID))
      .thenReturn(Uni.createFrom().item(Optional.empty()));

    assertThat(facade.getHero(DEFAULT_ID)).isEmpty();

    verify(heroService).findHeroApiById(DEFAULT_ID);
  }

  @Test
  void shouldGetRandomHeroNotFound() {
    when(heroService.findRandomHeroApi())
      .thenReturn(Uni.createFrom().item(Optional.empty()));

    assertThat(facade.getRandomHero()).isEmpty();

    verify(heroService).findRandomHeroApi();
  }

  @Test
  void shouldListHeroesWithFilter() {
    var dto = new HeroDto().setId(DEFAULT_ID).setName("name").setLevel(10);
    when(heroService.listHeroes("name"))
      .thenReturn(Uni.createFrom().item(List.of(dto)));

    assertThat(facade.getAllHeroes("name"))
      .singleElement()
      .extracting(HeroDto::getName)
      .isEqualTo("name");

    verify(heroService).listHeroes("name");
  }
}
