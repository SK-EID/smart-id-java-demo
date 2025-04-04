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
import java.time.Instant;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ee.sk.siddemo.exception.FileUploadException;
import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.smartid.exception.useraccount.CertificateLevelMismatchException;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.exception.useraction.UserRefusedException;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.v3.CertificateChoiceResponse;
import ee.sk.smartid.v3.CertificateChoiceResponseMapper;
import ee.sk.smartid.v3.CertificateLevel;
import ee.sk.smartid.v3.SignableData;
import ee.sk.smartid.v3.SignatureResponseMapper;
import ee.sk.smartid.v3.SmartIdClient;
import ee.sk.smartid.v3.rest.dao.DynamicLinkInteraction;
import ee.sk.smartid.v3.rest.dao.DynamicLinkSessionResponse;
import ee.sk.smartid.v3.rest.dao.SessionStatus;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdV3DynamicLinkSignatureService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV3DynamicLinkSignatureService.class);

    private final SmartIdV3NotificationBasedCertificateChoiceService notificationCertificateChoice;
    private final SmartIdV3SessionsStatusService sessionsStatusService;
    private final SmartIdClient smartIdClientV3;

    public SmartIdV3DynamicLinkSignatureService(SmartIdV3NotificationBasedCertificateChoiceService notificationCertificateChoice,
                                                SmartIdV3SessionsStatusService sessionsStatusService,
                                                SmartIdClient smartIdClientV3) {
        this.notificationCertificateChoice = notificationCertificateChoice;
        this.sessionsStatusService = sessionsStatusService;
        this.smartIdClientV3 = smartIdClientV3;
    }

    public void startSigningWithDocumentNumber(HttpSession session, UserDocumentNumberRequest userDocumentNumberRequest) {
        notificationCertificateChoice.startCertificateChoice(session, userDocumentNumberRequest);
        var signableData = toSignableData(userDocumentNumberRequest.getFile(), session);
        var signatureCertificateLevel = CertificateLevel.QUALIFIED;
        DynamicLinkSessionResponse sessionResponse = smartIdClientV3.createDynamicLinkSignature()
                .withCertificateLevel(signatureCertificateLevel)
                .withSignableData(signableData)
                .withAllowedInteractionsOrder(List.of(DynamicLinkInteraction.displayTextAndPIN("Sign the document!")))
                .withDocumentNumber(userDocumentNumberRequest.getDocumentNumber())
                .initSignatureSession();
        Instant responseReceivedTime = Instant.now();

        saveResponseAttributes(session, signatureCertificateLevel, sessionResponse, responseReceivedTime);

        sessionsStatusService.startPolling(session, sessionResponse.getSessionID());
    }

    public void startSigningWithPersonCode(HttpSession session, UserRequest userRequest) {
        notificationCertificateChoice.startCertificateChoice(session, userRequest);
        var signableData = toSignableData(userRequest.getFile(), session);
        var semanticsIdentifier = new SemanticsIdentifier(SemanticsIdentifier.IdentityType.PNO, userRequest.getCountry(), userRequest.getNationalIdentityNumber());
        var requestedCertificateLevel = CertificateLevel.QUALIFIED;
        DynamicLinkSessionResponse sessionResponse = smartIdClientV3.createDynamicLinkSignature()
                .withCertificateLevel(requestedCertificateLevel)
                .withSignableData(signableData)
                .withSemanticsIdentifier(semanticsIdentifier)
                .withAllowedInteractionsOrder(List.of(DynamicLinkInteraction.displayTextAndPIN("Sign the document!")))
                .initSignatureSession();
        Instant responseReceivedTime = Instant.now();

        saveResponseAttributes(session, requestedCertificateLevel, sessionResponse, responseReceivedTime);

        sessionsStatusService.startPolling(session, sessionResponse.getSessionID());
    }

    public boolean checkSignatureStatus(HttpSession session) {
        Optional<SessionStatus> sessionStatus = sessionsStatusService.getSessionsStatus(session.getId());
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

    private SignableData toSignableData(MultipartFile file,
                                        HttpSession session) {
        Container container = toContainer(file);
        X509Certificate certificate = getX509Certificate(session);
        DataToSign dataToSign = toDataToSign(container, certificate);
        saveSigningAttributes(session, container, dataToSign);
        return new SignableData(dataToSign.getDataToSign());
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

    private X509Certificate getX509Certificate(HttpSession session) {
        Optional<SessionStatus> certSessionStatus;
        do {
            certSessionStatus = getCertifiateChoiceSessionStatus(session);
        } while (certSessionStatus.isEmpty());

        CertificateChoiceResponse certificateChoiceResponse = CertificateChoiceResponseMapper.from(certSessionStatus.get());
        return certificateChoiceResponse.getCertificate();
    }

    private Optional<SessionStatus> getCertifiateChoiceSessionStatus(HttpSession session) {
        Optional<SessionStatus> certSessionStatus;
        try {
            certSessionStatus = sessionsStatusService.getSessionsStatus(session.getId());
        } catch (SessionTimeoutException | UserRefusedException ex) {
            throw new SidOperationException(ex.getMessage());
        }
        return certSessionStatus;
    }

    private static void saveSigningAttributes(HttpSession session, Container container, DataToSign dataToSign) {
        session.setAttribute("container", container);
        session.setAttribute("dataToSign", dataToSign);
    }

    private static void saveResponseAttributes(HttpSession session,
                                               CertificateLevel requestedCertificateLevel,
                                               DynamicLinkSessionResponse sessionResponse,
                                               Instant responseReceivedTime) {
        session.setAttribute("requestedCertificateLevel", requestedCertificateLevel);
        session.setAttribute("sessionSecret", sessionResponse.getSessionSecret());
        session.setAttribute("sessionToken", sessionResponse.getSessionToken());
        session.setAttribute("sessionID", sessionResponse.getSessionID());
        session.setAttribute("responseReceivedTime", responseReceivedTime);
    }

    private static DataToSign toDataToSign(Container container, X509Certificate certificate) {
        return SignatureBuilder.aSignature(container)
                .withSigningCertificate(certificate)
                .withSignatureDigestAlgorithm(DigestAlgorithm.SHA512)
                .withSignatureProfile(SignatureProfile.LT)
                .buildDataToSign();
    }

    private static void saveValidateResponse(HttpSession session, SessionStatus status) {
        try {
            CertificateLevel requestedCertificateLevel = (CertificateLevel) session.getAttribute("requestedCertificateLevel");
            var dynamicLinkSignatureResponse = SignatureResponseMapper.from(status, requestedCertificateLevel.name());
            session.setAttribute("signing_response", dynamicLinkSignatureResponse);
        } catch (SessionTimeoutException | UserRefusedException | CertificateLevelMismatchException ex) {
            throw new SidOperationException(ex.getMessage());
        }
    }
}
