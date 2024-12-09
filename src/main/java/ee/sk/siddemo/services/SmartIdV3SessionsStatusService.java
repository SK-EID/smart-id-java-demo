package ee.sk.siddemo.services;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import ee.sk.smartid.exception.permanent.SmartIdClientException;
import ee.sk.smartid.v3.DynamicLinkAuthenticationSessionResponse;
import ee.sk.smartid.v3.SmartIdClient;
import ee.sk.smartid.v3.rest.dao.SessionStatus;
import jakarta.servlet.http.HttpSession;

// TODO - 10.12.24: replace this with sessions status querying
@Service
public class SmartIdV3SessionsStatusService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV3SessionsStatusService.class);

    private final SmartIdClient smartIdClientV3;
    private final SessionStatusStore sessionStatusStore;

    public SmartIdV3SessionsStatusService(SmartIdClient smartIdClientV3, SessionStatusStore sessionStatusStore) {
        this.smartIdClientV3 = smartIdClientV3;
        this.sessionStatusStore = sessionStatusStore;
    }

    @Async
    public void startPolling(HttpSession session, DynamicLinkAuthenticationSessionResponse response) {
        CompletableFuture<SessionStatus> sessionStatusFuture = initPolling(response.getSessionID());
        sessionStatusStore.addSession(session.getId(), sessionStatusFuture);
    }

    private CompletableFuture<SessionStatus> initPolling(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Task was interrupted");
                }
                return smartIdClientV3.createSessionStatusPoller().fetchFinalSessionStatus(sessionId);
            } catch (SmartIdClientException ex) {
                logger.error("Error occurred while fetching session status", ex);
                throw new RuntimeException(ex);
            } catch (InterruptedException ex) {
                logger.debug("Polling was interrupted", ex);
                return null;
            }
        });
    }
}
