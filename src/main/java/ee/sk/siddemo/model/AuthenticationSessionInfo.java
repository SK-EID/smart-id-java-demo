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


import ee.sk.smartid.v2.AuthenticationHash;
import ee.sk.smartid.v2.rest.dao.SemanticsIdentifier;

public class AuthenticationSessionInfo {

    private final AuthenticationHash authenticationHash;
    private final String verificationCode;
    private final UserRequest userRequest;
    private final SemanticsIdentifier semanticsIdentifier;

    private AuthenticationSessionInfo(Builder builder) {
        this.authenticationHash = builder.authenticationHash;
        this.verificationCode = builder.verificationCode;
        this.userRequest = builder.userRequest;
        this.semanticsIdentifier = builder.semanticsIdentifier;
    }

    public AuthenticationHash getAuthenticationHash() {
        return authenticationHash;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public UserRequest getUserRequest() {
        return userRequest;
    }

    public SemanticsIdentifier getSemanticsIdentifier() {
        return semanticsIdentifier;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String verificationCode;
        private UserRequest userRequest;
        private AuthenticationHash authenticationHash;

        private SemanticsIdentifier semanticsIdentifier;

        private Builder() {
        }

        public Builder withAuthenticationHash(AuthenticationHash authenticationHash) {
            this.authenticationHash = authenticationHash;
            return this;
        }

        public Builder withVerificationCode(String verificationCode) {
            this.verificationCode = verificationCode;
            return this;
        }

        public Builder withUserRequest(UserRequest userRequest) {
            this.userRequest = userRequest;
            return this;
        }

        public Builder withSemanticsIdentifier(SemanticsIdentifier semanticsIdentifier) {
            this.semanticsIdentifier = semanticsIdentifier;
            return this;
        }

        public AuthenticationSessionInfo build() {
            return new AuthenticationSessionInfo(this);
        }

    }

}
