package ee.sk.siddemo.services;

/*-
 * #%L
 * Smart-ID sample Java client
 * %%
 * Copyright (C) 2018 - 2025 SK ID Solutions AS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${sid.v3.client.dynamic-link-url}")
    private String dynamicLinkUrl;

    public final SmartIdClient smartIdClientV3;

    public DynamicContentService(SmartIdClient smartIdClientV3) {
        this.smartIdClientV3 = smartIdClientV3;
    }

    public DynamicContent getDynamicContent(HttpSession session, SessionType sessionType) {
        String sessionSecret = (String) session.getAttribute("sessionSecret");
        String sessionToken = (String) session.getAttribute("sessionToken");
        Instant responseReceivedTime = (Instant) session.getAttribute("responseReceivedTime");

        return getDynamicContent(sessionType, sessionToken, sessionSecret, responseReceivedTime);
    }

    public DynamicContent getDynamicContent(SessionType sessionType, String sessionToken, String sessionSecret, Instant responseReceivedTime) {
        long elapsedSeconds = Duration.between(responseReceivedTime, Instant.now()).getSeconds();
        logger.info("Dynamic content elapsed seconds: {}", elapsedSeconds);

        DynamicContentBuilder contentBuilder = smartIdClientV3.createDynamicContent()
                .withBaseUrl(dynamicLinkUrl)
                .withSessionType(sessionType)
                .withSessionToken(sessionToken)
                .withElapsedSeconds(elapsedSeconds);

        URI dynamicLink = contentBuilder
                .withDynamicLinkType(DynamicLinkType.WEB_2_APP)
                .withAuthCode(AuthCode.createHash(DynamicLinkType.WEB_2_APP, sessionType, elapsedSeconds, sessionSecret))
                .createUri();

        String qrDataUri = contentBuilder
                .withDynamicLinkType(DynamicLinkType.QR_CODE)
                .withAuthCode(AuthCode.createHash(DynamicLinkType.QR_CODE, sessionType, elapsedSeconds, sessionSecret))
                .createQrCodeDataUri();
        return new DynamicContent(dynamicLink, qrDataUri);
    }
}
