package ee.sk.siddemo.services;

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

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.AuthenticationSessionInfo;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.smartid.AuthenticationIdentity;
import ee.sk.smartid.exception.UnprocessableSmartIdResponseException;
import ee.sk.smartid.exception.permanent.ServerMaintenanceException;
import ee.sk.smartid.exception.useraccount.CertificateLevelMismatchException;
import ee.sk.smartid.exception.useraccount.DocumentUnusableException;
import ee.sk.smartid.exception.useraccount.UserAccountNotFoundException;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.exception.useraction.UserRefusedException;
import ee.sk.smartid.exception.useraction.UserSelectedWrongVerificationCodeException;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.v2.AuthenticationHash;
import ee.sk.smartid.v2.AuthenticationResponseValidator;
import ee.sk.smartid.v2.SmartIdAuthenticationResponse;
import ee.sk.smartid.v2.SmartIdClient;
import ee.sk.smartid.v2.rest.dao.Interaction;

@Service
public class SmartIdV2AuthenticationServiceImpl implements SmartIdV2AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV2AuthenticationServiceImpl.class);

    @Value("${sid.v2.auth.displayText}")
    private String sidAuthDisplayText;

    private final SmartIdClient smartIdClientV2;
    private final AuthenticationResponseValidator sidAuthenticationResponseValidator;

    public SmartIdV2AuthenticationServiceImpl(SmartIdClient smartIdClientV2,
                                              AuthenticationResponseValidator sidAuthenticationResponseValidator) {
        this.smartIdClientV2 = smartIdClientV2;
        this.sidAuthenticationResponseValidator = sidAuthenticationResponseValidator;
    }

    @Override
    public AuthenticationSessionInfo startAuthentication(UserRequest userRequest) {


        var semanticsIdentifier = new SemanticsIdentifier(
                // 3 character identity type
                // (PAS-passport, IDC-national identity card or PNO - (national) personal number)
                SemanticsIdentifier.IdentityType.PNO,
                userRequest.getCountry(),
                userRequest.getNationalIdentityNumber()); // identifier (according to country and identity type reference)

// For security reasons a new hash value must be created for each new authentication request
        AuthenticationHash authenticationHash = AuthenticationHash.generateRandomHash();

        String verificationCode = authenticationHash.calculateVerificationCode();

        return AuthenticationSessionInfo.newBuilder()
                .withUserRequest(userRequest)
                .withAuthenticationHash(authenticationHash)
                .withVerificationCode(verificationCode)
                .withSemanticsIdentifier(semanticsIdentifier)
                .build();
    }

    @Override
    public AuthenticationIdentity authenticate(AuthenticationSessionInfo authenticationSessionInfo) {

        UserRequest userRequest = authenticationSessionInfo.getUserRequest();
        AuthenticationHash authenticationHash = authenticationSessionInfo.getAuthenticationHash();

        AuthenticationIdentity authIdentity = null;

        try {
            SmartIdAuthenticationResponse response = smartIdClientV2
                    .createAuthentication()
                    .withSemanticsIdentifier(authenticationSessionInfo.getSemanticsIdentifier())
                    .withAuthenticationHash(authenticationHash)
                    .withCertificateLevel("QUALIFIED") // Certificate level can either be "QUALIFIED" or "ADVANCED"
                    // Smart-ID app will display verification code to the user and user must insert PIN1
                    .withAllowedInteractionsOrder(
                            Collections.singletonList(Interaction.displayTextAndPIN(sidAuthDisplayText)
                            ))
                    .authenticate();


// throws SmartIdResponseValidationException if validation doesn't pass
            authIdentity = sidAuthenticationResponseValidator.validate(response);

            String givenName = authIdentity.getGivenName(); // e.g. Mari-Liis"
            String surname = authIdentity.getSurname(); // e.g. "Männik"
            String identityCode = authIdentity.getIdentityCode(); // e.g. "47101010033"
            String country = authIdentity.getCountry(); // e.g. "EE", "LV", "LT", "BE"
            Optional<LocalDate> dateOfBirth = authIdentity.getDateOfBirth(); // see next paragraph


        } catch (UserAccountNotFoundException e) {
            throw new SidOperationException("User account was not found", e);
        } catch (UserRefusedException e) {
            throw new SidOperationException("User refused", e);
        } catch (UserSelectedWrongVerificationCodeException e) {
            throw new SidOperationException("User selected wrong verification code", e);
        } catch (SessionTimeoutException e) {
            throw new SidOperationException("Session Timeout", e);
        } catch (DocumentUnusableException e) {
            throw new SidOperationException("Document Unusable", e);
        } catch (ServerMaintenanceException e) {
            throw new SidOperationException("Server is under maintenance", e);
        } catch (UnprocessableSmartIdResponseException e) {
            throw new SidOperationException("SID internal error (Unprocessable Smart-ID response)", e);
        } catch (CertificateLevelMismatchException e) {
            throw new SidOperationException("Certificate Level Mismatch", e);
        }

        return authIdentity;
    }
}
