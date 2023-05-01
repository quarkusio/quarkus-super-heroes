package io.quarkus.sample.superheroes.fight.client;

import java.util.Optional;
import java.util.function.Predicate;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

/**
 * {@link Predicate} for determining if a {@link Throwable} received from a rest client represents an HTTP {@code 404}.
 */
class Is404Exception implements Predicate<Throwable> {
	static final Is404Exception IS_404 = new Is404Exception();

	private Is404Exception() {

	}

	@Override
	public boolean test(Throwable throwable) {
		return Optional.ofNullable(throwable)
			.filter(t -> t instanceof WebApplicationException)
			.map(WebApplicationException.class::cast)
			.map(WebApplicationException::getResponse)
			.filter(response -> response.getStatus() == Status.NOT_FOUND.getStatusCode())
			.isPresent();
	}
}
