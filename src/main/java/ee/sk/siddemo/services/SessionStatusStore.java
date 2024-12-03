package ee.sk.siddemo.services;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import ee.sk.smartid.v3.rest.dao.SessionStatus;

@Service
public class SessionStatusStore {

    private final Map<String, Session> sessionStore = new ConcurrentHashMap<>();

    public void addSession(String sessionId, CompletableFuture<?> future) {
        sessionStore.put(sessionId, new Session(future));
    }

    public Optional<SessionStatus> getSessionsStatus(String sessionId) {
        Session session = sessionStore.get(sessionId);
        if (session == null) {
            return Optional.empty();
        }
        if (session.future.isDone()) {
            try {
                return Optional.of((SessionStatus) session.future.get());
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        }
        return Optional.empty();
    }

    public void removeSession(String sessionId) {
        Session session = sessionStore.remove(sessionId);
        if (session != null && session.future != null) {
            // TODO - 26.11.24: this will not interrupt the task if it is already running, polling ends with timeout
            session.future.cancel(true);
        }
    }

    private record Session(CompletableFuture<?> future) {
    }

}
