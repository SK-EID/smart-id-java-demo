package ee.sk.siddemo.model;

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

import ee.sk.smartid.CertificateByDocumentNumberResult;
import ee.sk.smartid.CertificateChoiceResponse;
import ee.sk.smartid.CertificateLevel;
import ee.sk.smartid.SignatureResponse;
import ee.sk.smartid.rest.dao.NotificationSignatureSessionResponse;

public class NotificationSignatureSessionInfo implements SignatureSessionInfo {

    private final CertificateByDocumentNumberResult certificateResult;
    private final CertificateChoiceResponse certChoiceResponse;
    private final NotificationSignatureSessionResponse sessionResponse;
    private final DataToSign dataToSign;
    private final Container container;
    private final CertificateLevel signatureCertificateLevel;

    private SignatureResponse signatureResponse;

    private NotificationSignatureSessionInfo(CertificateByDocumentNumberResult certificateResult,
                                             CertificateChoiceResponse certChoiceResponse,
                                             NotificationSignatureSessionResponse sessionResponse,
                                             DataToSign dataToSign,
                                             Container container,
                                             CertificateLevel signatureCertificateLevel) {
        this.certificateResult = certificateResult;
        this.certChoiceResponse = certChoiceResponse;
        this.sessionResponse = sessionResponse;
        this.dataToSign = dataToSign;
        this.container = container;
        this.signatureCertificateLevel = signatureCertificateLevel;
    }

    @Override
    public SignatureResponse getSignatureResponse() {
        return signatureResponse;
    }

    @Override
    public DataToSign getDataToSign() {
        return dataToSign;
    }

    @Override
    public Container getContainer() {
        return container;
    }

    public void setSignatureResponse(SignatureResponse signatureResponse) {
        this.signatureResponse = signatureResponse;
    }

    public String getSessionId() {
        return sessionResponse.sessionID();
    }

    public static Builder builder() {
        return new Builder();
    }

    public CertificateLevel getSignatureCertificateLevel() {
        return signatureCertificateLevel;
    }

    public static class Builder {

        private CertificateByDocumentNumberResult certificateResult;
        private NotificationSignatureSessionResponse sessionResponse;
        private DataToSign dataToSign;
        private Container container;
        private CertificateLevel signatureCertificateLevel;
        private CertificateChoiceResponse certChoiceResponse;

        public Builder withCertificateResult(CertificateByDocumentNumberResult certificateResult) {
            this.certificateResult = certificateResult;
            return this;
        }

        public Builder withSessionResponse(NotificationSignatureSessionResponse sessionResponse) {
            this.sessionResponse = sessionResponse;
            return this;
        }

        public Builder withDataToSign(DataToSign dataToSign) {
            this.dataToSign = dataToSign;
            return this;
        }

        public Builder withContainer(Container container) {
            this.container = container;
            return this;
        }

        public Builder withSignatureCertificateLevel(CertificateLevel signatureCertificateLevel) {
            this.signatureCertificateLevel = signatureCertificateLevel;
            return this;
        }

        public Builder withCertChoiceResponse(CertificateChoiceResponse certChoiceResponse) {
            this.certChoiceResponse = certChoiceResponse;
            return this;
        }

        public NotificationSignatureSessionInfo build() {
            return new NotificationSignatureSessionInfo(certificateResult, certChoiceResponse, sessionResponse, dataToSign, container, signatureCertificateLevel);
        }
    }
}
