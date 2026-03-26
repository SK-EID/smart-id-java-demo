package ee.sk.siddemo.controller;

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

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ee.sk.siddemo.services.SessionStore;
import ee.sk.siddemo.services.SmartIdSessionsStatusService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class SmartIdSessionController {

    private final SmartIdSessionsStatusService smartIdSessionsStatusService;
    private final SessionStore sessionStore;

    public SmartIdSessionController(SmartIdSessionsStatusService smartIdSessionsStatusService,
                                    SessionStore sessionStore) {
        this.smartIdSessionsStatusService = smartIdSessionsStatusService;
        this.sessionStore = sessionStore;
    }

    @GetMapping(value = "/cancel-session")
    public ModelAndView cancelSession(ModelMap model, HttpServletRequest request) {
        resetSession(request);
        return new ModelAndView("redirect:/rp-api-v3", model);
    }

    @GetMapping(value = "/session-error")
    public ModelAndView handleSessionError(@RequestParam(value = "errorMessage", required = false) String errorMessage,
                                           HttpServletRequest request,
                                           ModelMap model) {
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("activeTab", "rp-api-v3");
        resetSession(request);
        return new ModelAndView("sidOperationError", model);
    }

    private void resetSession(HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (session != null) {
            smartIdSessionsStatusService.cancelPolling(session.getId());
            sessionStore.remove(session.getId());
            session.invalidate();
        }
        // Create a new session
        request.getSession(true);
    }
}
