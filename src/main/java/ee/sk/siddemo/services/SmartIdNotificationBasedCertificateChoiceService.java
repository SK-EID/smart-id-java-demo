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

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.smartid.CertificateChoiceResponse;
import ee.sk.smartid.CertificateChoiceResponseValidator;
import ee.sk.smartid.CertificateLevel;
import ee.sk.smartid.SmartIdClient;
import ee.sk.smartid.exception.permanent.SmartIdClientException;
import ee.sk.smartid.exception.useraccount.CertificateLevelMismatchException;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.exception.useraction.UserRefusedException;
import ee.sk.smartid.exception.useraction.UserSelectedWrongVerificationCodeException;
import ee.sk.smartid.rest.dao.NotificationCertificateChoiceSessionResponse;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.rest.dao.SessionStatus;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdNotificationBasedCertificateChoiceService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdNotificationBasedCertificateChoiceService.class);

    private static final Map<String, String> OID_MAP = Map.of("2.5.4.5", "serialNumber", "2.5.4.42", "givenName", "2.5.4.4", "surname");

    private final SmartIdClient smartIdClient;
    private final SmartIdSessionsStatusService smartIdSessionsStatusService;
    private final CertificateChoiceResponseValidator certificateChoiceResponseValidator;

    public SmartIdNotificationBasedCertificateChoiceService(SmartIdClient smartIdClient,
                                                            SmartIdSessionsStatusService smartIdSessionsStatusService, CertificateChoiceResponseValidator certificateChoiceResponseValidator) {
        this.smartIdClient = smartIdClient;
        this.smartIdSessionsStatusService = smartIdSessionsStatusService;
        this.certificateChoiceResponseValidator = certificateChoiceResponseValidator;
    }

    public void startCertificateChoice(HttpSession session, UserRequest userRequest) {
        CertificateLevel certificateLevel = CertificateLevel.QSCD;
        var semanticsIdentifier = new SemanticsIdentifier(SemanticsIdentifier.IdentityType.PNO, userRequest.getCountry(), userRequest.getNationalIdentityNumber());
        NotificationCertificateChoiceSessionResponse response = smartIdClient.createNotificationCertificateChoice()
                .withCertificateLevel(certificateLevel)
                .withSemanticsIdentifier(semanticsIdentifier)
                .initCertificateChoice();

        session.setAttribute("certificateChoiceCertificateLevel", certificateLevel);
        smartIdSessionsStatusService.startPolling(session, response.sessionID());
    }

    public boolean checkCertificateChoiceStatus(HttpSession session) {
        Optional<SessionStatus> sessionStatus = smartIdSessionsStatusService.getSessionsStatus(session.getId());
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

    private CertificateChoiceResponse getCertificateChoice(HttpSession session, SessionStatus status) {
        try {
            CertificateLevel requestedCertificateLevel = (CertificateLevel) session.getAttribute("certificateChoiceCertificateLevel");
            return certificateChoiceResponseValidator.validate(status, requestedCertificateLevel);
        } catch (SessionTimeoutException | UserRefusedException | UserSelectedWrongVerificationCodeException | CertificateLevelMismatchException ex) {
            throw new SidOperationException(ex.getMessage());
        } catch (SmartIdClientException ex) {
            throw new SidOperationException("Error occurred while parsing certificate", ex);
        }
    }

    private void saveValidateResponse(HttpSession session, SessionStatus status) {
        CertificateChoiceResponse certificateChoiceResponse = getCertificateChoice(session, status);
        String distinguishedName = certificateChoiceResponse.getCertificate().getSubjectX500Principal().getName("RFC1779", OID_MAP);
        session.setAttribute("distinguishedName", distinguishedName);
    }
}
