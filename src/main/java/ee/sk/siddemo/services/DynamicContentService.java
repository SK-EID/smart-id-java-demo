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
import org.springframework.stereotype.Service;

import ee.sk.siddemo.client.SmartIdMockDeviceLinkClient;
import ee.sk.siddemo.model.DeviceLinkMockRequest;
import ee.sk.siddemo.model.DeviceLinkSessionInfo;
import ee.sk.siddemo.model.UserActionMock;
import ee.sk.smartid.DeviceLinkType;
import ee.sk.smartid.QrCodeGenerator;
import ee.sk.smartid.SessionType;
import ee.sk.smartid.SmartIdClient;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class DynamicContentService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicContentService.class);

    private final SmartIdClient smartIdClient;
    private final SmartIdMockDeviceLinkClient smartIdMockDeviceLinkClient;
    private final SessionStore sessionStore;

    public DynamicContentService(SmartIdClient smartIdClient,
                                 SmartIdMockDeviceLinkClient smartIdMockDeviceLinkClient,
                                 SessionStore sessionStore) {
        this.smartIdClient = smartIdClient;
        this.smartIdMockDeviceLinkClient = smartIdMockDeviceLinkClient;
        this.sessionStore = sessionStore;
    }

    public String getQrCode(HttpSession session, SessionType sessionType) {
        logger.debug("Getting QR-code content for session id: {}, session type: {}", session.getId(), sessionType);
        DeviceLinkSessionInfo deviceLinkSessionInfo = (DeviceLinkSessionInfo) sessionStore.get(session.getId(), "deviceLinkSessionInfo");

        long elapsedSeconds = Duration.between(deviceLinkSessionInfo.getSessionResponseReceived(), Instant.now()).getSeconds();
        DeviceLinkType deviceLinkType = DeviceLinkType.QR_CODE;
        URI qrLink = smartIdClient.createDynamicContent()
                .withSchemeName("smart-id-demo")
                .withDeviceLinkBase(deviceLinkSessionInfo.getDeviceLinkBase())
                .withDeviceLinkType(deviceLinkType)
                .withSessionType(sessionType)
                .withSessionToken(deviceLinkSessionInfo.getSessionToken())
                .withLang("eng")
                .withElapsedSeconds(elapsedSeconds)
                .withInteractions(deviceLinkSessionInfo.getInteractions())
                .withDigest(deviceLinkSessionInfo.getDigest())
                .buildDeviceLink(deviceLinkSessionInfo.getSessionSecret());
        if (deviceLinkSessionInfo.getMockUserAction() == UserActionMock.QR_CODE) {
            DeviceLinkMockRequest request = new DeviceLinkMockRequest(
                    "PNOEE-40404040009-MOCK-Q",
                    qrLink.toString(),
                    DeviceLinkType.QR_CODE.getValue(),
                    null,
                    null);
            smartIdMockDeviceLinkClient.mock(session.getId(), request);
        }
        return QrCodeGenerator.generateDataUri(qrLink.toString());
    }

    public String getDeviceLink(HttpSession session, SessionType sessionType, HttpServletRequest servletRequest) {
        var deviceLinkSessionInfo = (DeviceLinkSessionInfo) sessionStore.get(session.getId(), "deviceLinkSessionInfo");
        URI deviceLink = smartIdClient.createDynamicContent()
                .withSchemeName("smart-id-demo")
                .withDeviceLinkBase(deviceLinkSessionInfo.getDeviceLinkBase())
                .withDeviceLinkType(DeviceLinkType.WEB_2_APP)
                .withSessionType(sessionType)
                .withSessionToken(deviceLinkSessionInfo.getSessionToken())
                .withLang("eng")
                .withInitialCallbackUrl(deviceLinkSessionInfo.getInitialCallbackUrl())
                .withInteractions(deviceLinkSessionInfo.getInteractions())
                .withDigest(deviceLinkSessionInfo.getDigest())
                .buildDeviceLink(deviceLinkSessionInfo.getSessionSecret());
        if (deviceLinkSessionInfo.getMockUserAction().isSameDevice()) {
            Cookie[] cookies = servletRequest.getCookies();
            DeviceLinkMockRequest request = new DeviceLinkMockRequest(
                    "PNOEE-40404040009-MOCK-Q",
                    deviceLink.toString(),
                    getDeviceLinkType(deviceLinkSessionInfo.getMockUserAction()).getValue(),
                    "JSESSIONID=" + (cookies != null ? cookies[0].getValue() : "no-session-id"),
                    deviceLinkSessionInfo.getInitialCallbackUrl()); // callback URL must be accessible outside your localhost
            smartIdMockDeviceLinkClient.mock(session.getId(), request);
        }
        return deviceLink.toString();
    }

    public String getDeviceLink(HttpSession session, SessionType sessionType) {
        logger.debug("Getting device link for session id: {}, session type: {}", session.getId(), sessionType);
        DeviceLinkSessionInfo deviceLinkSessionInfo = (DeviceLinkSessionInfo) sessionStore.get(session.getId(), "deviceLinkSessionInfo");
        URI deviceLink = smartIdClient.createDynamicContent()
                .withSchemeName("smart-id-demo")
                .withDeviceLinkBase(deviceLinkSessionInfo.getDeviceLinkBase())
                .withDeviceLinkType(DeviceLinkType.WEB_2_APP)
                .withSessionType(sessionType)
                .withSessionToken(deviceLinkSessionInfo.getSessionToken())
                .withLang("eng")
                .withInitialCallbackUrl(deviceLinkSessionInfo.getInitialCallbackUrl())
                .withInteractions(deviceLinkSessionInfo.getInteractions())
                .withDigest(deviceLinkSessionInfo.getDigest())
                .buildDeviceLink(deviceLinkSessionInfo.getSessionSecret());

        return deviceLink.toString();
    }

    private DeviceLinkType getDeviceLinkType(UserActionMock mock) {
        return switch (mock) {
            case APP2APP -> DeviceLinkType.APP_2_APP;
            case WEB2APP -> DeviceLinkType.WEB_2_APP;
            default -> throw new IllegalStateException("Unexpected value: " + mock);
        };
    }
}
