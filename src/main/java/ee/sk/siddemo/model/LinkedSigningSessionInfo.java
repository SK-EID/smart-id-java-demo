package ee.sk.siddemo.model;

/*-
 * #%L
 * Smart-ID sample Java client
 * %%
 * Copyright (C) 2018 - 2026 SK ID Solutions AS
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

import org.digidoc4j.Container;
import org.digidoc4j.DataFile;
import org.digidoc4j.DataToSign;

import ee.sk.smartid.CertificateChoiceResponse;
import ee.sk.smartid.CertificateLevel;
import ee.sk.smartid.SignatureResponse;
import ee.sk.smartid.common.devicelink.CallbackUrl;
import ee.sk.smartid.rest.dao.DeviceLinkSessionResponse;
import ee.sk.smartid.signature.SigningSignatureAlgorithm;

public class LinkedSigningSessionInfo implements DeviceLinkSessionInfo, SignatureSessionInfo {

    private final DeviceLinkSessionResponse certificateSessionResponse;
    private final CertificateLevel certificateLevel;
    private final DataFile uploadedDataFile;
    private final CallbackUrl callbackUrl;
    private final SigningSignatureAlgorithm signingSignatureAlgorithm;

    private CertificateChoiceResponse certificateChoiceResponse;
    private Container container;
    private DataToSign dataToSign;
    private SignatureResponse signatureResponse;
    private UserActionMock userActionMock = UserActionMock.NONE;

    public LinkedSigningSessionInfo(DeviceLinkSessionResponse certificateSessionResponse,
                                    CertificateLevel certificateLevel,
                                    DataFile uploadedDataFile,
                                    CallbackUrl callbackUrl,
                                    SigningSignatureAlgorithm signingSignatureAlgorithm) {
        this.certificateSessionResponse = certificateSessionResponse;
        this.certificateLevel = certificateLevel;
        this.uploadedDataFile = uploadedDataFile;
        this.callbackUrl = callbackUrl;
        this.signingSignatureAlgorithm = signingSignatureAlgorithm;
    }

    @Override
    public Instant getSessionResponseReceived() {
        return certificateSessionResponse.receivedAt();
    }

    @Override
    public String getDeviceLinkBase() {
        return certificateSessionResponse.deviceLinkBase().toString();
    }

    @Override
    public String getSessionToken() {
        return certificateSessionResponse.sessionToken();
    }

    @Override
    public String getInteractions() {
        return "";
    }

    @Override
    public String getDigest() {
        return "";
    }

    @Override
    public String getSessionSecret() {
        return certificateSessionResponse.sessionSecret();
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

    public String getCertificateChoiceSessionId() {
        return certificateSessionResponse.sessionID();
    }

    public CertificateLevel getCertificateLevel() {
        return certificateLevel;
    }

    public DataFile getUploadedDataFile() {
        return uploadedDataFile;
    }

    public SigningSignatureAlgorithm getSigningSignatureAlgorithm() {
        return signingSignatureAlgorithm;
    }

    public void setCertificateChoiceResponse(CertificateChoiceResponse certificateChoiceResponse) {
        this.certificateChoiceResponse = certificateChoiceResponse;
    }

    public CertificateChoiceResponse getCertificateChoiceResponse() {
        return certificateChoiceResponse;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public void setDataToSign(DataToSign dataToSign) {
        this.dataToSign = dataToSign;
    }

    public void setSignatureResponse(SignatureResponse signatureResponse) {
        this.signatureResponse = signatureResponse;
    }

    @Override
    public DataToSign getDataToSign() {
        return dataToSign;
    }

    @Override
    public Container getContainer() {
        return container;
    }

    @Override
    public SignatureResponse getSignatureResponse() {
        return signatureResponse;
    }
}
