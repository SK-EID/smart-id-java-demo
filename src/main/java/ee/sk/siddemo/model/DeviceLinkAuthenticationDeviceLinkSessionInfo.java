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

import ee.sk.smartid.common.devicelink.CallbackUrl;
import ee.sk.smartid.rest.dao.DeviceLinkAuthenticationSessionRequest;
import ee.sk.smartid.rest.dao.DeviceLinkSessionResponse;
import ee.sk.smartid.rest.dao.SessionStatus;

public class DeviceLinkAuthenticationDeviceLinkSessionInfo implements DeviceLinkSessionInfo {

    private final DeviceLinkSessionResponse sessionResponse;
    private final DeviceLinkAuthenticationSessionRequest sessionRequest;
    private final CallbackUrl callbackUrl;
    private final UserActionMock userActionMock;

    private SessionStatus sessionStatus;
    private String userChallengeVerifier;

    public DeviceLinkAuthenticationDeviceLinkSessionInfo(DeviceLinkSessionResponse sessionResponse,
                                                         DeviceLinkAuthenticationSessionRequest sessionRequest,
                                                         CallbackUrl callbackUrl,
                                                         UserActionMock userActionMock) {
        this.sessionResponse = sessionResponse;
        this.sessionRequest = sessionRequest;
        this.callbackUrl = callbackUrl;
        this.userActionMock = userActionMock;
    }

    public String getSessionId() {
        return sessionResponse.sessionID();
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
    public String getSessionSecret() {
        return sessionResponse.sessionSecret();
    }

    @Override
    public String getDigest() {
        return sessionRequest.signatureProtocolParameters().rpChallenge();
    }

    @Override
    public String getInteractions() {
        return sessionRequest.interactions();
    }

    public DeviceLinkAuthenticationSessionRequest getRequest() {
        return sessionRequest;
    }

    public SessionStatus getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(SessionStatus sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    @Override
    public String getUrlToken() {
        return callbackUrl.urlToken();
    }

    @Override
    public String getInitialCallbackUrl() {
        return callbackUrl.initialCallbackUri().toString();
    }

    @Override
    public String getUserChallengeVerifier() {
        return userChallengeVerifier;
    }

    @Override
    public UserActionMock getMockUserAction() {
        return userActionMock;
    }

    @Override
    public void setUserChallengeVerifier(String userChallengeVerifier) {
        this.userChallengeVerifier = userChallengeVerifier;
    }
}
