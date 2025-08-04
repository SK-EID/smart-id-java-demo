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
import java.time.ZonedDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.SigningResult;
import ee.sk.smartid.SignatureResponse;
import eu.europa.esig.dss.asic.xades.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.SignatureValue;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdSignatureService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdSignatureService.class);

    private final FileService fileService;

    public SmartIdSignatureService(FileService fileService) {
        this.fileService = fileService;
    }

    public SigningResult handleSignatureResult(HttpSession session) {
        var signatureResponse = (SignatureResponse) session.getAttribute("signatureResponse");
        if (signatureResponse == null) {
            throw new SidOperationException("No signature response found in session");
        }

        byte[] signatureValue = signatureResponse.getSignatureValue();

        ASiCWithXAdESService aSiCEContainerService = (ASiCWithXAdESService) session.getAttribute("containerService");
        DSSDocument document = (DSSDocument) session.getAttribute("dssDocument");
        ASiCWithXAdESSignatureParameters signatureParameters = (ASiCWithXAdESSignatureParameters) session.getAttribute("signatureParameters");
        SignatureValue sv = new SignatureValue(SignatureAlgorithm.RSA_SSA_PSS_SHA512_MGF1, signatureValue);
        DSSDocument dssDocument = aSiCEContainerService.signDocument(List.of(document), signatureParameters, sv);

        String filePath = fileService.createPath();
        try {
            dssDocument.save(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return SigningResult.newBuilder()
                .withResult("Signing successful")
                .withValid(true)
                .withTimestamp(java.util.Date.from(ZonedDateTime.now().toInstant()))
                .withContainerFilePath(filePath)
                .build();
    }
}
