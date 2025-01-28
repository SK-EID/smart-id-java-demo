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

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.smartid.CertificateParser;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.v3.CertificateLevel;
import ee.sk.smartid.v3.ErrorResultHandler;
import ee.sk.smartid.v3.SmartIdClient;
import ee.sk.smartid.v3.rest.dao.NotificationCertificateChoiceSessionResponse;
import ee.sk.smartid.v3.rest.dao.SessionStatus;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Service
public class SmartIdV3NotificationBasedCertificateChoiceService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV3NotificationBasedCertificateChoiceService.class);

    private static final Map<String, String> oidMap = Map.of("2.5.4.5", "serialNumber", "2.5.4.42", "givenName", "2.5.4.4", "surname");

    private final SmartIdClient smartIdClientV3;
    private final SmartIdV3SessionsStatusService smartIdV3SessionsStatusService;

    public SmartIdV3NotificationBasedCertificateChoiceService(SmartIdClient smartIdClientV3,
                                                              SmartIdV3SessionsStatusService smartIdV3SessionsStatusService) {
        this.smartIdClientV3 = smartIdClientV3;
        this.smartIdV3SessionsStatusService = smartIdV3SessionsStatusService;
    }

    public void startCertificateChoice(HttpSession session, @Valid UserRequest userRequest) {
        var semanticsIdentifier = new SemanticsIdentifier(SemanticsIdentifier.IdentityType.PNO, userRequest.getCountry(), userRequest.getNationalIdentityNumber());
        NotificationCertificateChoiceSessionResponse response = smartIdClientV3.createNotificationCertificateChoice()
                .withCertificateLevel(CertificateLevel.QSCD)
                .withSemanticsIdentifier(semanticsIdentifier)
                .initCertificateChoice();

        smartIdV3SessionsStatusService.startPolling(session, response.getSessionID());
    }

    public void startCertificateChoice(HttpSession session, @Valid UserDocumentNumberRequest userDocumentNumberRequest) {
        NotificationCertificateChoiceSessionResponse response = smartIdClientV3.createNotificationCertificateChoice()
                .withCertificateLevel(CertificateLevel.QUALIFIED)
                .withDocumentNumber(userDocumentNumberRequest.getDocumentNumber())
                .initCertificateChoice();

        smartIdV3SessionsStatusService.startPolling(session, response.getSessionID());
    }

    public boolean checkCertificateChoiceStatus(HttpSession session) {
        Optional<SessionStatus> sessionStatus = smartIdV3SessionsStatusService.getSessionsStatus(session.getId());
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

    private static void saveValidateResponse(HttpSession session, SessionStatus status) {
        try {
            if (!"OK".equals(status.getResult().getEndResult())) {
                ErrorResultHandler.handle(status.getResult().getEndResult());
            }
            X509Certificate certificate = CertificateParser.parseX509Certificate(status.getCert().getValue());
            String distinguishedName = certificate.getSubjectX500Principal().getName("RFC1779", oidMap);
            session.setAttribute("distinguishedName", distinguishedName);
        } catch (SessionTimeoutException ex) {
            throw new SidOperationException("Session timed out", ex);
        }
    }
}
