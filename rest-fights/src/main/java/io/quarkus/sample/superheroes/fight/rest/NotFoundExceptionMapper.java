package io.quarkus.sample.superheroes.fight.rest;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
  @Override
  public Response toResponse(NotFoundException e) {
    return (e.getCause() instanceof NumberFormatException) ?
           Response.fromResponse(e.getResponse()).status(Status.BAD_REQUEST).build() :
           e.getResponse();
  }
}
