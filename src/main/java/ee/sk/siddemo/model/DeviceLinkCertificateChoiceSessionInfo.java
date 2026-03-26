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

import ee.sk.smartid.CertificateChoiceResponse;
import ee.sk.smartid.CertificateLevel;
import ee.sk.smartid.common.devicelink.CallbackUrl;
import ee.sk.smartid.rest.dao.DeviceLinkSessionResponse;

public class DeviceLinkCertificateChoiceSessionInfo implements DeviceLinkSessionInfo {

    private final DeviceLinkSessionResponse sessionResponse;
    private final CertificateLevel certificateLevel;
    private final CallbackUrl callbackUrl;

    private CertificateChoiceResponse certificateChoiceResponse;
    private UserActionMock userActionMock = UserActionMock.NONE;

    public DeviceLinkCertificateChoiceSessionInfo(DeviceLinkSessionResponse response,
                                                  CertificateLevel certificateLevel,
                                                  CallbackUrl callbackUrl) {
        this.sessionResponse = response;
        this.certificateLevel = certificateLevel;
        this.callbackUrl = callbackUrl;
    }

    public CertificateLevel getCertificateLevel() {
        return certificateLevel;
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
        return "";
    }

    @Override
    public String getDigest() {
        return "";
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

    public CertificateChoiceResponse getCertificateChoiceResponse() {
        return certificateChoiceResponse;
    }

    public void setCertificateChoiceResponse(CertificateChoiceResponse certificateChoiceResponse) {
        this.certificateChoiceResponse = certificateChoiceResponse;
    }
}
