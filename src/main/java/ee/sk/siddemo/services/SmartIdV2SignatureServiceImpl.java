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
import java.nio.file.Path;
import java.nio.file.Paths;

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
import ee.sk.smartid.v2.SignableData;
import ee.sk.smartid.v2.SignableHash;
import ee.sk.smartid.v2.SmartIdCertificate;
import ee.sk.smartid.v2.SmartIdClient;
import ee.sk.smartid.v2.SmartIdSignature;
import ee.sk.smartid.exception.permanent.ServerMaintenanceException;
import ee.sk.smartid.exception.useraccount.DocumentUnusableException;
import ee.sk.smartid.exception.useraccount.UserAccountNotFoundException;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.exception.useraction.UserRefusedException;
import ee.sk.smartid.exception.useraction.UserSelectedWrongVerificationCodeException;
import ee.sk.smartid.v2.rest.dao.Interaction;
import ee.sk.smartid.v2.rest.dao.SemanticsIdentifier;

@Service
public class SmartIdV2SignatureServiceImpl implements SmartIdV2SignatureService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV2SignatureServiceImpl.class);

    @Value("${sid.v2.sign.displayText}")
    private String sidSignDisplayText;

    @Value("${sid.v2.client.relyingPartyUuid}")
    private String sidRelyingPartyUuid;

    @Value("${sid.v2.client.relyingPartyName}")
    private String sidRelyingPartyName;

    @Value("${app.signed-files-directory}")
    private String signedFilesDirectory;

    private final SmartIdCertificateService certificateService;
    private final SmartIdClient smartIdClientV2;

    public SmartIdV2SignatureServiceImpl(SmartIdCertificateService certificateService, SmartIdClient smartIdClientV2) {
        this.certificateService = certificateService;
        this.smartIdClientV2 = smartIdClientV2;
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
        Signature signature;
        Path targetPath;

        try {

            SmartIdSignature smartIdSignature = smartIdClientV2
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
            targetPath = createSavePath(containerFile);
            signingSessionInfo.getContainer().saveAsFile(targetPath.toString());
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
                .withContainerFilePath(targetPath.toString())
                .build();
    }

    private Path createSavePath(File containerFile) {
        Path targetDir = Paths.get(signedFilesDirectory);
        File directory = targetDir.toFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return targetDir.resolve(containerFile.getName());
    }
}
