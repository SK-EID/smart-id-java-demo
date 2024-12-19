package ee.sk.siddemo.services;

/*-
 * #%L
 * Smart-ID sample Java client
 * %%
 * Copyright (C) 2018 - 2024 SK ID Solutions AS
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
import ee.sk.smartid.AuthenticationIdentity;
import ee.sk.smartid.exception.UnprocessableSmartIdResponseException;
import ee.sk.smartid.exception.useraccount.CertificateLevelMismatchException;
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

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV3AuthenticationService.class);

    private final SmartIdClient smartIdClientV3;
    private final SmartIdV3SessionsStatusService smartIdV3SessionsStatusService;
    private final AuthenticationResponseValidator authenticationResponseValidatorV3;

    @Value("${sid.v3.auth.displayText}")
    private String displayText;

    public SmartIdV3AuthenticationService(SmartIdClient smartIdClientV3,
                                          SmartIdV3SessionsStatusService smartIdV3SessionsStatusService,
                                          AuthenticationResponseValidator authenticationResponseValidatorV3) {
        this.smartIdClientV3 = smartIdClientV3;
        this.smartIdV3SessionsStatusService = smartIdV3SessionsStatusService;
        this.authenticationResponseValidatorV3 = authenticationResponseValidatorV3;
    }

    public void startAuthentication(HttpSession session) {
        String randomChallenge = RandomChallenge.generate();
        DynamicLinkSessionResponse response = smartIdClientV3.createDynamicLinkAuthentication()
                .withRandomChallenge(randomChallenge)
                .withAllowedInteractionsOrder(List.of(DynamicLinkInteraction.displayTextAndPIN(displayText)))
                .withShareMdClientIpAddress(true)
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
                .withAllowedInteractionsOrder(List.of(DynamicLinkInteraction.displayTextAndPIN(displayText)))
                .withShareMdClientIpAddress(true)
                .initAuthenticationSession();
        Instant responseReceivedTime = Instant.now();

        updateSession(session, randomChallenge, response, responseReceivedTime);

        smartIdV3SessionsStatusService.startPolling(session, response);
    }

    public void startAuthentication(HttpSession session, UserDocumentNumberRequest userDocumentNumberRequest) {
        String randomChallenge = RandomChallenge.generate();
        DynamicLinkSessionResponse response = smartIdClientV3.createDynamicLinkAuthentication()
                .withRandomChallenge(randomChallenge)
                .withDocumentNumber(userDocumentNumberRequest.getDocumentNumber())
                .withAllowedInteractionsOrder(List.of(DynamicLinkInteraction.displayTextAndPIN(displayText)))
                .withShareMdClientIpAddress(true)
                .initAuthenticationSession();
        Instant responseReceivedTime = Instant.now();

        updateSession(session, randomChallenge, response, responseReceivedTime);

        smartIdV3SessionsStatusService.startPolling(session, response);
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

    private static void saveValidateResponse(HttpSession session, SessionStatus status) {
        try {
            // validate sessions status for dynamic link authentication
            var dynamicLinkAuthenticationResponse = DynamicLinkAuthenticationResponseMapper.from(status);
            session.setAttribute("authentication_response", dynamicLinkAuthenticationResponse);
        } catch (SessionTimeoutException ex) {
            throw new SidOperationException("Session timed out", ex);
        }
    }

    public AuthenticationIdentity authenticate(HttpSession session) {
        // validate sessions status for dynamic link authentication
        DynamicLinkAuthenticationResponse response = (DynamicLinkAuthenticationResponse) session.getAttribute("authentication_response");
        String randomChallenge = (String) session.getAttribute("randomChallenge");

        try {
            // validate and map authentication response to authentication identity
            AuthenticationIdentity authenticationIdentity = authenticationResponseValidatorV3.toAuthenticationIdentity(response, randomChallenge);
            // invalidate current session after successful authentication
            session.invalidate();
            return authenticationIdentity;
        } catch (UnprocessableSmartIdResponseException ex) {
            throw new SidOperationException("Invalid authentication response", ex);
        } catch (CertificateLevelMismatchException ex) {
            throw new SidOperationException("Certificate level mismatch", ex);
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
