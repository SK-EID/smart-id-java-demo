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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.DeviceLinkCertificateChoiceSessionInfo;
import ee.sk.smartid.CertificateChoiceResponse;
import ee.sk.smartid.CertificateChoiceResponseValidator;
import ee.sk.smartid.CertificateLevel;
import ee.sk.smartid.SmartIdClient;
import ee.sk.smartid.common.devicelink.CallbackUrl;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.exception.useraction.UserRefusedException;
import ee.sk.smartid.rest.dao.DeviceLinkSessionResponse;
import ee.sk.smartid.rest.dao.SessionStatus;
import ee.sk.smartid.util.CallbackUrlUtil;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdDeviceLinkCertificateChoiceService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdDeviceLinkCertificateChoiceService.class);
    private static final Map<String, String> OID_MAP = Map.of("2.5.4.5", "serialNumber", "2.5.4.42", "givenName", "2.5.4.4", "surname");


    private final SmartIdClient smartIdClient;
    private final SmartIdSessionsStatusService smartIdSessionsStatusService;
    private final CertificateChoiceResponseValidator certificateChoiceResponseValidator;
    private final SessionStore sessionStore;

    @Value("${sid.callbackUrl}")
    private String callbackUrlBase;

    public SmartIdDeviceLinkCertificateChoiceService(SmartIdClient smartIdClient,
                                                     SmartIdSessionsStatusService smartIdSessionsStatusService,
                                                     CertificateChoiceResponseValidator certificateChoiceResponseValidator,
                                                     SessionStore sessionStore) {
        this.smartIdClient = smartIdClient;
        this.smartIdSessionsStatusService = smartIdSessionsStatusService;
        this.certificateChoiceResponseValidator = certificateChoiceResponseValidator;
        this.sessionStore = sessionStore;
    }

    public void startCertificateChoice(HttpSession session) {
        CertificateLevel requesteCertificateLevel = CertificateLevel.QUALIFIED;
        CallbackUrl callbackUrl = CallbackUrlUtil.createCallbackUrl(callbackUrlBase);
        DeviceLinkSessionResponse response = this.smartIdClient.createDeviceLinkCertificateRequest()
                .withCertificateLevel(requesteCertificateLevel)
                .withShareMdClientIpAddress(false)
                .withInitialCallbackUrl(callbackUrl.initialCallbackUri().toString())
                .initCertificateChoice();

        var sessionInfo = new DeviceLinkCertificateChoiceSessionInfo(response, requesteCertificateLevel, callbackUrl);
        sessionStore.put(session.getId(), "deviceLinkSessionInfo", sessionInfo);
        smartIdSessionsStatusService.startPolling(session, response.sessionID());
    }

    public boolean checkCertificateChoiceStatus(HttpSession session) {
        Optional<SessionStatus> sessionStatus = smartIdSessionsStatusService.getSessionsStatus(session.getId());
        return sessionStatus
                .map(ss -> {
                    if (ss.getState().equals("COMPLETE")) {
                        saveValidateResponse(session, ss);
                        logger.debug("Mobile device IP address: {}", ss.getDeviceIpAddress());
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    private void saveValidateResponse(HttpSession session, SessionStatus sessionStatus) {
        try {
            DeviceLinkCertificateChoiceSessionInfo sessionInfo = (DeviceLinkCertificateChoiceSessionInfo) sessionStore.get(session.getId(), "deviceLinkSessionInfo");
            CertificateChoiceResponse certChoiceResponse = certificateChoiceResponseValidator.validate(sessionStatus, sessionInfo.getCertificateLevel());
            sessionInfo.setCertificateChoiceResponse(certChoiceResponse);

            X509Certificate certificate = certChoiceResponse.getCertificate();
            String distinguishedName = certificate.getSubjectX500Principal().getName("RFC1779", OID_MAP);
            session.setAttribute("distinguishedName", distinguishedName);
            session.setAttribute("documentNumber", sessionStatus.getResult().getDocumentNumber());
        } catch (SessionTimeoutException | UserRefusedException ex) {
            throw new SidOperationException(ex.getMessage());
        }
    }
}
