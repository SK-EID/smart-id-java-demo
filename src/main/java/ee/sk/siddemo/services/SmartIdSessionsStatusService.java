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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.smartid.exception.permanent.SmartIdClientException;
import ee.sk.smartid.SmartIdClient;
import ee.sk.smartid.rest.dao.SessionStatus;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdSessionsStatusService {

    private final Map<String, Future<?>> sessions = new ConcurrentHashMap<>();

    private final SmartIdClient smartIdClient;

    public SmartIdSessionsStatusService(SmartIdClient smartIdClient) {
        this.smartIdClient = smartIdClient;
    }

    public void startPolling(HttpSession httpSession, String rpApiSessionId) {
        Callable<SessionStatus> task = () -> poll(rpApiSessionId);
        Future<?> future = Executors.newSingleThreadExecutor().submit(task);
        sessions.put(httpSession.getId(), future);
    }

    public Optional<SessionStatus> getSessionsStatus(String userSessionId) {
        Future<?> session = sessions.get(userSessionId);
        if (session != null && session.isDone()) {
            try {
                return Optional.of((SessionStatus) session.get());
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        }
        return Optional.empty();
    }

    public void cancelPolling(String userSessionId) {
        Future<?> future = sessions.get(userSessionId);
        if (future != null) {
            future.cancel(true);
            sessions.remove(userSessionId);
        }
    }

    public SessionStatus poll(String rpApiSessionId) {
        try {
            return smartIdClient.getSessionStatusPoller().fetchFinalSessionStatus(rpApiSessionId);
        } catch (SmartIdClientException ex) {
            throw new SidOperationException("Error occurred while fetching session status", ex);
        }
    }
}
