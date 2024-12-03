package ee.sk.siddemo.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.smartid.exception.useraccount.DocumentUnusableException;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.v3.DynamicLinkAuthenticationSessionResponse;
import ee.sk.smartid.v3.RandomChallenge;
import ee.sk.smartid.v3.SmartIdClient;
import ee.sk.smartid.v3.rest.dao.Interaction;
import ee.sk.smartid.v3.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.v3.rest.dao.SessionStatus;
import ee.sk.smartid.v3.service.SmartIdRequestBuilderService;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdV3AuthenticationService {

    private final SmartIdClient smartIdClientV3;
    private final SmartIdV3SessionsStatusService smartIdV3SessionsStatusService;
    private final SmartIdRequestBuilderService smartIdRequestBuilderService;
    private final SessionStatusStore sessionStatusStore;

    public SmartIdV3AuthenticationService(SmartIdClient smartIdClientV3,
                                          SmartIdV3SessionsStatusService smartIdV3SessionsStatusService,
                                          SmartIdRequestBuilderService smartIdRequestBuilderService, SessionStatusStore sessionStatusStore) {
        this.smartIdClientV3 = smartIdClientV3;
        this.smartIdV3SessionsStatusService = smartIdV3SessionsStatusService;
        this.smartIdRequestBuilderService = smartIdRequestBuilderService;
        this.sessionStatusStore = sessionStatusStore;
    }

    public void startAuthentication(HttpSession session) {
        String randomChallenge = RandomChallenge.generate();
        DynamicLinkAuthenticationSessionResponse response = smartIdClientV3.createDynamicLinkAuthentication()
                .withRandomChallenge(randomChallenge)
                .withAllowedInteractionsOrder(List.of(Interaction.displayTextAndPIN("Login ? ")))
                .initAuthenticationSession();
        Instant responseReceivedTime = Instant.now();

        updateSession(session, randomChallenge, response, responseReceivedTime);

        smartIdV3SessionsStatusService.startPolling(session, response);
    }

    public void startAuthentication(HttpSession session, UserRequest userRequest) {
        String randomChallenge = RandomChallenge.generate();
        SemanticsIdentifier semanticsIdentifier = new SemanticsIdentifier(SemanticsIdentifier.IdentityType.PNO, userRequest.getCountry(), userRequest.getNationalIdentityNumber());

        DynamicLinkAuthenticationSessionResponse response = smartIdClientV3.createDynamicLinkAuthentication()
                .withRandomChallenge(randomChallenge)
                .withSemanticsIdentifier(semanticsIdentifier)
                .withAllowedInteractionsOrder(
                        List.of(Interaction.displayTextAndPIN("Login ? "))
                ).initAuthenticationSession();
        Instant responseReceivedTime = Instant.now();

        updateSession(session, randomChallenge, response, responseReceivedTime);

        smartIdV3SessionsStatusService.startPolling(session, response);
    }

    public void startAuthentication(HttpSession session, UserDocumentNumberRequest userDocumentNumberRequest) {
        String randomChallenge = RandomChallenge.generate();

        DynamicLinkAuthenticationSessionResponse response = smartIdClientV3.createDynamicLinkAuthentication()
                .withRandomChallenge(randomChallenge)
                .withDocumentNumber(userDocumentNumberRequest.getDocumentNumber())
                .withAllowedInteractionsOrder(
                        List.of(Interaction.displayTextAndPIN("Login ? "))
                ).initAuthenticationSession();
        Instant responseReceivedTime = Instant.now();

        updateSession(session, randomChallenge, response, responseReceivedTime);

        smartIdV3SessionsStatusService.startPolling(session, response);
    }

    public boolean checkAuthenticationStatus(HttpSession session) {
        Optional<SessionStatus> sessionStatus = sessionStatusStore.getSessionsStatus(session.getId());
        return sessionStatus
                .map(status -> {
                    if (status.getState().equals("COMPLETE")) {
                        session.setAttribute("session_status", "COMPLETED");
                        session.setAttribute("session_status_response", status);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    public void authenticate(HttpSession session) {
        SessionStatus sessionsStatusResponse = (SessionStatus) session.getAttribute("session_status_response");
        try {
            // TODO - 26.11.24: create authentication response and invalidate current session
            smartIdRequestBuilderService.createSmartIdAuthenticationResponse(sessionsStatusResponse, "QUALIFIED", null, (String) session.getAttribute("randomChallenge"));
            // TODO - 26.11.24: clear session status storage
        } catch (DocumentUnusableException ex) {
            throw new SidOperationException("Invalid authentication response", ex);
        } catch (SessionTimeoutException ex) {
            throw new SidOperationException("Session timed out", ex);
        }
    }

    private static void updateSession(HttpSession session, String randomChallenge, DynamicLinkAuthenticationSessionResponse response, Instant responseReceivedTime) {
        session.setAttribute("randomChallenge", randomChallenge);
        session.setAttribute("sessionSecret", response.getSessionSecret());
        session.setAttribute("sessionToken", response.getSessionToken());
        session.setAttribute("sessionID", response.getSessionID());
        session.setAttribute("responseReceivedTime", responseReceivedTime);
    }
}
