package ee.sk.siddemo.services;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ee.sk.siddemo.model.DynamicContent;
import ee.sk.smartid.v3.AuthCode;
import ee.sk.smartid.v3.DynamicContentBuilder;
import ee.sk.smartid.v3.DynamicLinkType;
import ee.sk.smartid.v3.SessionType;
import ee.sk.smartid.v3.SmartIdClient;
import jakarta.servlet.http.HttpSession;

@Service
public class DynamicContentService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicContentService.class);

    public final SmartIdClient smartIdClientV3;

    public DynamicContentService(SmartIdClient smartIdClientV3) {
        this.smartIdClientV3 = smartIdClientV3;
    }

    public DynamicContent getDynamicContent(HttpSession session) {
        String sessionSecret = (String) session.getAttribute("sessionSecret");
        String sessionToken = (String) session.getAttribute("sessionToken");
        Instant responseReceivedTime = (Instant) session.getAttribute("responseReceivedTime");

        return getDynamicContent(sessionToken, sessionSecret, responseReceivedTime);
    }

    public DynamicContent getDynamicContent(String sessionToken, String sessionSecret, Instant responseReceivedTime) {
        long elapsedSeconds = Duration.between(responseReceivedTime, Instant.now()).getSeconds();
        logger.info("Dynamic content elapsed seconds: {}", elapsedSeconds);

        String authCode = AuthCode.createHash(DynamicLinkType.WEB_2_APP, SessionType.AUTHENTICATION, sessionSecret, elapsedSeconds);
        DynamicContentBuilder contentBuilder = smartIdClientV3.createDynamicContent()
                .withSessionType(SessionType.AUTHENTICATION)
                .withSessionToken(sessionToken)
                .withElapsedSeconds(elapsedSeconds)
                .withAuthCode(authCode);
        URI dynamicLink = contentBuilder.withDynamicLinkType(DynamicLinkType.WEB_2_APP).createUri();
        String qrDataUri = contentBuilder.withDynamicLinkType(DynamicLinkType.QR_CODE).createQrCodeDataUri();
        return new DynamicContent(dynamicLink, qrDataUri);
    }
}
