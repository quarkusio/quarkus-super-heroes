package io.quarkus.sample.superheroes.villain.rest;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.villain.api.model.Villain;
import io.quarkus.sample.superheroes.villain.api.model.VillainPatch;
import io.quarkus.sample.superheroes.villain.api.resources.VillainsResource;
import io.quarkus.sample.superheroes.villain.mapping.VillainMapper;
import io.quarkus.sample.superheroes.villain.service.VillainService;

import io.smallrye.common.annotation.RunOnVirtualThread;

public class VillainResource implements VillainsResource {
  @Inject
	VillainService service;

  @Inject
  VillainMapper villainMapper;

  @Context
  UriInfo uriInfo;

  @Override
  @RunOnVirtualThread
	public Response getRandomVillain() {
		return this.service.findRandomVillain()
			.map(v -> {
				Log.debugf("Found random villain: %s", v);
				return Response.ok(this.villainMapper.toApiModel(v)).build();
			})
			.orElseGet(() -> {
				Log.debug("No random villain found");
				return Response.status(Status.NOT_FOUND).build();
			});
	}

  @Override
  @RunOnVirtualThread
	public List<Villain> getAllVillains(String nameFilter) {
    var villains = (nameFilter != null)
      ? this.service.findAllVillainsHavingName(nameFilter)
      : this.service.findAllVillains();

		Log.debugf("Total number of villains: %d", villains.size());

		return this.villainMapper.toApiModelList(villains);
	}

  @Override
  @RunOnVirtualThread
	public Response getVillain(Long id) {
		return this.service.findVillainById(id)
			.map(v -> {
				Log.debugf("Found villain: %s", v);
				return Response.ok(this.villainMapper.toApiModel(v)).build();
			})
			.orElseGet(() -> {
				Log.debugf("No villain found with id %d", id);
				return Response.status(Status.NOT_FOUND).build();
			});
	}

  @Override
  @RunOnVirtualThread
	public Response createVillain(Villain villain) {
		var v = this.service.persistVillain(this.villainMapper.toEntity(villain));
		var uri = this.uriInfo.getAbsolutePathBuilder().path(Long.toString(v.id)).build();
		Log.debugf("New villain created with URI %s", uri.toString());
		return Response.created(uri).build();
	}

  @Override
  @RunOnVirtualThread
	public Response fullyUpdateVillain(Long id, Villain villain) {
    var entity = this.villainMapper.toEntity(villain);

    if (entity.id == null) {
			entity.id = id;
		}

		return this.service.replaceVillain(entity)
			.map(v -> {
				Log.debugf("Villain replaced with new values %s", v);
				return Response.noContent().build();
			})
			.orElseGet(() -> {
				Log.debugf("No villain found with id %d", entity.id);
				return Response.status(Status.NOT_FOUND).build();
			});
	}

  @Override
  @RunOnVirtualThread
  public Response replaceAllVillains(List<Villain> villains) {
    this.service.replaceAllVillains(this.villainMapper.toEntityList(villains));
		var uri = this.uriInfo.getAbsolutePathBuilder().build();
		Log.debugf("New Villains created with URI %s", uri.toString());
		return Response.created(uri).build();
  }

  @Override
  @RunOnVirtualThread
	public Response partiallyUpdateVillain(Long id, VillainPatch villain) {
    var entity = this.villainMapper.toEntity(villain);

		if (entity.id == null) {
			entity.id = id;
		}

		return this.service.partialUpdateVillain(entity)
			.map(v -> {
				Log.debugf("Villain updated with new values %s", v);
				return Response.ok(this.villainMapper.toApiModel(v)).build();
			})
			.orElseGet(() -> {
				Log.debugf("No villain found with id %d", entity.id);
				return Response.status(Status.NOT_FOUND).build();
			});
	}

  @Override
  @RunOnVirtualThread
	public Response deleteAllVillains() {
		this.service.deleteAllVillains();
		Log.debug("Deleted all villains");
		return Response.noContent().build();
	}

  @Override
  @RunOnVirtualThread
	public Response deleteVillain(Long id) {
		this.service.deleteVillain(id);
		Log.debugf("Villain with id %d deleted ", id);
		return Response.noContent().build();
	}

}
