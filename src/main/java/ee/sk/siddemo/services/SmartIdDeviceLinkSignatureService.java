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
import ee.sk.siddemo.model.DeviceLinkSignatureSessionInfo;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.smartid.CertificateByDocumentNumberResult;
import ee.sk.smartid.CertificateLevel;
import ee.sk.smartid.DeviceLinkSignatureSessionRequestBuilder;
import ee.sk.smartid.SignatureResponse;
import ee.sk.smartid.SignatureResponseValidator;
import ee.sk.smartid.SmartIdClient;
import ee.sk.smartid.common.devicelink.CallbackUrl;
import ee.sk.smartid.common.devicelink.interactions.DeviceLinkInteraction;
import ee.sk.smartid.exception.useraccount.CertificateLevelMismatchException;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.exception.useraction.UserRefusedException;
import ee.sk.smartid.rest.dao.DeviceLinkSessionResponse;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.rest.dao.SessionStatus;
import ee.sk.smartid.rest.dao.DeviceLinkSignatureSessionRequest;
import ee.sk.smartid.signature.SignableData;
import ee.sk.smartid.signature.SigningSignatureAlgorithm;
import ee.sk.smartid.util.CallbackUrlUtil;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdDeviceLinkSignatureService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdDeviceLinkSignatureService.class);

    private final SmartIdSessionsStatusService sessionsStatusService;
    private final SmartIdClient smartIdClient;
    private final SignatureResponseValidator signatureResponseValidator;
    private final SessionStore sessionStore;

    @Value("${sid.callbackUrl}")
    private String callbackUrlBase;

    public SmartIdDeviceLinkSignatureService(SmartIdSessionsStatusService sessionsStatusService,
                                             SmartIdClient smartIdClient,
                                             SignatureResponseValidator signatureResponseValidator,
                                             SessionStore sessionStore) {
        this.sessionsStatusService = sessionsStatusService;
        this.smartIdClient = smartIdClient;
        this.signatureResponseValidator = signatureResponseValidator;
        this.sessionStore = sessionStore;
    }

    public void startSigningWithDocumentNumber(HttpSession session, UserDocumentNumberRequest userDocumentNumberRequest) {
        SigningSignatureAlgorithm signatureAlgorithm = fromRequestOrDefault(userDocumentNumberRequest.getSigningSignatureAlgorithm());
        var signatureCertificateLevel = CertificateLevel.QUALIFIED;
        CertificateByDocumentNumberResult certificateByDocumentNumberResult = smartIdClient
                .createCertificateByDocumentNumber()
                .withDocumentNumber(userDocumentNumberRequest.getDocumentNumber())
                .withCertificateLevel(signatureCertificateLevel)
                .getCertificateByDocumentNumber();

        var sessionInfoBuilder = DeviceLinkSignatureSessionInfo.builder().withRequestedCertificateLevel(signatureCertificateLevel);

        CallbackUrl callbackUrl = CallbackUrlUtil.createCallbackUrl(callbackUrlBase);
        sessionInfoBuilder.withCallbackUrl(callbackUrl);
        SignableData signableData = toSignableData(userDocumentNumberRequest.getFile(), certificateByDocumentNumberResult.certificate(), sessionInfoBuilder, signatureAlgorithm);
        var deviceLinkSignatureSessionRequestBuilder = smartIdClient.createDeviceLinkSignature()
                .withCertificateLevel(signatureCertificateLevel)
                .withSignableData(signableData)
                .withSignatureAlgorithm(signatureAlgorithm)
                .withInteractions(List.of(DeviceLinkInteraction.displayTextAndPin("Sign the document!")))
                .withDocumentNumber(userDocumentNumberRequest.getDocumentNumber())
                .withInitialCallbackUrl(callbackUrl.initialCallbackUri().toString());
        DeviceLinkSessionResponse sessionResponse = deviceLinkSignatureSessionRequestBuilder.initSignatureSession();
        DeviceLinkSignatureSessionRequest sessionRequest = deviceLinkSignatureSessionRequestBuilder.getSignatureSessionRequest();

        sessionStore.put(session.getId(), "deviceLinkSessionInfo",
                sessionInfoBuilder.withSessionResponse(sessionResponse).withSessionRequest(sessionRequest).build());
        sessionsStatusService.startPolling(session, sessionResponse.sessionID());
    }

    public void startSigningWithPersonCode(HttpSession session, UserRequest userRequest) {
        SigningSignatureAlgorithm signatureAlgorithm = fromRequestOrDefault(userRequest.getSigningSignatureAlgorithm());
        var signatureCertificateLevel = CertificateLevel.QUALIFIED;
        String documentNumber = (String) session.getAttribute("documentNumber");
        CertificateByDocumentNumberResult certificateByDocumentNumberResult = smartIdClient
                .createCertificateByDocumentNumber()
                .withDocumentNumber(documentNumber)
                .withCertificateLevel(signatureCertificateLevel)
                .getCertificateByDocumentNumber();
        var sessionInfoBuilder = DeviceLinkSignatureSessionInfo.builder().withRequestedCertificateLevel(signatureCertificateLevel);
        CallbackUrl callbackUrl = CallbackUrlUtil.createCallbackUrl(callbackUrlBase);
        sessionInfoBuilder.withCallbackUrl(callbackUrl);
        SignableData signableData = toSignableData(userRequest.getFile(), certificateByDocumentNumberResult.certificate(), sessionInfoBuilder, signatureAlgorithm);
        var semanticsIdentifier = new SemanticsIdentifier(SemanticsIdentifier.IdentityType.PNO, userRequest.getCountry(), userRequest.getNationalIdentityNumber());
        DeviceLinkSignatureSessionRequestBuilder builder = smartIdClient.createDeviceLinkSignature()
                .withCertificateLevel(signatureCertificateLevel)
                .withSignableData(signableData)
                .withSemanticsIdentifier(semanticsIdentifier)
                .withSignatureAlgorithm(signatureAlgorithm)
                .withInteractions(List.of(DeviceLinkInteraction.displayTextAndPin("Sign the document!")))
                .withInitialCallbackUrl(callbackUrl.initialCallbackUri().toString());
        DeviceLinkSessionResponse sessionResponse = builder.initSignatureSession();
        DeviceLinkSignatureSessionRequest sessionRequest = builder.getSignatureSessionRequest();

        sessionStore.put(session.getId(), "deviceLinkSessionInfo",
                sessionInfoBuilder.withSessionResponse(sessionResponse).withSessionRequest(sessionRequest).build());
        sessionsStatusService.startPolling(session, sessionResponse.sessionID());
    }

    public boolean checkSignatureStatus(HttpSession session) {
        Optional<SessionStatus> sessionStatus = sessionsStatusService.getSessionsStatus(session.getId());
        return sessionStatus
                .map(status -> {
                    if (status.getState().equals("COMPLETE")) {
                        saveValidateResponse(session, status);
                        logger.debug("Mobile device IP address: {}", status.getDeviceIpAddress());
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    private void verifySignature(SignatureResponse signatureResponse) {
        logger.info("Signature from Smart-ID validated: algorithm={}, certSubject={}",
                signatureResponse.getSignatureAlgorithm(),
                signatureResponse.getCertificate().getSubjectDN());
    }

    private SignableData toSignableData(MultipartFile file, X509Certificate certificate,
                                        DeviceLinkSignatureSessionInfo.Builder sessionInfoBuilder,
                                        SigningSignatureAlgorithm signatureAlgorithm) {
        Container container = toContainer(file);
        DataToSign dataToSign = toDataToSign(container, certificate, signatureAlgorithm);

        sessionInfoBuilder.withContainer(container).withDataToSign(dataToSign);
        byte[] dataToSignBytes = dataToSign.getDataToSign();
        if (signatureAlgorithm.isLegacyRsa()) {
            return new SignableData(dataToSignBytes, signatureAlgorithm.getHashAlgorithmForLegacy());
        }
        return new SignableData(dataToSignBytes);
    }

    private Container toContainer(MultipartFile file) {
        DataFile uploadedFile = getUploadedDataFile(file);

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

    private void saveValidateResponse(HttpSession session, SessionStatus status) {
        try {
            DeviceLinkSignatureSessionInfo sessionInfo = (DeviceLinkSignatureSessionInfo) sessionStore.get(session.getId(), "deviceLinkSessionInfo");
            var signatureResponse = signatureResponseValidator.validate(status, sessionInfo.getRequestedCertificateLevel());
            sessionInfo.setSignatureResponse(signatureResponse);
            verifySignature(signatureResponse);
        } catch (SessionTimeoutException | UserRefusedException | CertificateLevelMismatchException ex) {
            throw new SidOperationException(ex.getMessage());
        }
    }
}
