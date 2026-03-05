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

import java.time.Instant;
import java.util.Optional;

import org.digidoc4j.Container;
import org.digidoc4j.DataToSign;

import ee.sk.smartid.CertificateLevel;
import ee.sk.smartid.SignatureResponse;
import ee.sk.smartid.common.devicelink.CallbackUrl;
import ee.sk.smartid.rest.dao.DeviceLinkSessionResponse;
import ee.sk.smartid.rest.dao.DeviceLinkSignatureSessionRequest;

public class DeviceLinkSignatureSessionInfo implements DeviceLinkSessionInfo, SignatureSessionInfo {

    private final DeviceLinkSessionResponse sessionResponse;
    private final DeviceLinkSignatureSessionRequest sessionRequest;
    private final CertificateLevel requestedCertificateLevel;
    private final Container container;
    private final DataToSign dataToSign;
    private final CallbackUrl callbackUrl;

    private SignatureResponse signatureResponse;
    private UserActionMock userActionMock = UserActionMock.NONE;

    private DeviceLinkSignatureSessionInfo(DeviceLinkSessionResponse sessionResponse,
                                           DeviceLinkSignatureSessionRequest sessionRequest,
                                           CertificateLevel requestedCertificateLevel,
                                           Container container,
                                           DataToSign dataToSign,
                                           CallbackUrl callbackUrl) {
        this.sessionResponse = sessionResponse;
        this.sessionRequest = sessionRequest;
        this.requestedCertificateLevel = requestedCertificateLevel;
        this.container = container;
        this.dataToSign = dataToSign;
        this.callbackUrl = callbackUrl;
    }

    @Override
    public Instant getSessionResponseReceived() {
        return sessionResponse.receivedAt();
    }

    @Override
    public String getDeviceLinkBase() {
        return sessionResponse.deviceLinkBase().toString();
    }

    @Override
    public String getSessionToken() {
        return sessionResponse.sessionToken();
    }

    @Override
    public String getInteractions() {
        return sessionRequest.interactions();
    }

    @Override
    public String getDigest() {
        return sessionRequest.signatureProtocolParameters().digest();
    }

    @Override
    public String getSessionSecret() {
        return sessionResponse.sessionSecret();
    }

    @Override
    public String getInitialCallbackUrl() {
        return callbackUrl.initialCallbackUri().toString();
    }

    @Override
    public String getUrlToken() {
        return callbackUrl.urlToken();
    }

    @Override
    public void setUserChallengeVerifier(String userChallengeVerifier) {
        // Not used
    }

    @Override
    public String getUserChallengeVerifier() {
        return "";
    }

    @Override
    public UserActionMock getMockUserAction() {
        return userActionMock;
    }

    public CertificateLevel getRequestedCertificateLevel() {
        return requestedCertificateLevel;
    }

    @Override
    public SignatureResponse getSignatureResponse() {
        return signatureResponse;
    }

    public void setSignatureResponse(SignatureResponse signatureResponse) {
        this.signatureResponse = signatureResponse;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Optional<Container> getContainer() {
        return Optional.of(container);
    }

    @Override
    public DataToSign getDataToSign() {
        return dataToSign;
    }

    public static class Builder {

        private DeviceLinkSessionResponse sessionResponse;
        private DeviceLinkSignatureSessionRequest sessionRequest;
        private CertificateLevel requestedCertificateLevel;
        private Container container;
        private DataToSign dataToSign;
        private CallbackUrl callbackUrl;

        public Builder withSessionResponse(DeviceLinkSessionResponse sessionResponse) {
            this.sessionResponse = sessionResponse;
            return this;
        }

        public Builder withSessionRequest(DeviceLinkSignatureSessionRequest sessionRequest) {
            this.sessionRequest = sessionRequest;
            return this;
        }

        public Builder withRequestedCertificateLevel(CertificateLevel requestedCertificateLevel) {
            this.requestedCertificateLevel = requestedCertificateLevel;
            return this;
        }

        public Builder withContainer(Container container) {
            this.container = container;
            return this;
        }

        public Builder withDataToSign(DataToSign dataToSign) {
            this.dataToSign = dataToSign;
            return this;
        }

        public Builder withCallbackUrl(CallbackUrl callbackUrl) {
            this.callbackUrl = callbackUrl;
            return this;
        }

        public DeviceLinkSignatureSessionInfo build() {
            return new DeviceLinkSignatureSessionInfo(sessionResponse,
                    sessionRequest,
                    requestedCertificateLevel,
                    container,
                    dataToSign,
                    callbackUrl);
        }
    }
}
