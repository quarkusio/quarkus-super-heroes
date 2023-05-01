package io.quarkus.sample.superheroes.villain.rest;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.sample.superheroes.villain.Villain;
import io.quarkus.sample.superheroes.villain.service.VillainService;

import io.smallrye.common.annotation.Blocking;

@Path("/")
public class UIResource {
  @Inject
  VillainService service;

  @CheckedTemplate
  static class Templates {
    static native TemplateInstance index(List<Villain> villains);
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Blocking
  public TemplateInstance get(@QueryParam("name_filter") Optional<String> nameFilter) {
    var villains = nameFilter
      .map(this.service::findAllVillainsHavingName)
      .orElseGet(this.service::findAllVillains);

    return Templates.index(villains);
  }
}
