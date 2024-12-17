package ee.sk.siddemo.services;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ee.sk.smartid.exception.permanent.SmartIdClientException;
import ee.sk.smartid.v3.SmartIdClient;
import ee.sk.smartid.v3.rest.dao.DynamicLinkSessionResponse;
import ee.sk.smartid.v3.rest.dao.SessionStatus;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdV3SessionsStatusService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV3SessionsStatusService.class);

    private final Map<String, Future<?>> sessions = new ConcurrentHashMap<>();

    private final SmartIdClient smartIdClientV3;

    public SmartIdV3SessionsStatusService(SmartIdClient smartIdClientV3) {
        this.smartIdClientV3 = smartIdClientV3;
    }

    public void startPolling(HttpSession httpSession, DynamicLinkSessionResponse response) {
        Callable<SessionStatus> task = () -> initPolling(response.getSessionID());
        Future<?> future = Executors.newSingleThreadExecutor().submit(task);
        sessions.put(httpSession.getId(), future);
    }

    public Optional<SessionStatus> getSessionsStatus(String sessionId) {
        Future<?> session = sessions.get(sessionId);
        if (session != null && session.isDone()) {
            try {
                return Optional.of((SessionStatus) session.get());
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        }
        return Optional.empty();
    }

    public void cancelPolling(String sessionId) {
        Future<?> future = sessions.get(sessionId);
        if (future != null) {
            future.cancel(true);
            sessions.remove(sessionId);
        }
    }

    private SessionStatus initPolling(String sessionId) {
        try {
            return smartIdClientV3.getSessionsStatusPoller().fetchFinalSessionStatus(sessionId);
        } catch (SmartIdClientException ex) {
            logger.error("Error occurred while fetching session status", ex);
            throw new RuntimeException(ex);
        }
    }
}
