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

import org.digidoc4j.Container;
import org.digidoc4j.DataToSign;
import org.digidoc4j.Signature;
import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.SigningResult;
import ee.sk.smartid.SignatureResponse;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdSignatureService {

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
        DataToSign dataToSign = (DataToSign) session.getAttribute("dataToSign");
        Signature signature = dataToSign.finalize(signatureValue);

        Container container = (Container) session.getAttribute("container");
        container.addSignature(signature);
        String filePath = fileService.createPath();
        container.saveAsFile(filePath);
        return SigningResult.newBuilder()
                .withResult("Signing successful")
                .withValid(signature.validateSignature().isValid())
                .withTimestamp(signature.getTimeStampCreationTime())
                .withContainerFilePath(filePath)
                .build();
    }
}
