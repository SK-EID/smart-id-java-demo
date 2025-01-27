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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.DynamicContent;
import ee.sk.siddemo.services.DynamicContentService;
import ee.sk.siddemo.services.SmartIdV3AuthenticationService;
import ee.sk.smartid.v3.SessionType;
import jakarta.servlet.http.HttpSession;

@RestController
public class SmartIdV3AuthenticationStatusController {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV3AuthenticationStatusController.class);

    private final DynamicContentService dynamicContentService;
    private final SmartIdV3AuthenticationService smartIdV3AuthenticationService;

    public SmartIdV3AuthenticationStatusController(DynamicContentService dynamicContentService,
                                                   SmartIdV3AuthenticationService smartIdV3AuthenticationService) {
        this.dynamicContentService = dynamicContentService;
        this.smartIdV3AuthenticationService = smartIdV3AuthenticationService;
    }

    @GetMapping(value = "/v3/check-authentication-status")
    public ResponseEntity<Map<String, String>> checkAuthenticationStatus(HttpSession session) {
        boolean checkCompleted;
        try {
            checkCompleted = smartIdV3AuthenticationService.checkAuthenticationStatus(session);
        } catch (SidOperationException ex) {
            logger.error("Error occurred while checking authentication status", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("errorMessage", ex.getMessage()));
        }
        if (checkCompleted) {
            logger.debug("Session status: COMPLETED");
            return ResponseEntity.ok(Map.of("sessionStatus", "COMPLETED"));
        }

        // Generate QR-code and dynamic link
        logger.debug("Generate dynamic content for session {}", session.getId());
        DynamicContent dynamicContent = dynamicContentService.getDynamicContent(session, SessionType.AUTHENTICATION);
        Map<String, String> content = new HashMap<>();
        content.put("dynamicLink", dynamicContent.getDynamicLink().toString());
        content.put("qrCode", dynamicContent.getQrCode());
        return ResponseEntity.ok(content);
    }

    @GetMapping(value = "/auth-session-error")
    public ModelAndView handleAuthenticationSessionErrors(@RequestParam(value = "errorMessage", required = false) String errorMessage,
                                                    ModelMap model) {
        model.addAttribute("errorMessage", errorMessage);
        return new ModelAndView("sidOperationError", model);
    }
}
