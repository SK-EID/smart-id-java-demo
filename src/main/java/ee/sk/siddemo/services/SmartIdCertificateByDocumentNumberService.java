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

import ee.sk.smartid.CertificateByDocumentNumberResult;
import ee.sk.smartid.CertificateLevel;
import ee.sk.smartid.SmartIdClient;
import ee.sk.smartid.exception.permanent.SmartIdClientException;
import ee.sk.smartid.exception.useraccount.DocumentUnusableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmartIdCertificateByDocumentNumberService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdCertificateByDocumentNumberService.class);

    private final SmartIdClient smartIdClient;

    public SmartIdCertificateByDocumentNumberService(SmartIdClient smartIdClient) {
        this.smartIdClient = smartIdClient;
    }

    public CertificateByDocumentNumberResult getCertificate(String documentNumber) {
        try {
            return smartIdClient
                    .createCertificateByDocumentNumber()
                    .withDocumentNumber(documentNumber)
                    .withCertificateLevel(CertificateLevel.QUALIFIED)
                    .getCertificateByDocumentNumber();
        } catch (DocumentUnusableException ex) {
            logger.warn("Document is unusable for documentNumber {}: {}", documentNumber, ex.getMessage());
            throw ex;
        } catch (SmartIdClientException ex) {
            logger.error("SmartIdClient misconfiguration or parameter error for documentNumber {}: {}", documentNumber, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            logger.error("Unexpected error while retrieving certificate for documentNumber {}: {}", documentNumber, ex.getMessage(), ex);
            throw new RuntimeException("Unexpected error while retrieving certificate", ex);
        }
    }
}
