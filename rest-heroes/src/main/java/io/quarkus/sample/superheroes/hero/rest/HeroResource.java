package io.quarkus.sample.superheroes.hero.rest;

import java.util.List;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.hero.api.model.Hero;
import io.quarkus.sample.superheroes.hero.api.model.HeroPatch;
import io.quarkus.sample.superheroes.hero.api.resources.HeroesResource;
import io.quarkus.sample.superheroes.hero.mapping.HeroMapper;
import io.quarkus.sample.superheroes.hero.service.HeroService;

import io.smallrye.mutiny.Uni;

public class HeroResource implements HeroesResource {
	private final HeroService heroService;
	private final HeroMapper heroMapper;

	@Context
	UriInfo uriInfo;

	public HeroResource(HeroService heroService, HeroMapper heroMapper) {
		this.heroService = heroService;
		this.heroMapper = heroMapper;
	}

	@Override
	public Uni<Response> getRandomHero() {
		return this.heroService.findRandomHero()
			.onItem().ifNotNull().transform(h -> {
				Log.debugf("Found random hero: %s", h);
				return Response.ok(this.heroMapper.toApiModel(h)).build();
			})
			.replaceIfNullWith(() -> {
				Log.debug("No random hero found");
				return Response.status(Status.NOT_FOUND).build();
			});
	}

	@Override
	public Uni<List<Hero>> getAllHeroes(String nameFilter) {
		var heroes = (nameFilter != null)
			? this.heroService.findAllHeroesHavingName(nameFilter)
			: this.heroService.findAllHeroes().replaceIfNullWith(List::of);

		return heroes
			.invoke(h -> Log.debugf("Total number of heroes: %d", h.size()))
			.map(this.heroMapper::toApiModelList);
	}

	@Override
	public Uni<Response> getHero(Long id) {
		return this.heroService.findHeroById(id)
			.onItem().ifNotNull().transform(h -> {
				Log.debugf("Found hero: %s", h);
				return Response.ok(this.heroMapper.toApiModel(h)).build();
			})
			.replaceIfNullWith(() -> {
				Log.debugf("No hero found with id %d", id);
				return Response.status(Status.NOT_FOUND).build();
			});
	}

	@Override
	public Uni<Response> createHero(Hero hero) {
		return this.heroService.persistHero(this.heroMapper.toEntity(hero))
			.map(h -> {
				var uri = this.uriInfo.getAbsolutePathBuilder().path(Long.toString(h.getId())).build();
				Log.debugf("New Hero created with URI %s", uri.toString());
				return Response.created(uri).build();
			});
	}

	@Override
	public Uni<Response> fullyUpdateHero(Long id, Hero hero) {
		var entity = this.heroMapper.toEntity(hero);

		if (entity.getId() == null) {
			entity.setId(id);
		}

		return this.heroService.replaceHero(entity)
			.onItem().ifNotNull().transform(h -> {
				Log.debugf("Hero replaced with new values %s", h);
				return Response.noContent().build();
			})
			.replaceIfNullWith(() -> {
				Log.debugf("No hero found with id %d", entity.getId());
				return Response.status(Status.NOT_FOUND).build();
			});
	}

	@Override
	public Uni<Response> replaceAllHeroes(List<Hero> heroes) {
		return this.heroService.replaceAllHeroes(this.heroMapper.toEntityList(heroes))
			.map(h -> {
				var uri = this.uriInfo.getAbsolutePathBuilder().build();
				Log.debugf("New Heroes created with URI %s", uri.toString());
				return Response.created(uri).build();
			});
	}

	@Override
	public Uni<Response> partiallyUpdateHero(Long id, HeroPatch hero) {
		var entity = this.heroMapper.toEntity(hero);

		if (entity.getId() == null) {
			entity.setId(id);
		}

		return this.heroService.partialUpdateHero(entity)
			.onItem().ifNotNull().transform(h -> {
				Log.debugf("Hero updated with new values %s", h);
				return Response.ok(this.heroMapper.toApiModel(h)).build();
			})
			.replaceIfNullWith(() -> {
				Log.debugf("No hero found with id %d", entity.getId());
				return Response.status(Status.NOT_FOUND).build();
			})
			.onFailure(ConstraintViolationException.class)
			.transform(cve -> new ResteasyReactiveViolationException(((ConstraintViolationException) cve).getConstraintViolations()));
	}

	@Override
	public Uni<Response> deleteAllHeroes() {
		return this.heroService.deleteAllHeroes()
			.invoke(() -> Log.debug("Deleted all heroes"))
			.replaceWith(Response.noContent().build());
	}

	@Override
	public Uni<Response> deleteHero(Long id) {
		return this.heroService.deleteHero(id)
			.invoke(() -> Log.debugf("Hero deleted with %d", id))
			.replaceWith(Response.noContent().build());
	}
}
