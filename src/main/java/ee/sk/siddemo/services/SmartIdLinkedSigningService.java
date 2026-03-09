package ee.sk.siddemo.services;

/*-
 * #%L
 * Smart-ID sample Java client
 * %%
 * Copyright (C) 2018 - 2026 SK ID Solutions AS
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

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.digidoc4j.Configuration;
import org.digidoc4j.Container;
import org.digidoc4j.ContainerBuilder;
import org.digidoc4j.DataFile;
import org.digidoc4j.DataToSign;
import org.digidoc4j.DigestAlgorithm;
import org.digidoc4j.SignatureBuilder;
import org.digidoc4j.SignatureProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ee.sk.siddemo.exception.FileUploadException;
import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.LinkedSigningRequest;
import ee.sk.siddemo.model.LinkedSigningSessionInfo;
import ee.sk.smartid.CertificateChoiceResponse;
import ee.sk.smartid.CertificateChoiceResponseValidator;
import ee.sk.smartid.CertificateLevel;
import ee.sk.smartid.SignatureResponse;
import ee.sk.smartid.SignatureResponseValidator;
import ee.sk.smartid.SmartIdClient;
import ee.sk.smartid.common.devicelink.CallbackUrl;
import ee.sk.smartid.common.devicelink.interactions.DeviceLinkInteraction;
import ee.sk.smartid.exception.useraccount.CertificateLevelMismatchException;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.exception.useraction.UserRefusedException;
import ee.sk.smartid.rest.dao.DeviceLinkSessionResponse;
import ee.sk.smartid.rest.dao.LinkedSignatureSessionResponse;
import ee.sk.smartid.rest.dao.SessionStatus;
import ee.sk.smartid.signature.SignableData;
import ee.sk.smartid.signature.SigningSignatureAlgorithm;
import ee.sk.smartid.util.CallbackUrlUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Service
public class SmartIdLinkedSigningService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdLinkedSigningService.class);

    private static final Map<String, String> OID_MAP = Map.of("2.5.4.5", "serialNumber", "2.5.4.42", "givenName", "2.5.4.4", "surname");

    private final SmartIdClient smartIdClient;
    private final SmartIdSessionsStatusService smartIdSessionsStatusService;
    private final CertificateChoiceResponseValidator certificateChoiceResponseValidator;
    private final SignatureResponseValidator signatureResponseValidator;
    private final SessionStore sessionStore;

    @Value("${sid.callbackUrl}")
    private String callbackUrlBase;

    public SmartIdLinkedSigningService(SmartIdClient smartIdClient,
                                       SmartIdSessionsStatusService smartIdSessionsStatusService,
                                       CertificateChoiceResponseValidator certificateChoiceResponseValidator,
                                       SignatureResponseValidator signatureResponseValidator, SessionStore sessionStore) {
        this.smartIdClient = smartIdClient;
        this.smartIdSessionsStatusService = smartIdSessionsStatusService;
        this.certificateChoiceResponseValidator = certificateChoiceResponseValidator;
        this.signatureResponseValidator = signatureResponseValidator;
        this.sessionStore = sessionStore;
    }

    public void startSigning(HttpSession session, @Valid LinkedSigningRequest linkedSigningRequest) {
        SigningSignatureAlgorithm signatureAlgorithm = fromRequestOrDefault(linkedSigningRequest.getSigningSignatureAlgorithm());
        CertificateLevel certificateLevel = CertificateLevel.QUALIFIED;
        CallbackUrl callbackUrl = CallbackUrlUtil.createCallbackUrl(callbackUrlBase);
        DeviceLinkSessionResponse response = this.smartIdClient.createDeviceLinkCertificateRequest()
                .withCertificateLevel(certificateLevel)
                .withInitialCallbackUrl(callbackUrl.initialCallbackUri().toString())
                .initCertificateChoice();

        DataFile uploadedDataFile = getUploadedDataFile(linkedSigningRequest.getFile());
        Container container = toContainer(uploadedDataFile);
        var linkedSigningSessionInfo = new LinkedSigningSessionInfo(
                response,
                certificateLevel,
                uploadedDataFile,
                callbackUrl,
                signatureAlgorithm);
        linkedSigningSessionInfo.setContainer(container);
        sessionStore.put(session.getId(), "deviceLinkSessionInfo", linkedSigningSessionInfo);
        smartIdSessionsStatusService.startPolling(session, response.sessionID());
    }

    public boolean checkCertificateChoiceStatus(HttpSession session) {
        Optional<SessionStatus> sessionStatus = smartIdSessionsStatusService.getSessionsStatus(session.getId());
        return sessionStatus
                .map(ss -> {
                    if (ss.getState().equals("COMPLETE")) {
                        saveValidateCertificateChoiceResponse(session, ss);
                        logger.debug("Mobile device IP address: {}", ss.getDeviceIpAddress());
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    public void continueSigning(HttpSession session) {
        LinkedSigningSessionInfo sessionInfo = (LinkedSigningSessionInfo) sessionStore.get(session.getId(), "deviceLinkSessionInfo");
        CertificateChoiceResponse certChoiceResponse = sessionInfo.getCertificateChoiceResponse();
        SigningSignatureAlgorithm signatureAlgorithm = sessionInfo.getSigningSignatureAlgorithm();
        SignableData signableData = toSignableData(sessionInfo.getUploadedDataFile(), certChoiceResponse.getCertificate(), sessionInfo, signatureAlgorithm);
        LinkedSignatureSessionResponse linkedSignatureSessionResponse = smartIdClient.createLinkedNotificationSignature()
                .withDocumentNumber(certChoiceResponse.getDocumentNumber())
                .withLinkedSessionID(sessionInfo.getCertificateChoiceSessionId())
                .withCertificateLevel(sessionInfo.getCertificateLevel())
                .withSignableData(signableData)
                .withSignatureAlgorithm(signatureAlgorithm)
                .withInteractions(List.of(DeviceLinkInteraction.displayTextAndPin("Sign it!")))
                .initSignatureSession();
        smartIdSessionsStatusService.startPolling(session, linkedSignatureSessionResponse.sessionID());
    }

    public boolean checkSignatureSessionStatus(HttpSession session) {
        Optional<SessionStatus> sessionStatus = smartIdSessionsStatusService.getSessionsStatus(session.getId());
        return sessionStatus
                .map(ss -> {
                    if (ss.getState().equals("COMPLETE")) {
                        saveValidateSignatureResponse(session, ss);
                        logger.debug("Mobile device IP address: {}", ss.getDeviceIpAddress());
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    private void saveValidateCertificateChoiceResponse(HttpSession session, SessionStatus sessionStatus) {
        try {
            LinkedSigningSessionInfo sessionInfo = (LinkedSigningSessionInfo) sessionStore.get(session.getId(), "deviceLinkSessionInfo");
            CertificateChoiceResponse certChoiceResponse = certificateChoiceResponseValidator.validate(sessionStatus, sessionInfo.getCertificateLevel());
            X509Certificate certificate = certChoiceResponse.getCertificate();
            String distinguishedName = certificate.getSubjectX500Principal().getName("RFC1779", OID_MAP);
            session.setAttribute("distinguishedName", distinguishedName);
            sessionInfo.setCertificateChoiceResponse(certChoiceResponse);
        } catch (SessionTimeoutException | UserRefusedException ex) {
            throw new SidOperationException(ex.getMessage());
        }
    }

    private static SignableData toSignableData(DataFile file,
                                              X509Certificate certificate,
                                              LinkedSigningSessionInfo sessionInfo,
                                              SigningSignatureAlgorithm signatureAlgorithm) {
        Container container = sessionInfo.getContainer();
        if (container == null) {
            container = toContainer(file);
            sessionInfo.setContainer(container);
        }
        DataToSign dataToSign = toDataToSign(container, certificate, signatureAlgorithm);
        sessionInfo.setDataToSign(dataToSign);
        byte[] dataToSignBytes = dataToSign.getDataToSign();
        if (signatureAlgorithm.isLegacyRsa()) {
            return new SignableData(dataToSignBytes, signatureAlgorithm.getHashAlgorithmForLegacy());
        }
        return new SignableData(dataToSignBytes);
    }

    private static Container toContainer(DataFile file) {
        var configuration = new Configuration(Configuration.Mode.TEST);
        return ContainerBuilder.aContainer()
                .withConfiguration(configuration)
                .withDataFile(file)
                .build();
    }

    private static DataToSign toDataToSign(Container container,
                                          X509Certificate certificate,
                                          SigningSignatureAlgorithm signatureAlgorithm) {
        return SignatureBuilder.aSignature(container)
                .withSigningCertificate(certificate)
                .withSignatureDigestAlgorithm(toDigestAlgorithm(signatureAlgorithm))
                .withSignatureProfile(SignatureProfile.LT)
                .buildDataToSign();
    }

    private static SigningSignatureAlgorithm fromRequestOrDefault(String signingSignatureAlgorithm) {
        if (signingSignatureAlgorithm == null || signingSignatureAlgorithm.isBlank()) {
            return SigningSignatureAlgorithm.RSASSA_PSS;
        }
        return SigningSignatureAlgorithm.fromString(signingSignatureAlgorithm.trim());
    }

    private static DigestAlgorithm toDigestAlgorithm(SigningSignatureAlgorithm signatureAlgorithm) {
        if (signatureAlgorithm.isLegacyRsa()) {
            return switch (signatureAlgorithm) {
                case SHA256_WITH_RSA_ENCRYPTION -> DigestAlgorithm.SHA256;
                case SHA384_WITH_RSA_ENCRYPTION -> DigestAlgorithm.SHA384;
                case SHA512_WITH_RSA_ENCRYPTION -> DigestAlgorithm.SHA512;
                default -> DigestAlgorithm.SHA512;
            };
        }
        return DigestAlgorithm.SHA512;
    }

    private DataFile getUploadedDataFile(MultipartFile uploadedFile) {
        try {
            return new DataFile(uploadedFile.getInputStream(), uploadedFile.getOriginalFilename(), uploadedFile.getContentType());
        } catch (IOException e) {
            throw new FileUploadException(e.getCause());
        }
    }

    private void saveValidateSignatureResponse(HttpSession session, SessionStatus status) {
        try {
            LinkedSigningSessionInfo sessionInfo = (LinkedSigningSessionInfo) sessionStore.get(session.getId(), "deviceLinkSessionInfo");
            SignatureResponse signatureResponse = signatureResponseValidator.validate(status, sessionInfo.getCertificateLevel());
            sessionInfo.setSignatureResponse(signatureResponse);
        } catch (SessionTimeoutException | UserRefusedException | CertificateLevelMismatchException ex) {
            throw new SidOperationException(ex.getMessage());
        }
    }
}
