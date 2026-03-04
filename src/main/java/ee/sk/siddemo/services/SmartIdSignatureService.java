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

import org.digidoc4j.DataToSign;
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

    public SmartIdSignatureService(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    public SigningResult handleSignatureResult(HttpSession session) {
        SignatureSessionInfo signatureSessionInfo = getSignatureSessionInfo(session);
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

        return SigningResult.newBuilder()
                .withResult("Signing successful")
                .withValid(true)
                .withTimestamp(Date.from(ZonedDateTime.now().toInstant()))
                .withContainerFilePath("N/A – container not created in demo")
                .build();
    }

    private SignatureSessionInfo getSignatureSessionInfo(HttpSession session) {
        SignatureSessionInfo deviceLinkSignatureSessionInfo = (SignatureSessionInfo) sessionStore.get(session.getId(), "deviceLinkSessionInfo");
        if (deviceLinkSignatureSessionInfo != null) {
            return deviceLinkSignatureSessionInfo;
        }
        SignatureSessionInfo notificationBasedSignatureSessionInfo = (SignatureSessionInfo) sessionStore.get(session.getId(), "notificationSignatureSessionInfo");
        if (notificationBasedSignatureSessionInfo != null) {
            return notificationBasedSignatureSessionInfo;
        }
        throw new SidOperationException("No signature session info found in the current session");
    }
}
