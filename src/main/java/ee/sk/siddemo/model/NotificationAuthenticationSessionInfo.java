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

import ee.sk.smartid.rest.dao.NotificationAuthenticationSessionRequest;
import ee.sk.smartid.rest.dao.SessionStatus;

public class NotificationAuthenticationSessionInfo {

    private final String sessionId;
    private final NotificationAuthenticationSessionRequest authenticationSessionRequest;
    private SessionStatus sessionStatus;

    public NotificationAuthenticationSessionInfo(String sessionId, NotificationAuthenticationSessionRequest authenticationSessionRequest) {
        this.sessionId = sessionId;
        this.authenticationSessionRequest = authenticationSessionRequest;
    }

    public String getSessionId() {
        return sessionId;
    }

    public NotificationAuthenticationSessionRequest getAuthenticationSessionRequest() {
        return authenticationSessionRequest;
    }

    public void setSessionStatus(SessionStatus sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public SessionStatus getSessionStatus() {
        return sessionStatus;
    }
}
