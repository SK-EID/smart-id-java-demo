package ee.sk.siddemo.services;

/*-
 * #%L
 * Smart-ID sample Java client
 * %%
 * Copyright (C) 2018 - 2019 SK ID Solutions AS
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

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.digidoc4j.Configuration;
import org.digidoc4j.Container;
import org.digidoc4j.ContainerBuilder;
import org.digidoc4j.DataFile;
import org.digidoc4j.DataToSign;
import org.digidoc4j.DigestAlgorithm;
import org.digidoc4j.Signature;
import org.digidoc4j.SignatureBuilder;
import org.digidoc4j.SignatureProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ee.sk.siddemo.exception.FileUploadException;
import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.SigningResult;
import ee.sk.siddemo.model.SigningSessionInfo;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.smartid.HashType;
import ee.sk.smartid.SignableData;
import ee.sk.smartid.SignableHash;
import ee.sk.smartid.SmartIdCertificate;
import ee.sk.smartid.SmartIdClient;
import ee.sk.smartid.SmartIdSignature;
import ee.sk.smartid.exception.permanent.ServerMaintenanceException;
import ee.sk.smartid.exception.useraccount.DocumentUnusableException;
import ee.sk.smartid.exception.useraccount.UserAccountNotFoundException;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.exception.useraction.UserRefusedException;
import ee.sk.smartid.exception.useraction.UserSelectedWrongVerificationCodeException;
import ee.sk.smartid.rest.SmartIdConnector;
import ee.sk.smartid.rest.dao.Interaction;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.rest.dao.SessionStatus;

@Service
public class SmartIdSignatureServiceImpl implements SmartIdSignatureService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdSignatureServiceImpl.class);

    @Value("${sid.sign.displayText}")
    private String sidSignDisplayText;

    @Value("${sid.client.relyingPartyUuid}")
    private String sidRelyingPartyUuid;

    @Value("${sid.client.relyingPartyName}")
    private String sidRelyingPartyName;

    private final SmartIdCertificateService certificateService;
    private final SmartIdClient client;

    public SmartIdSignatureServiceImpl(SmartIdCertificateService certificateService, SmartIdClient client) {
        this.certificateService = certificateService;
        this.client = client;
    }

    @Override
    public SigningSessionInfo sendSignatureRequest(UserRequest userRequest) {

        var semanticsIdentifier = new SemanticsIdentifier(
                // 3 character identity type
                // (PAS-passport, IDC-national identity card or PNO - (national) personal number)
                SemanticsIdentifier.IdentityType.PNO,
                userRequest.getCountry(), // 2 character ISO 3166-1 alpha-2 country code
                userRequest.getNationalIdentityNumber());

        DataFile uploadedFile = getUploadedDataFile(userRequest.getFile());

        var configuration = new Configuration(Configuration.Mode.TEST);

        Container container = ContainerBuilder.aContainer()
                .withConfiguration(configuration)
                .withDataFile(uploadedFile)
                .build();

        SmartIdCertificate signingCert = certificateService.getCertificate(userRequest);

        DataToSign dataToSignExternally = SignatureBuilder.aSignature(container)
                .withSigningCertificate(signingCert.getCertificate())
                .withSignatureDigestAlgorithm(DigestAlgorithm.SHA256)
                .withSignatureProfile(SignatureProfile.LT)
                .buildDataToSign();


        var signableData = new SignableData(dataToSignExternally.getDataToSign());
        signableData.setHashType(HashType.SHA256);

        var hashToSign = new SignableHash();
        hashToSign.setHash(signableData.calculateHash());
        hashToSign.setHashType(signableData.getHashType());


        return SigningSessionInfo.newBuilder()
                .withVerificationCode(hashToSign.calculateVerificationCode())
                .withDataToSign(dataToSignExternally)
                .withContainer(container)
                .withDocumentNumber(signingCert.getDocumentNumber())
                .withHashToSign(hashToSign)
                .build();
    }

    private DataFile getUploadedDataFile(MultipartFile uploadedFile) {
        try {
            return new DataFile(uploadedFile.getInputStream(), uploadedFile.getOriginalFilename(), uploadedFile.getContentType());
        } catch (IOException e) {
            throw new FileUploadException(e.getCause());
        }
    }

    @Override
    public SigningResult sign(SigningSessionInfo signingSessionInfo) {
        String filePath;
        Signature signature;

        try {

            SmartIdSignature smartIdSignature = client
                    .createSignature()
                    .withDocumentNumber(signingSessionInfo.getDocumentNumber())
                    .withSignableHash(signingSessionInfo.getHashToSign())
                    .withCertificateLevel("QUALIFIED")
                    .withAllowedInteractionsOrder(asList(
                            Interaction.confirmationMessage("Confirmation message dialogue"),
                            Interaction.displayTextAndPIN("Do you want to sign the file?")
                    ))
                    .sign();

            byte[] signatureValue = smartIdSignature.getValue();


            signature = signingSessionInfo.getDataToSign().finalize(signatureValue);
            signingSessionInfo.getContainer().addSignature(signature);

            File containerFile = File.createTempFile("sid-demo-container-", ".asice");
            filePath = containerFile.getAbsolutePath();
            signingSessionInfo.getContainer().saveAsFile(filePath);
        } catch (UserAccountNotFoundException | UserRefusedException | UserSelectedWrongVerificationCodeException | SessionTimeoutException |
                 DocumentUnusableException | ServerMaintenanceException e) {
            logger.warn("Smart-ID service returned internal error that cannot be handled locally.");
            throw new SidOperationException("Smart-ID internal error", e);
        } catch (IOException e) {
            throw new SidOperationException("Could not create container file.", e);
        }

        return SigningResult.newBuilder()
                .withResult("Signing successful")
                .withValid(signature.validateSignature().isValid())
                .withTimestamp(signature.getTimeStampCreationTime())
                .withContainerFilePath(filePath)
                .build();
    }

    private SessionStatus pollSessionStatus(String sessionId, SmartIdConnector connector1) throws InterruptedException {
        SessionStatus sessionStatus = null;
        while (sessionStatus == null || "RUNNING".equalsIgnoreCase(sessionStatus.getState())) {
            sessionStatus = connector1.getSessionStatus(sessionId);
            TimeUnit.SECONDS.sleep(1);
        }
        return sessionStatus;
    }

}
