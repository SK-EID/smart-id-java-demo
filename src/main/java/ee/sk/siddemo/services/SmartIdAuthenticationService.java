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

import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.DeviceLinkAuthenticationDeviceLinkSessionInfo;
import ee.sk.siddemo.model.NotificationAuthenticationSessionInfo;
import ee.sk.smartid.AuthenticationIdentity;
import ee.sk.smartid.DeviceLinkAuthenticationResponseValidator;
import ee.sk.smartid.NotificationAuthenticationResponseValidator;
import ee.sk.smartid.exception.UnprocessableSmartIdResponseException;
import ee.sk.smartid.exception.useraccount.CertificateLevelMismatchException;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdAuthenticationService {

    private final DeviceLinkAuthenticationResponseValidator deviceLinkAuthenticationResponseValidator;
    private final NotificationAuthenticationResponseValidator notificationAuthenticationResponseValidator;
    private final SessionStore sessionStore;

    public SmartIdAuthenticationService(DeviceLinkAuthenticationResponseValidator deviceLinkAuthenticationResponseValidator,
                                        NotificationAuthenticationResponseValidator notificationAuthenticationResponseValidator, SessionStore sessionStore) {
        this.deviceLinkAuthenticationResponseValidator = deviceLinkAuthenticationResponseValidator;
        this.notificationAuthenticationResponseValidator = notificationAuthenticationResponseValidator;
        this.sessionStore = sessionStore;
    }

    public AuthenticationIdentity authenticate(HttpSession session) {
        // validate sessions status for dynamic link authentication
        DeviceLinkAuthenticationDeviceLinkSessionInfo deviceLinkSessionInfo = (DeviceLinkAuthenticationDeviceLinkSessionInfo) sessionStore.get(session.getId(), "deviceLinkSessionInfo");
        AuthenticationIdentity authenticationIdentity = null;
        if (deviceLinkSessionInfo != null) {
            // validate and map authentication response to authentication identity
            authenticationIdentity = deviceLinkAuthenticationResponseValidator.validate(deviceLinkSessionInfo.getSessionStatus(),
                    deviceLinkSessionInfo.getRequest(), deviceLinkSessionInfo.getUserChallengeVerifier(), "smart-id-demo", null);
        }
        NotificationAuthenticationSessionInfo notificationAuthenticationSessionInfo = (NotificationAuthenticationSessionInfo) sessionStore.get(session.getId(), "notificationAuthenticationSessionInfo");
        if (notificationAuthenticationSessionInfo != null) {
            // validate and map authentication response to authentication identity
            authenticationIdentity = notificationAuthenticationResponseValidator.validate(notificationAuthenticationSessionInfo.getSessionStatus(),
                    notificationAuthenticationSessionInfo.getAuthenticationSessionRequest(), "smart-id-demo");
        }
        if (authenticationIdentity == null) {
            throw new SidOperationException("No authentication session request found in the current session");
        }

        try {
            // invalidate current session after successful authentication
            session.invalidate();
            return authenticationIdentity;
        } catch (UnprocessableSmartIdResponseException ex) {
            throw new SidOperationException("Invalid authentication response", ex);
        } catch (CertificateLevelMismatchException ex) {
            throw new SidOperationException("Certificate level mismatch", ex);
        }
    }
}
