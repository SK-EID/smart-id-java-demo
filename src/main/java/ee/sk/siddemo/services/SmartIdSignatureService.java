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

import java.time.ZonedDateTime;
import java.util.Date;

import org.digidoc4j.Container;
import org.digidoc4j.DataToSign;
import org.digidoc4j.Signature;
import org.digidoc4j.ValidationResult;
import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.SignatureSessionInfo;
import ee.sk.siddemo.model.SigningResult;
import ee.sk.smartid.SignatureResponse;
import ee.sk.smartid.SignatureValueValidator;
import ee.sk.smartid.SignatureValueValidatorImpl;
import ee.sk.smartid.SigningSignatureAlgorithm;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdSignatureService {

    private final SessionStore sessionStore;
    private final FileService fileService;

    public SmartIdSignatureService(SessionStore sessionStore, FileService fileService) {
        this.sessionStore = sessionStore;
        this.fileService = fileService;
    }

    public SigningResult handleSignatureResult(HttpSession session) {
        SignatureSessionInfo signatureSessionInfo = consumeSignatureSessionInfo(session);
        SignatureResponse signatureResponse = signatureSessionInfo.getSignatureResponse();
        DataToSign dataToSign = signatureSessionInfo.getDataToSign();
        if (signatureResponse == null || dataToSign == null) {
            throw new SidOperationException("Required session data is missing");
        }
        SignatureValueValidator validator = new SignatureValueValidatorImpl();
        SigningSignatureAlgorithm signatureAlgorithm = signatureResponse.getSignatureAlgorithm();
        if (signatureAlgorithm != null && signatureAlgorithm.isLegacyRsa()) {
            validator.validate(
                    signatureResponse.getSignatureValue(),
                    dataToSign.getDataToSign(),
                    signatureResponse.getCertificate(),
                    signatureAlgorithm.getAlgorithmName());
        } else {
            validator.validate(
                    signatureResponse.getSignatureValue(),
                    dataToSign.getDataToSign(),
                    signatureResponse.getCertificate(),
                    signatureResponse.getRsaSsaPssParameters());
        }

        boolean valid = true;
        Date timestamp = Date.from(ZonedDateTime.now().toInstant());
        String containerFilePath = "N/A – container not created in demo";

        // DigiDoc4J finalize() verifies the signature using RSA PKCS#1 v1.5 only; it does not support RSASSA-PSS.
        // When Smart-ID returns an RSASSA-PSS signature, skip container finalization and saving
        // so that signing still succeeds (client-side validation above already passed).
        // When RSASSA-PSS support is added to DigiDoc4J then all algorithms can use DigiDoc4JContainer
        boolean useDigiDoc4JContainer = signatureAlgorithm != null && signatureAlgorithm.isLegacyRsa();

        Container container = signatureSessionInfo.getContainer();
        if (useDigiDoc4JContainer && container != null) {
            byte[] signatureValue = signatureResponse.getSignatureValue();
            Signature digiDoc4jSignature = dataToSign.finalize(signatureValue);
            container.addSignature(digiDoc4jSignature);

            ValidationResult validationResult = digiDoc4jSignature.validateSignature();
            valid = validationResult.isValid();
            Date ts = digiDoc4jSignature.getTimeStampCreationTime();
            if (ts != null) {
                timestamp = ts;
            }

            try {
                String targetPath = fileService.createPath();
                container.saveAsFile(targetPath);
                containerFilePath = targetPath;
            } catch (RuntimeException e) {
                throw new SidOperationException("Could not save signed container", e);
            }
        } else if (useDigiDoc4JContainer) {
            throw new SidOperationException("Container was not created for this signing session");
        } else if (container != null) {
            containerFilePath = "N/A – container not saved (RSASSA-PSS not supported by DigiDoc4J finalization)";
        }

        return SigningResult.newBuilder()
                .withResult("Signing successful")
                .withValid(valid)
                .withTimestamp(timestamp)
                .withContainerFilePath(containerFilePath)
                .build();
    }

    private SignatureSessionInfo consumeSignatureSessionInfo(HttpSession session) {
        String sessionId = session.getId();
        SignatureSessionInfo deviceLinkSignatureSessionInfo = (SignatureSessionInfo) sessionStore.get(sessionId, "deviceLinkSessionInfo");
        if (deviceLinkSignatureSessionInfo != null) {
            sessionStore.remove(sessionId, "deviceLinkSessionInfo");
            return deviceLinkSignatureSessionInfo;
        }
        SignatureSessionInfo notificationBasedSignatureSessionInfo = (SignatureSessionInfo) sessionStore.get(sessionId, "notificationSignatureSessionInfo");
        if (notificationBasedSignatureSessionInfo != null) {
            sessionStore.remove(sessionId, "notificationSignatureSessionInfo");
            return notificationBasedSignatureSessionInfo;
        }
        throw new SidOperationException("No signature session info found in the current session");
    }
}
