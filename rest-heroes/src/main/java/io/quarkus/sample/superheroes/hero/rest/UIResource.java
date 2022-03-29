package io.quarkus.sample.superheroes.hero.rest;

import java.util.List;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.sample.superheroes.hero.Hero;
import io.quarkus.sample.superheroes.hero.service.HeroService;

import io.smallrye.mutiny.Uni;

@Path("/")
public class UIResource {
  private final HeroService heroService;

  public UIResource(HeroService heroService) {
    this.heroService = heroService;
  }

  @CheckedTemplate
  static class Templates {
    static native TemplateInstance index(List<Hero> heroes);
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public Uni<String> get(@QueryParam("name_filter") Optional<String> nameFilter) {
    return nameFilter
      .map(this.heroService::findAllHeroesHavingName)
      .orElseGet(this.heroService::findAllHeroes)
      .map(Templates::index)
      .flatMap(TemplateInstance::createUni);
  }
}
