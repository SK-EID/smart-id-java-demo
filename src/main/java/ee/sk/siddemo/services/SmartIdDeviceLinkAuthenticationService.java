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

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ee.sk.siddemo.model.AnonymousRequest;
import ee.sk.siddemo.model.DeviceLinkAuthenticationDeviceLinkSessionInfo;
import ee.sk.siddemo.model.UserActionMock;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.smartid.AuthenticationCertificateLevel;
import ee.sk.smartid.DeviceLinkAuthenticationSessionRequestBuilder;
import ee.sk.smartid.RpChallengeGenerator;
import ee.sk.smartid.SmartIdClient;
import ee.sk.smartid.common.devicelink.CallbackUrl;
import ee.sk.smartid.common.devicelink.interactions.DeviceLinkInteraction;
import ee.sk.smartid.rest.dao.DeviceLinkAuthenticationSessionRequest;
import ee.sk.smartid.rest.dao.DeviceLinkSessionResponse;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.rest.dao.SessionStatus;
import ee.sk.smartid.signature.AuthenticationSignatureAlgorithm;
import ee.sk.smartid.signature.HashAlgorithm;
import ee.sk.smartid.util.CallbackUrlUtil;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdDeviceLinkAuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdDeviceLinkAuthenticationService.class);

    private final SmartIdClient smartIdClient;
    private final SmartIdSessionsStatusService smartIdSessionsStatusService;
    private final SessionStore sessionStore;

    @Value("${sid.auth.displayText}")
    private String displayText;
    @Value("${sid.callbackUrl}")
    private String callbackUrlBase;

    public SmartIdDeviceLinkAuthenticationService(SmartIdClient smartIdClient,
                                                  SmartIdSessionsStatusService smartIdSessionsStatusService,
                                                  SessionStore sessionStore) {
        this.smartIdClient = smartIdClient;
        this.smartIdSessionsStatusService = smartIdSessionsStatusService;
        this.sessionStore = sessionStore;
    }

    public void startAuthentication(HttpSession session, AnonymousRequest anonymousRequest) {
        String rpChallenge = RpChallengeGenerator.generate().toBase64EncodedValue();
        var authenticationCertificateLevel = AuthenticationCertificateLevel.ADVANCED;
        CallbackUrl callbackUrl = CallbackUrlUtil.createCallbackUrl(callbackUrlBase);
        DeviceLinkAuthenticationSessionRequestBuilder builder = smartIdClient.createDeviceLinkAuthentication()
                .withRpChallenge(rpChallenge)
                .withCertificateLevel(authenticationCertificateLevel)
                .withSignatureAlgorithm(AuthenticationSignatureAlgorithm.RSASSA_PSS)
                .withHashAlgorithm(HashAlgorithm.SHA3_512)
                .withInteractions(List.of(DeviceLinkInteraction.displayTextAndPin(displayText)))
                .withInitialCallbackUrl(callbackUrl.initialCallbackUri().toString())
                .withShareMdClientIpAddress(true);
        DeviceLinkSessionResponse response = builder.initAuthenticationSession();
        DeviceLinkAuthenticationSessionRequest request = builder.getAuthenticationSessionRequest();

        var deviceLinkAuthenticationDeviceLinkSessionInfo = new DeviceLinkAuthenticationDeviceLinkSessionInfo(response, request, callbackUrl, anonymousRequest.getUserActionMock());
        sessionStore.put(session.getId(), "deviceLinkSessionInfo", deviceLinkAuthenticationDeviceLinkSessionInfo);
        smartIdSessionsStatusService.startPolling(session, response.sessionID());
    }

    public void startAuthentication(HttpSession session, UserRequest userRequest) {
        String rpChallenge = RpChallengeGenerator.generate().toBase64EncodedValue();
        var semanticsIdentifier = new SemanticsIdentifier(SemanticsIdentifier.IdentityType.PNO, userRequest.getCountry(), userRequest.getNationalIdentityNumber());
        var requestedCertificateLevel = AuthenticationCertificateLevel.QUALIFIED;
        List<DeviceLinkInteraction> interactions = List.of(DeviceLinkInteraction.displayTextAndPin(displayText));
        CallbackUrl callbackUrl = CallbackUrlUtil.createCallbackUrl(callbackUrlBase);
        DeviceLinkAuthenticationSessionRequestBuilder builder = smartIdClient.createDeviceLinkAuthentication()
                .withRpChallenge(rpChallenge)
                .withSemanticsIdentifier(semanticsIdentifier)
                .withCertificateLevel(requestedCertificateLevel)
                .withSignatureAlgorithm(AuthenticationSignatureAlgorithm.RSASSA_PSS)
                .withHashAlgorithm(HashAlgorithm.SHA3_512)
                .withShareMdClientIpAddress(true)
                .withInteractions(interactions);
        DeviceLinkSessionResponse response = builder.initAuthenticationSession();
        DeviceLinkAuthenticationSessionRequest request = builder.getAuthenticationSessionRequest();

        var deviceLinkAuthenticationDeviceLinkSessionInfo = new DeviceLinkAuthenticationDeviceLinkSessionInfo(response, request, callbackUrl, UserActionMock.NONE);
        sessionStore.put(session.getId(), "deviceLinkSessionInfo", deviceLinkAuthenticationDeviceLinkSessionInfo);
        smartIdSessionsStatusService.startPolling(session, response.sessionID());
    }

    public void startAuthentication(HttpSession session, UserDocumentNumberRequest userDocumentNumberRequest) {
        String rpChallenge = RpChallengeGenerator.generate().toBase64EncodedValue();
        var requestedCertificateLevel = AuthenticationCertificateLevel.QUALIFIED;
        List<DeviceLinkInteraction> interactions = List.of(DeviceLinkInteraction.displayTextAndPin(displayText));
        CallbackUrl callbackUrl = CallbackUrlUtil.createCallbackUrl(callbackUrlBase);
        DeviceLinkAuthenticationSessionRequestBuilder builder = smartIdClient.createDeviceLinkAuthentication()
                .withRpChallenge(rpChallenge)
                .withDocumentNumber(userDocumentNumberRequest.getDocumentNumber())
                .withCertificateLevel(requestedCertificateLevel)
                .withSignatureAlgorithm(AuthenticationSignatureAlgorithm.RSASSA_PSS)
                .withHashAlgorithm(HashAlgorithm.SHA3_512)
                .withShareMdClientIpAddress(true)
                .withInteractions(interactions);
        DeviceLinkSessionResponse response = builder.initAuthenticationSession();
        DeviceLinkAuthenticationSessionRequest request = builder.getAuthenticationSessionRequest();

        var deviceLinkAuthenticationDeviceLinkSessionInfo = new DeviceLinkAuthenticationDeviceLinkSessionInfo(response, request, callbackUrl, UserActionMock.NONE);
        sessionStore.put(session.getId(), "deviceLinkSessionInfo", deviceLinkAuthenticationDeviceLinkSessionInfo);
        smartIdSessionsStatusService.startPolling(session, response.sessionID());
    }

    public boolean checkAuthenticationStatus(HttpSession session) {
        Optional<SessionStatus> sessionStatus = smartIdSessionsStatusService.getSessionsStatus(session.getId());
        return sessionStatus
                .map(status -> {
                    if (status.getState().equals("COMPLETE")) {
                        DeviceLinkAuthenticationDeviceLinkSessionInfo sessionInfo = (DeviceLinkAuthenticationDeviceLinkSessionInfo) sessionStore.get(session.getId(), "deviceLinkSessionInfo");
                        if (sessionInfo.getSessionStatus() == null) {
                            sessionInfo.setSessionStatus(status);
                            logger.debug("Mobile device IP address: {}", status.getDeviceIpAddress());
                        }
                        // TODO - 07.10.25: this should be reworked. Provide only one flow for use QR-code or device-link.
                        if (!isCallbackCompleted(sessionInfo)) {
                            logger.debug("Callback not yet received.");
                            return false;
                        }
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    private static boolean isCallbackCompleted(DeviceLinkAuthenticationDeviceLinkSessionInfo sessionInfo) {
        if (sessionInfo.getSessionStatus().getSignature() == null) {
            logger.debug("Signature completed with error");
            return true;
        }
        if (sessionInfo.getSessionStatus().getSignature().getFlowType().equals("QR")) {
            logger.debug("QR flow, callback completed and userChallengeVerifier should not be checked");
            return true;
        }
        return sessionInfo.getUserChallengeVerifier() != null;
    }
}
