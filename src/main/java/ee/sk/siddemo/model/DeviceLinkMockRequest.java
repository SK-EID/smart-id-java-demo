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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Object to send to Smart-ID Mock service to simulate user-action in the Device Link flow.
 *
 * @param documentNumber     Required. The document number of the user.
 * @param deviceLink         Required. The device link URL generated for device link flow
 * @param flowType           Required. Supported values QR, Web2App and App2App
 * @param browserCookie      Required for Web2App and App2App flows. The browser cookie value for the session.
 * @param initialCallbackUrl Required for Web2App and App2App flows. The initial callback URL to which the user will be redirected.
 */
public record DeviceLinkMockRequest(String documentNumber,
                                    String deviceLink,
                                    String flowType,
                                    @JsonInclude(JsonInclude.Include.NON_EMPTY) String browserCookie,
                                    @JsonInclude(JsonInclude.Include.NON_EMPTY) String initialCallbackUrl) {
}
