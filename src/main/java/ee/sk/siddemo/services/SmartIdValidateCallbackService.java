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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.CallbackPayload;
import ee.sk.siddemo.model.DeviceLinkSessionInfo;
import ee.sk.smartid.exception.SmartIdException;
import ee.sk.smartid.util.CallbackUrlUtil;

@Service
public class SmartIdValidateCallbackService {

    private final Logger logger = LoggerFactory.getLogger(SmartIdValidateCallbackService.class);

    private final SessionStore sessionStore;

    public SmartIdValidateCallbackService(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    public void validate(CallbackPayload body) {

        List<String> sessionIds = sessionStore.getSessionInfo(body.asParams().get("value"));
        if (sessionIds.isEmpty()) {
            logger.error("Invalid callback request, session not found for url token: {}", body.asParams().get("value"));
            throw new SidOperationException("Session not found");
        }
        if (sessionIds.size() > 1) {
            logger.error("Invalid callback request, multiple sessions found for url token: {}", body.asParams().get("value"));
            throw new SidOperationException("Multiple sessions found");
        }

        DeviceLinkSessionInfo sessionInfo = (DeviceLinkSessionInfo) sessionStore.get(sessionIds.get(0), "deviceLinkSessionInfo");
        String urlSessionSecret = body.asParams().get("sessionSecretDigest");

        try {
            CallbackUrlUtil.validateSessionSecretDigest(urlSessionSecret, sessionInfo.getSessionSecret());
        } catch (SmartIdException ex) {
            throw new SidOperationException("Invalid callback request", ex);
        }
        String userChallengeVerifier = body.asParams().get("userChallengeVerifier");
        if (userChallengeVerifier != null) {
            sessionInfo.setUserChallengeVerifier(userChallengeVerifier);
        }
        logger.debug("Callback verified for session {}", sessionIds.get(0));
    }
}
