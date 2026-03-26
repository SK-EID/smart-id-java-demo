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
import ee.sk.siddemo.model.NotificationAuthenticationSessionInfo;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.smartid.AuthenticationCertificateLevel;
import ee.sk.smartid.NotificationAuthenticationSessionRequestBuilder;
import ee.sk.smartid.RpChallenge;
import ee.sk.smartid.RpChallengeGenerator;
import ee.sk.smartid.SmartIdClient;
import ee.sk.smartid.VerificationCodeCalculator;
import ee.sk.smartid.common.notification.interactions.NotificationInteraction;
import ee.sk.smartid.rest.dao.NotificationAuthenticationSessionResponse;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.rest.dao.SessionStatus;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdNotificationBasedAuthenticationService {

    private final SmartIdClient smartIdClient;
    private final SmartIdSessionsStatusService sessionStatusService;
    private final SessionStore sessionStore;

    @Value("${sid.auth.displayText}")
    private String displayText;

    public SmartIdNotificationBasedAuthenticationService(SmartIdClient smartIdClient,
                                                         SmartIdSessionsStatusService sessionStatusService,
                                                         SessionStore sessionStore) {
        this.smartIdClient = smartIdClient;
        this.sessionStatusService = sessionStatusService;
        this.sessionStore = sessionStore;
    }

    public String startAuthenticationWithPersonCode(HttpSession session, UserRequest userRequest) {
        var semanticsIdentifier = new SemanticsIdentifier(SemanticsIdentifier.IdentityType.PNO, userRequest.getCountry(), userRequest.getNationalIdentityNumber());

        RpChallenge rpChallenge = RpChallengeGenerator.generate();
        String verificationCode = VerificationCodeCalculator.calculate(rpChallenge.value());

        var authenticationCertificateLevel = AuthenticationCertificateLevel.QUALIFIED;
        NotificationAuthenticationSessionRequestBuilder builder = smartIdClient.createNotificationAuthentication()
                .withSemanticsIdentifier(semanticsIdentifier)
                .withRpChallenge(rpChallenge.toBase64EncodedValue())
                .withCertificateLevel(authenticationCertificateLevel)
                .withInteractions(List.of(NotificationInteraction.displayTextAndPin(displayText)));
        NotificationAuthenticationSessionResponse sessionResponse = builder.initAuthenticationSession();


        var notificationAuthenticationSessionInfo = new NotificationAuthenticationSessionInfo(sessionResponse.sessionID(), builder.getAuthenticationSessionRequest());
        sessionStore.put(session.getId(), "notificationAuthenticationSessionInfo", notificationAuthenticationSessionInfo);
        return verificationCode;
    }

    public String startAuthenticationWithDocumentNumber(HttpSession session, UserDocumentNumberRequest userDocumentNumberRequest) {
        RpChallenge rpChallenge = RpChallengeGenerator.generate();
        String verificationCode = VerificationCodeCalculator.calculate(rpChallenge.value());
        var requestedCertificateLevel = AuthenticationCertificateLevel.QUALIFIED;
        NotificationAuthenticationSessionRequestBuilder builder = smartIdClient.createNotificationAuthentication()
                .withDocumentNumber(userDocumentNumberRequest.getDocumentNumber())
                .withRpChallenge(rpChallenge.toBase64EncodedValue())
                .withCertificateLevel(requestedCertificateLevel)
                .withInteractions(List.of(NotificationInteraction.displayTextAndPin(displayText)));
        NotificationAuthenticationSessionResponse sessionResponse = builder.initAuthenticationSession();

        var notificationAuthenticationSessionInfo = new NotificationAuthenticationSessionInfo(sessionResponse.sessionID(), builder.getAuthenticationSessionRequest());
        sessionStore.put(session.getId(), "notificationAuthenticationSessionInfo", notificationAuthenticationSessionInfo);
        return verificationCode;
    }

    public void checkAuthenticationStatus(HttpSession session) {
        var notificationAuthenticationSessionInfo = (NotificationAuthenticationSessionInfo) sessionStore.get(session.getId(), "notificationAuthenticationSessionInfo");
        String sessionId = notificationAuthenticationSessionInfo.getSessionId();
        if (sessionId == null) {
            throw new SidOperationException("Session ID is missing");
        }
        SessionStatus sessionStatus = sessionStatusService.poll(sessionId);
        notificationAuthenticationSessionInfo.setSessionStatus(sessionStatus);
    }
}
