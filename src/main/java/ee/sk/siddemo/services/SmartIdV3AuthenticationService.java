package ee.sk.siddemo.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.smartid.AuthenticationIdentity;
import ee.sk.smartid.exception.useraccount.DocumentUnusableException;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.v3.AuthenticationResponseValidator;
import ee.sk.smartid.v3.DynamicLinkAuthenticationResponse;
import ee.sk.smartid.v3.DynamicLinkAuthenticationResponseMapper;
import ee.sk.smartid.v3.RandomChallenge;
import ee.sk.smartid.v3.SmartIdClient;
import ee.sk.smartid.v3.rest.dao.DynamicLinkInteraction;
import ee.sk.smartid.v3.rest.dao.DynamicLinkSessionResponse;
import ee.sk.smartid.v3.rest.dao.SessionStatus;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdV3AuthenticationService {

    private final SmartIdClient smartIdClientV3;
    private final SmartIdV3SessionsStatusService smartIdV3SessionsStatusService;
    private final SessionStatusStore sessionStatusStore;
    private final AuthenticationResponseValidator authenticationResponseValidatorV3;

    public SmartIdV3AuthenticationService(SmartIdClient smartIdClientV3,
                                          SmartIdV3SessionsStatusService smartIdV3SessionsStatusService,
                                          SessionStatusStore sessionStatusStore,
                                          AuthenticationResponseValidator authenticationResponseValidatorV3) {
        this.smartIdClientV3 = smartIdClientV3;
        this.smartIdV3SessionsStatusService = smartIdV3SessionsStatusService;
        this.sessionStatusStore = sessionStatusStore;
        this.authenticationResponseValidatorV3 = authenticationResponseValidatorV3;
    }

    public void startAuthentication(HttpSession session) {
        String randomChallenge = RandomChallenge.generate();
        DynamicLinkSessionResponse response = smartIdClientV3.createDynamicLinkAuthentication()
                .withRandomChallenge(randomChallenge)
                .withAllowedInteractionsOrder(List.of(DynamicLinkInteraction.displayTextAndPIN("Login ? ")))
                .initAuthenticationSession();
        Instant responseReceivedTime = Instant.now();

        updateSession(session, randomChallenge, response, responseReceivedTime);

        smartIdV3SessionsStatusService.startPolling(session, response);
    }

    public void startAuthentication(HttpSession session, UserRequest userRequest) {
        String randomChallenge = RandomChallenge.generate();
        SemanticsIdentifier semanticsIdentifier = new SemanticsIdentifier(SemanticsIdentifier.IdentityType.PNO, userRequest.getCountry(), userRequest.getNationalIdentityNumber());

        DynamicLinkSessionResponse response = smartIdClientV3.createDynamicLinkAuthentication()
                .withRandomChallenge(randomChallenge)
                .withSemanticsIdentifier(semanticsIdentifier)
                .withAllowedInteractionsOrder(
                        List.of(DynamicLinkInteraction.displayTextAndPIN("Login ? "))
                ).initAuthenticationSession();
        Instant responseReceivedTime = Instant.now();

        updateSession(session, randomChallenge, response, responseReceivedTime);

        smartIdV3SessionsStatusService.startPolling(session, response);
    }

    public void startAuthentication(HttpSession session, UserDocumentNumberRequest userDocumentNumberRequest) {
        String randomChallenge = RandomChallenge.generate();

        DynamicLinkSessionResponse response = smartIdClientV3.createDynamicLinkAuthentication()
                .withRandomChallenge(randomChallenge)
                .withDocumentNumber(userDocumentNumberRequest.getDocumentNumber())
                .withAllowedInteractionsOrder(
                        List.of(DynamicLinkInteraction.displayTextAndPIN("Login ? "))
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

    public AuthenticationIdentity authenticate(HttpSession session) {
        SessionStatus sessionsStatusResponse = (SessionStatus) session.getAttribute("session_status_response");
        String randomChallenge = (String) session.getAttribute("randomChallenge");

        try {
            // validate sessions status for dynamic link authentication
            DynamicLinkAuthenticationResponse dynamicLinkAuthenticationResponse = DynamicLinkAuthenticationResponseMapper.from(sessionsStatusResponse);

            // validate and map authentication response to authentication identity
            AuthenticationIdentity authenticationIdentity = authenticationResponseValidatorV3.toAuthenticationIdentity(dynamicLinkAuthenticationResponse, randomChallenge);
            // invalidate current session after successful authentication
            session.invalidate();
            return authenticationIdentity;
        } catch (DocumentUnusableException ex) {
            throw new SidOperationException("Invalid authentication response", ex);
        } catch (SessionTimeoutException ex) {
            throw new SidOperationException("Session timed out", ex);
        }
    }

    private static void updateSession(HttpSession session, String randomChallenge, DynamicLinkSessionResponse response, Instant responseReceivedTime) {
        session.setAttribute("randomChallenge", randomChallenge);
        session.setAttribute("sessionSecret", response.getSessionSecret());
        session.setAttribute("sessionToken", response.getSessionToken());
        session.setAttribute("sessionID", response.getSessionID());
        session.setAttribute("responseReceivedTime", responseReceivedTime);
    }
}
