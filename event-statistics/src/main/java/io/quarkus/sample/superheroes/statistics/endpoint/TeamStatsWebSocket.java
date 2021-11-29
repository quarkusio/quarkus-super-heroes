package io.quarkus.sample.superheroes.statistics.endpoint;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.microprofile.reactive.messaging.Channel;

import io.quarkus.logging.Log;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;

@ServerEndpoint("/stats/team")
@ApplicationScoped
public class TeamStatsWebSocket {
	@Inject
	@Channel("team-stats")
	Multi<Double> stream;

	private final List<Session> sessions = new CopyOnWriteArrayList<>();
	private Cancellable cancellable;

	@OnOpen
	public void onOpen(Session session) {
		this.sessions.add(session);
	}

	@OnClose
	public void onClose(Session session) {
		this.sessions.remove(session);
	}

	@PostConstruct
	public void subscribe() {
		this.cancellable = this.stream
			.map(ratio -> Double.toString(ratio))
			.subscribe().with(ratio -> this.sessions.forEach(session -> write(session, ratio)));
	}

	@PreDestroy
	public void cleanup() {
		this.cancellable.cancel();
	}

	private void write(Session session, String text) {
		session.getAsyncRemote().sendText(text, result -> {
			if (result.getException() != null) {
				Log.error("Unable to write message to web socket", result.getException());
			}
		});
	}
}
