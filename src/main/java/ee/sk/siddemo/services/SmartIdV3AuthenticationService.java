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
import ee.sk.smartid.AuthenticationIdentity;
import ee.sk.smartid.exception.UnprocessableSmartIdResponseException;
import ee.sk.smartid.exception.useraccount.CertificateLevelMismatchException;
import ee.sk.smartid.v3.AuthenticationCertificateLevel;
import ee.sk.smartid.v3.AuthenticationResponse;
import ee.sk.smartid.v3.AuthenticationResponseValidator;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdV3AuthenticationService {

    private final AuthenticationResponseValidator authenticationResponseValidatorV3;

    public SmartIdV3AuthenticationService(AuthenticationResponseValidator authenticationResponseValidatorV3) {
        this.authenticationResponseValidatorV3 = authenticationResponseValidatorV3;
    }

    public AuthenticationIdentity authenticate(HttpSession session) {
        // validate sessions status for dynamic link authentication
        AuthenticationResponse response = (AuthenticationResponse) session.getAttribute("authentication_response");
        String randomChallenge = (String) session.getAttribute("randomChallenge");
        AuthenticationCertificateLevel requestedCertificateLevel = (AuthenticationCertificateLevel) session.getAttribute("requestedCertificateLevel");

        try {
            // validate and map authentication response to authentication identity
            AuthenticationIdentity authenticationIdentity = authenticationResponseValidatorV3.toAuthenticationIdentity(response, requestedCertificateLevel, randomChallenge);
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
