package ee.sk.siddemo.services;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.apache.hc.client5.http.utils.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.v3.ErrorResultHandler;
import ee.sk.smartid.v3.SmartIdClient;
import ee.sk.smartid.v3.rest.dao.DynamicLinkSessionResponse;
import ee.sk.smartid.v3.rest.dao.SessionResult;
import ee.sk.smartid.v3.rest.dao.SessionStatus;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdV3CertificateChoiceService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV3CertificateChoiceService.class);

    private final SmartIdClient smartIdClientV3;
    private final SmartIdV3SessionsStatusService smartIdV3SessionsStatusService;

    public SmartIdV3CertificateChoiceService(SmartIdClient smartIdClientV3,
                                             SmartIdV3SessionsStatusService smartIdV3SessionsStatusService) {
        this.smartIdClientV3 = smartIdClientV3;
        this.smartIdV3SessionsStatusService = smartIdV3SessionsStatusService;
    }

    public void startCertificateChoice(HttpSession session) {
        // TODO - 16.12.24: uncomment when anonymous certificate choice is supported by RP API v3
//        DynamicLinkSessionResponse response = this.smartIdClientV3.createDynamicLinkCertificateRequest()
//                .withCertificateLevel(CertificateLevel.QUALIFIED)
//                .withShareMdClientIpAddress(true)
//                .initCertificateChoice();
        DynamicLinkSessionResponse response = new DynamicLinkSessionResponse();
        response.setSessionID("fake-session-id");
        response.setSessionToken("fake-token");
        response.setSessionSecret(Base64.encodeBase64String("fake-secret".getBytes(StandardCharsets.UTF_8)));

        Instant responseReceivedTime = Instant.now();

        session.setAttribute("sessionID", response.getSessionID());
        session.setAttribute("sessionToken", response.getSessionToken());
        session.setAttribute("sessionSecret", response.getSessionSecret());
        session.setAttribute("responseReceivedTime", responseReceivedTime);

        // TODO - 16.12.24: uncomment when anonymous certificate choice session can be created
//        smartIdV3SessionsStatusService.startPolling(session, response);
    }

    public boolean checkCertificateChoiceStatus(HttpSession session) {
        // TODO - 16.12.24: uncomment when anonymous certificate choice is supported by RP API v3
        // Optional<SessionStatus> sessionStatus = smartIdV3SessionsStatusService.getSessionsStatus(session.getId());

        // Dummy session status to simulate querying the session status, displaying dynamic sessions for 10seconds
        // Remove after RP API v3 supports dynamic-link certificate choice
        Instant responseReceivedTime = (Instant) session.getAttribute("responseReceivedTime");
        long elapsedSeconds = Duration.between(responseReceivedTime, Instant.now()).getSeconds();
        SessionStatus status = dummySessionsStatus(elapsedSeconds);
        Optional<SessionStatus> sessionStatus = Optional.of(status);

        return sessionStatus
                .map(ss -> {
                    if (ss.getState().equals("COMPLETE")) {
                        saveValidateResponse(session, ss);
                        session.setAttribute("session_status", "COMPLETED");
                        logger.debug("Mobile device IP address: {}", ss.getDeviceIpAddress());
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    // TODO - 17.12.24: only used for simulating querying the session status, remove after actuall functionality can be used
    private static SessionStatus dummySessionsStatus(long elapsedSeconds) {
        SessionStatus status;
        if (elapsedSeconds > 10) {
            SessionResult result = new SessionResult();
            result.setEndResult("OK");
            result.setDocumentNumber("PNOEE-1234567890-MOCK-Q");

            status = new SessionStatus();
            status.setState("COMPLETE");
            status.setResult(result);
        } else {
            status = new SessionStatus();
            status.setState("RUNNING");
        }
        return status;
    }

    private static void saveValidateResponse(HttpSession session, SessionStatus status) {
        try {
            // validate sessions status does not contain errors
            if (!"OK".equals(status.getResult().getEndResult())) {
                ErrorResultHandler.handle(status.getResult().getEndResult());
            }
            // TODO - 17.12.24: uncomment when anonymous certificate choice is supported by RP API v3
//            X509Certificate certificate = CertificateParser.parseX509Certificate(status.getCert().getValue());
//            String distinguishedName = certificate.getSubjectX500Principal().getName();
            String distinguishedName = "CN=John Doe, OU=IT, O=Company, C=EE";
            session.setAttribute("distinguishedName", distinguishedName);
            session.setAttribute("documentNumber", status.getResult().getDocumentNumber());
        } catch (SessionTimeoutException ex) {
            throw new SidOperationException("Session timed out", ex);
        }
    }
}
