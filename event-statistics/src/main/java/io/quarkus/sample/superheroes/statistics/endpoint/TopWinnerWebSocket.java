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

import io.quarkus.logging.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.unchecked.Unchecked;

@ServerEndpoint("/stats/winners")
@ApplicationScoped
public class TopWinnerWebSocket {
	private final ObjectMapper mapper;
	private final WinnerStatsChannelHolder winnerStatsChannelHolder;
	private final List<Session> sessions = new CopyOnWriteArrayList<>();
	private Cancellable cancellable;

	public TopWinnerWebSocket(ObjectMapper mapper, WinnerStatsChannelHolder winnerStatsChannelHolder) {
		this.mapper = mapper;
		this.winnerStatsChannelHolder = winnerStatsChannelHolder;
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
		this.cancellable = this.winnerStatsChannelHolder.getWinners()
			.map(Unchecked.function(this.mapper::writeValueAsString))
			.subscribe().with(serialized -> this.sessions.forEach(session -> write(session, serialized)));
	}

	@PreDestroy
	public void cleanup() {
		this.cancellable.cancel();
	}

	private void write(Session session, String text) {
		Log.infof("Writing message %s", text);

		session.getAsyncRemote().sendText(text, result -> {
			if (result.getException() != null) {
				Log.error("Unable to write message to web socket", result.getException());
			}
		});
	}
}
