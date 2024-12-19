package ee.sk.siddemo.services;

/*-
 * #%L
 * Smart-ID sample Java client
 * %%
 * Copyright (C) 2018 - 2024 SK ID Solutions AS
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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ee.sk.smartid.exception.permanent.SmartIdClientException;
import ee.sk.smartid.v3.SmartIdClient;
import ee.sk.smartid.v3.rest.dao.DynamicLinkSessionResponse;
import ee.sk.smartid.v3.rest.dao.SessionStatus;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdV3SessionsStatusService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV3SessionsStatusService.class);

    private final Map<String, Future<?>> sessions = new ConcurrentHashMap<>();

    private final SmartIdClient smartIdClientV3;

    public SmartIdV3SessionsStatusService(SmartIdClient smartIdClientV3) {
        this.smartIdClientV3 = smartIdClientV3;
    }

    public void startPolling(HttpSession httpSession, DynamicLinkSessionResponse response) {
        Callable<SessionStatus> task = () -> initPolling(response.getSessionID());
        Future<?> future = Executors.newSingleThreadExecutor().submit(task);
        sessions.put(httpSession.getId(), future);
    }

    public Optional<SessionStatus> getSessionsStatus(String sessionId) {
        Future<?> session = sessions.get(sessionId);
        if (session != null && session.isDone()) {
            try {
                return Optional.of((SessionStatus) session.get());
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        }
        return Optional.empty();
    }

    public void cancelPolling(String sessionId) {
        Future<?> future = sessions.get(sessionId);
        if (future != null) {
            future.cancel(true);
            sessions.remove(sessionId);
        }
    }

    private SessionStatus initPolling(String sessionId) {
        try {
            return smartIdClientV3.getSessionsStatusPoller().fetchFinalSessionStatus(sessionId);
        } catch (SmartIdClientException ex) {
            logger.error("Error occurred while fetching session status", ex);
            throw new RuntimeException(ex);
        }
    }
}
