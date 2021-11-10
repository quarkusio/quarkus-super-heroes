package io.quarkus.workshop.superheroes.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.unchecked.Unchecked;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ServerEndpoint("/stats/winners")
@ApplicationScoped
public class TopWinnerWebSocket {

    @Inject ObjectMapper mapper;

    @Inject Logger logger;

    @Channel("winner-stats")
    Multi<Iterable<Score>> winners;

    private final List<Session> sessions = new CopyOnWriteArrayList<>();
    private Cancellable cancellable;

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
    }

    @PostConstruct
    public void subscribe() {
        cancellable = winners
            .map(Unchecked.function(scores -> mapper.writeValueAsString(scores)))
            .subscribe().with(serialized -> sessions.forEach(session -> write(session, serialized)));
    }

    @PreDestroy
    public void cleanup() {
        cancellable.cancel();
    }

    private void write(Session session, String serialized) {
        session.getAsyncRemote().sendText(serialized, result -> {
            if (result.getException() != null) {
                logger.error("Unable to write message to web socket", result.getException());
            }
        });
    }
}
