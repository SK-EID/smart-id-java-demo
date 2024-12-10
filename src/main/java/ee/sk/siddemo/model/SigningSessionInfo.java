package ee.sk.siddemo.model;

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

import org.digidoc4j.Container;
import org.digidoc4j.DataToSign;

import ee.sk.smartid.v2.SignableHash;

public class SigningSessionInfo {

    private final String sessionID;
    private final String verificationCode;
    private final DataToSign dataToSign;
    private final Container container;
    private final SignableHash hashToSign;

    private final String documentNumber;

    private SigningSessionInfo(Builder builder) {
        this.sessionID = builder.sessionID;
        this.verificationCode = builder.verificationCode;
        this.dataToSign = builder.dataToSign;
        this.container = builder.container;
        this.hashToSign = builder.hashToSign;
        this.documentNumber = builder.documentNumber;
    }

    public String getSessionID() {
        return sessionID;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public DataToSign getDataToSign() {
        return dataToSign;
    }

    public Container getContainer() {
        return container;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public SignableHash getHashToSign() {
        return hashToSign;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String sessionID;
        private String verificationCode;
        private DataToSign dataToSign;
        private Container container;

        private SignableHash hashToSign;
        private String documentNumber;

        private Builder() {
        }

        public Builder withSessionID(String sessionID) {
            this.sessionID = sessionID;
            return this;
        }

        public Builder withVerificationCode(String verificationCode) {
            this.verificationCode = verificationCode;
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

        public Builder withDocumentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
            return this;
        }

        public Builder withHashToSign(SignableHash hashToSign) {
            this.hashToSign = hashToSign;
            return this;
        }

        public SigningSessionInfo build() {
            return new SigningSessionInfo(this);
        }
    }

}
