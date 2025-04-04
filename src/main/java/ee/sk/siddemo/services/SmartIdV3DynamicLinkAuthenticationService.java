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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.exception.useraction.UserRefusedException;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.v3.AuthenticationCertificateLevel;
import ee.sk.smartid.v3.AuthenticationResponseMapper;
import ee.sk.smartid.v3.RandomChallenge;
import ee.sk.smartid.v3.SmartIdClient;
import ee.sk.smartid.v3.rest.dao.DynamicLinkInteraction;
import ee.sk.smartid.v3.rest.dao.DynamicLinkSessionResponse;
import ee.sk.smartid.v3.rest.dao.SessionStatus;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdV3DynamicLinkAuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV3DynamicLinkAuthenticationService.class);

    private final SmartIdClient smartIdClientV3;
    private final SmartIdV3SessionsStatusService smartIdV3SessionsStatusService;

    @Value("${sid.v3.auth.displayText}")
    private String displayText;

    public SmartIdV3DynamicLinkAuthenticationService(SmartIdClient smartIdClientV3,
                                                     SmartIdV3SessionsStatusService smartIdV3SessionsStatusService) {
        this.smartIdClientV3 = smartIdClientV3;
        this.smartIdV3SessionsStatusService = smartIdV3SessionsStatusService;
    }

    public void startAuthentication(HttpSession session) {
        String randomChallenge = RandomChallenge.generate();
        var authenticationCertificateLevel = AuthenticationCertificateLevel.QUALIFIED;
        DynamicLinkSessionResponse response = smartIdClientV3.createDynamicLinkAuthentication()
                .withRandomChallenge(randomChallenge)
                .withCertificateLevel(authenticationCertificateLevel)
                .withAllowedInteractionsOrder(List.of(DynamicLinkInteraction.displayTextAndPIN(displayText)))
                .withShareMdClientIpAddress(true)
                .initAuthenticationSession();
        Instant responseReceivedTime = Instant.now();

        updateSession(session, randomChallenge, authenticationCertificateLevel, response, responseReceivedTime);

        smartIdV3SessionsStatusService.startPolling(session, response.getSessionID());
    }

    public void startAuthentication(HttpSession session, UserRequest userRequest) {
        String randomChallenge = RandomChallenge.generate();
        var semanticsIdentifier = new SemanticsIdentifier(SemanticsIdentifier.IdentityType.PNO, userRequest.getCountry(), userRequest.getNationalIdentityNumber());
        var requestedCertificateLevel = AuthenticationCertificateLevel.QUALIFIED;
        DynamicLinkSessionResponse response = smartIdClientV3.createDynamicLinkAuthentication()
                .withRandomChallenge(randomChallenge)
                .withSemanticsIdentifier(semanticsIdentifier)
                .withCertificateLevel(requestedCertificateLevel)
                .withAllowedInteractionsOrder(List.of(DynamicLinkInteraction.displayTextAndPIN(displayText)))
                .withShareMdClientIpAddress(true)
                .initAuthenticationSession();
        Instant responseReceivedTime = Instant.now();

        updateSession(session, randomChallenge, requestedCertificateLevel, response, responseReceivedTime);

        smartIdV3SessionsStatusService.startPolling(session, response.getSessionID());
    }

    public void startAuthentication(HttpSession session, UserDocumentNumberRequest userDocumentNumberRequest) {
        String randomChallenge = RandomChallenge.generate();
        var requestedCertificateLevel = AuthenticationCertificateLevel.QUALIFIED;
        DynamicLinkSessionResponse response = smartIdClientV3.createDynamicLinkAuthentication()
                .withRandomChallenge(randomChallenge)
                .withDocumentNumber(userDocumentNumberRequest.getDocumentNumber())
                .withCertificateLevel(requestedCertificateLevel)
                .withAllowedInteractionsOrder(List.of(DynamicLinkInteraction.displayTextAndPIN(displayText)))
                .withShareMdClientIpAddress(true)
                .initAuthenticationSession();
        Instant responseReceivedTime = Instant.now();

        updateSession(session, randomChallenge, requestedCertificateLevel, response, responseReceivedTime);

        smartIdV3SessionsStatusService.startPolling(session, response.getSessionID());
    }

    public boolean checkAuthenticationStatus(HttpSession session) {
        Optional<SessionStatus> sessionStatus = smartIdV3SessionsStatusService.getSessionsStatus(session.getId());
        return sessionStatus
                .map(status -> {
                    if (status.getState().equals("COMPLETE")) {
                        saveValidateResponse(session, status);
                        session.setAttribute("session_status", "COMPLETED");
                        logger.debug("Mobile device IP address: {}", status.getDeviceIpAddress());
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    private static void updateSession(HttpSession session,
                                      String randomChallenge,
                                      AuthenticationCertificateLevel certificateLevel,
                                      DynamicLinkSessionResponse response,
                                      Instant responseReceivedTime) {
        session.setAttribute("randomChallenge", randomChallenge);
        session.setAttribute("requestedCertificateLevel", certificateLevel);
        session.setAttribute("sessionSecret", response.getSessionSecret());
        session.setAttribute("sessionToken", response.getSessionToken());
        session.setAttribute("sessionID", response.getSessionID());
        session.setAttribute("responseReceivedTime", responseReceivedTime);
    }

    private static void saveValidateResponse(HttpSession session, SessionStatus status) {
        try {
            // validate sessions status for dynamic link authentication
            var dynamicLinkAuthenticationResponse = AuthenticationResponseMapper.from(status);
            session.setAttribute("authentication_response", dynamicLinkAuthenticationResponse);
        } catch (SessionTimeoutException | UserRefusedException ex) {
            throw new SidOperationException(ex.getMessage());
        }
    }
}
