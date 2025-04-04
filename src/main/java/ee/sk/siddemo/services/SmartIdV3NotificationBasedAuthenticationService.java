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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.exception.useraction.UserRefusedException;
import ee.sk.smartid.exception.useraction.UserSelectedWrongVerificationCodeException;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.v3.AuthenticationCertificateLevel;
import ee.sk.smartid.v3.AuthenticationResponseMapper;
import ee.sk.smartid.v3.RandomChallenge;
import ee.sk.smartid.v3.SmartIdClient;
import ee.sk.smartid.v3.rest.dao.NotificationAuthenticationSessionResponse;
import ee.sk.smartid.v3.rest.dao.NotificationInteraction;
import ee.sk.smartid.v3.rest.dao.SessionStatus;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdV3NotificationBasedAuthenticationService {

    private final SmartIdClient smartIdClientV3;
    private final SmartIdV3SessionsStatusService sessionStatusService;

    @Value("${sid.v3.auth.displayText}")
    private String displayText;

    public SmartIdV3NotificationBasedAuthenticationService(SmartIdClient smartIdClientV3, SmartIdV3SessionsStatusService sessionStatusService) {
        this.smartIdClientV3 = smartIdClientV3;
        this.sessionStatusService = sessionStatusService;
    }

    public String startAuthenticationWithPersonCode(HttpSession session, UserRequest userRequest) {
        var semanticsIdentifier = new SemanticsIdentifier(SemanticsIdentifier.IdentityType.PNO, userRequest.getCountry(), userRequest.getNationalIdentityNumber());

        String randomChallenge = RandomChallenge.generate();
        var authenticationCertificateLevel = AuthenticationCertificateLevel.QUALIFIED;
        NotificationAuthenticationSessionResponse sessionResponse = smartIdClientV3.createNotificationAuthentication()
                .withSemanticsIdentifier(semanticsIdentifier)
                .withRandomChallenge(randomChallenge)
                .withCertificateLevel(authenticationCertificateLevel)
                .withAllowedInteractionsOrder(List.of(NotificationInteraction.verificationCodeChoice(displayText)))
                .initAuthenticationSession();

        session.setAttribute("sessionID", sessionResponse.getSessionID());
        session.setAttribute("randomChallenge", randomChallenge);
        session.setAttribute("requestedCertificateLevel", authenticationCertificateLevel);
        return sessionResponse.getVc().getValue();
    }

    public String startAuthenticationWithDocumentNumber(HttpSession session, UserDocumentNumberRequest userDocumentNumberRequest) {
        String randomChallenge = RandomChallenge.generate();
        var requestedCertificateLevel = AuthenticationCertificateLevel.QUALIFIED;
        NotificationAuthenticationSessionResponse sessionResponse = smartIdClientV3.createNotificationAuthentication()
                .withDocumentNumber(userDocumentNumberRequest.getDocumentNumber())
                .withRandomChallenge(randomChallenge)
                .withCertificateLevel(requestedCertificateLevel)
                .withAllowedInteractionsOrder(List.of(NotificationInteraction.verificationCodeChoice(displayText)))
                .initAuthenticationSession();

        session.setAttribute("sessionID", sessionResponse.getSessionID());
        session.setAttribute("randomChallenge", randomChallenge);
        session.setAttribute("requestedCertificateLevel", requestedCertificateLevel);
        return sessionResponse.getVc().getValue();
    }

    public void checkAuthenticationStatus(HttpSession session) {
        String sessionId = (String) session.getAttribute("sessionID");
        if (sessionId == null) {
            throw new SidOperationException("Session ID is missing");
        }
        SessionStatus sessionStatus = sessionStatusService.poll(sessionId);
        saveValidateResponse(session, sessionStatus);
    }

    private static void saveValidateResponse(HttpSession session, SessionStatus status) {
        try {
            var signatureResponse = AuthenticationResponseMapper.from(status);
            session.setAttribute("authentication_response", signatureResponse);
        } catch (SessionTimeoutException | UserRefusedException | UserSelectedWrongVerificationCodeException ex) {
            throw new SidOperationException(ex.getMessage());
        }
    }
}
