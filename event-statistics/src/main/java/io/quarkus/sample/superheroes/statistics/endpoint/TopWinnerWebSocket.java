package io.quarkus.sample.superheroes.statistics.endpoint;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.microprofile.reactive.messaging.Channel;

import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.statistics.domain.Score;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.unchecked.Unchecked;

@ServerEndpoint("/stats/winners")
@ApplicationScoped
public class TopWinnerWebSocket {
	private final ObjectMapper mapper;
	private final Multi<Iterable<Score>> winners;
	private final List<Session> sessions = new CopyOnWriteArrayList<>();
	private Cancellable cancellable;

	public TopWinnerWebSocket(ObjectMapper mapper, @Channel("winner-stats") Multi<Iterable<Score>> winners) {
		this.mapper = mapper;
		this.winners = winners;
	}

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
		this.cancellable = this.winners
			.map(Unchecked.function(scores -> this.mapper.writeValueAsString(scores)))
			.subscribe().with(serialized -> this.sessions.forEach(session -> write(session, serialized)));
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
