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

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.digidoc4j.Configuration;
import org.digidoc4j.Container;
import org.digidoc4j.ContainerBuilder;
import org.digidoc4j.DataFile;
import org.digidoc4j.DataToSign;
import org.digidoc4j.DigestAlgorithm;
import org.digidoc4j.SignatureBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ee.sk.siddemo.exception.FileUploadException;
import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.NotificationSignatureSessionInfo;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.smartid.CertificateByDocumentNumberResult;
import ee.sk.smartid.CertificateChoiceResponse;
import ee.sk.smartid.CertificateChoiceResponseValidator;
import ee.sk.smartid.CertificateLevel;
import ee.sk.smartid.SignatureResponseValidator;
import ee.sk.smartid.SmartIdClient;
import ee.sk.smartid.common.notification.interactions.NotificationInteraction;
import ee.sk.smartid.exception.useraccount.CertificateLevelMismatchException;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.exception.useraction.UserRefusedException;
import ee.sk.smartid.exception.useraction.UserSelectedWrongVerificationCodeException;
import ee.sk.smartid.rest.dao.NotificationCertificateChoiceSessionResponse;
import ee.sk.smartid.rest.dao.NotificationSignatureSessionResponse;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.rest.dao.SessionStatus;
import ee.sk.smartid.signature.HashAlgorithm;
import ee.sk.smartid.signature.SignableData;
import ee.sk.smartid.signature.SigningSignatureAlgorithm;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdNotificationBasedSigningService {

    private final SmartIdClient smartIdClient;
    private final SmartIdSessionsStatusService sessionStatusService;
    private final SignatureResponseValidator signatureResponseValidator;
    private final SessionStore sessionStore;
    private final CertificateChoiceResponseValidator certificateChoiceResponseValidator;

    public SmartIdNotificationBasedSigningService(SmartIdClient smartIdClient,
                                                  SmartIdSessionsStatusService sessionStatusService,
                                                  SignatureResponseValidator signatureResponseValidator,
                                                  SessionStore sessionStore, CertificateChoiceResponseValidator certificateChoiceResponseValidator) {
        this.smartIdClient = smartIdClient;
        this.sessionStatusService = sessionStatusService;
        this.signatureResponseValidator = signatureResponseValidator;
        this.sessionStore = sessionStore;
        this.certificateChoiceResponseValidator = certificateChoiceResponseValidator;
    }

    public String startSigningWithDocumentNumber(HttpSession session, UserDocumentNumberRequest userDocumentNumberRequest) {
        SigningSignatureAlgorithm signatureAlgorithm = fromRequestOrDefault(userDocumentNumberRequest.getSigningSignatureAlgorithm());
        var signatureCertificateLevel = CertificateLevel.QSCD;
        CertificateByDocumentNumberResult certificateResult = smartIdClient
                .createCertificateByDocumentNumber()
                .withDocumentNumber(userDocumentNumberRequest.getDocumentNumber())
                .withCertificateLevel(signatureCertificateLevel)
                .getCertificateByDocumentNumber();
        var sessionInfoBuilder = NotificationSignatureSessionInfo.builder()
                .withSignatureCertificateLevel(signatureCertificateLevel)
                .withCertificateResult(certificateResult);

        SignableData signableData = toSignableData(userDocumentNumberRequest.getFile(), certificateResult.certificate(), sessionInfoBuilder, signatureAlgorithm);
        NotificationSignatureSessionResponse sessionResponse = smartIdClient.createNotificationSignature()
                .withCertificateLevel(signatureCertificateLevel)
                .withSignableData(signableData)
                .withSignatureAlgorithm(signatureAlgorithm)
                .withDocumentNumber(userDocumentNumberRequest.getDocumentNumber())
                .withInteractions(List.of(NotificationInteraction.displayTextAndPin("Sign the document!")))
                .initSignatureSession();

        var sessionInfo = sessionInfoBuilder.withSessionResponse(sessionResponse).build();
        sessionStore.put(session.getId(), "notificationSignatureSessionInfo", sessionInfo);
        return sessionResponse.vc().value();
    }

    public String startSigningWithPersonCode(HttpSession session, UserRequest userRequest) {
        SigningSignatureAlgorithm signatureAlgorithm = fromRequestOrDefault(userRequest.getSigningSignatureAlgorithm());
        var signatureCertificateLevel = CertificateLevel.QUALIFIED;
        var sessionInfoBuilder = NotificationSignatureSessionInfo.builder().withSignatureCertificateLevel(signatureCertificateLevel);
        var semanticsIdentifier = new SemanticsIdentifier(SemanticsIdentifier.IdentityType.PNO, userRequest.getCountry(), userRequest.getNationalIdentityNumber());
        NotificationCertificateChoiceSessionResponse response = smartIdClient.createNotificationCertificateChoice()
                .withCertificateLevel(signatureCertificateLevel)
                .withSemanticsIdentifier(semanticsIdentifier)
                .initCertificateChoice();
        SessionStatus certChoiceSessionStatus = sessionStatusService.poll(response.sessionID());
        CertificateChoiceResponse certChoiceResponse = certificateChoiceResponseValidator.validate(certChoiceSessionStatus, signatureCertificateLevel);
        sessionInfoBuilder.withCertChoiceResponse(certChoiceResponse);
        var signableData = toSignableData(userRequest.getFile(), certChoiceResponse.getCertificate(), sessionInfoBuilder, signatureAlgorithm);
        NotificationSignatureSessionResponse sessionResponse = smartIdClient.createNotificationSignature()
                .withCertificateLevel(signatureCertificateLevel)
                .withSignableData(signableData)
                .withSignatureAlgorithm(signatureAlgorithm)
                .withSemanticsIdentifier(semanticsIdentifier)
                .withInteractions(List.of(NotificationInteraction.displayTextAndPin("Sign the document!")))
                .initSignatureSession();
        var sessionInfo = sessionInfoBuilder.withSessionResponse(sessionResponse).build();
        sessionStore.put(session.getId(), "notificationSignatureSessionInfo", sessionInfo);
        return sessionResponse.vc().value();
    }

    public void checkSignatureStatus(HttpSession session) {
        NotificationSignatureSessionInfo sessionInfo = (NotificationSignatureSessionInfo) sessionStore.get(session.getId(), "notificationSignatureSessionInfo");
        if (sessionInfo == null) {
            throw new SidOperationException("No signing session info found");
        }
        String sessionId = sessionInfo.getSessionId();
        SessionStatus sessionStatus = sessionStatusService.poll(sessionId);
        try {
            CertificateLevel requestedCertificateLevel = sessionInfo.getSignatureCertificateLevel();
            var signatureResponse = signatureResponseValidator.validate(sessionStatus, requestedCertificateLevel);
            sessionInfo.setSignatureResponse(signatureResponse);
        } catch (SessionTimeoutException | UserRefusedException | CertificateLevelMismatchException | UserSelectedWrongVerificationCodeException ex) {
            throw new SidOperationException(ex.getMessage());
        }
    }

    private SignableData toSignableData(MultipartFile uploadedFile,
                                        X509Certificate certificate,
                                        NotificationSignatureSessionInfo.Builder sessionInfoBuilder,
                                        SigningSignatureAlgorithm signatureAlgorithm) {
        Container container = toContainer(uploadedFile);
        DataToSign dataToSign = toDataToSign(container, certificate, signatureAlgorithm);
        sessionInfoBuilder.withDataToSign(dataToSign);
        sessionInfoBuilder.withContainer(container);
        byte[] dataToSignBytes = dataToSign.getDataToSign();
        HashAlgorithm hashAlgorithm = signatureAlgorithm.isLegacyRsa()
                ? signatureAlgorithm.getHashAlgorithmForLegacy()
                : HashAlgorithm.SHA_256;
        return new SignableData(dataToSignBytes, hashAlgorithm);
    }

    private Container toContainer(MultipartFile userDocumentNumberRequest) {
        DataFile uploadedFile = getUploadedDataFile(userDocumentNumberRequest);

        var configuration = new Configuration(Configuration.Mode.TEST);
        return ContainerBuilder.aContainer()
                .withConfiguration(configuration)
                .withDataFile(uploadedFile)
                .build();
    }

    private DataFile getUploadedDataFile(MultipartFile uploadedFile) {
        try {
            return new DataFile(uploadedFile.getInputStream(), uploadedFile.getOriginalFilename(), uploadedFile.getContentType());
        } catch (IOException e) {
            throw new FileUploadException(e.getCause());
        }
    }

    private static DataToSign toDataToSign(Container container, X509Certificate certificate,
                                          SigningSignatureAlgorithm signatureAlgorithm) {
        return SignatureBuilder.aSignature(container)
                .withSigningCertificate(certificate)
                .withSignatureDigestAlgorithm(toDigestAlgorithm(signatureAlgorithm))
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
                default -> DigestAlgorithm.SHA256;
            };
        }
        return DigestAlgorithm.SHA256;
    }
}
