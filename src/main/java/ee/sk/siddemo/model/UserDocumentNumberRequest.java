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

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

public class UserDocumentNumberRequest {

    @NotNull
    private String documentNumber;
    private MultipartFile file;
    private String signingSignatureAlgorithm = "rsassa-pss";
    private UserActionMock userActionMock = UserActionMock.NONE;

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public UserActionMock getUserActionMock() {
        return userActionMock;
    }

    public void setUserActionMock(UserActionMock userActionMock) {
        this.userActionMock = userActionMock;
    }

    public String getSigningSignatureAlgorithm() {
        return signingSignatureAlgorithm;
    }

    public void setSigningSignatureAlgorithm(String signingSignatureAlgorithm) {
        this.signingSignatureAlgorithm = signingSignatureAlgorithm;
    }
}
