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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.AnonymousRequest;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.siddemo.services.DynamicContentService;
import ee.sk.siddemo.services.SmartIdDeviceLinkAuthenticationService;
import ee.sk.smartid.SessionType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;


@Controller
public class SmartIdDeviceLinkAuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdDeviceLinkAuthenticationController.class);

    private final SmartIdDeviceLinkAuthenticationService smartIdDeviceLinkAuthenticationService;
    private final DynamicContentService dynamicContentService;

    public SmartIdDeviceLinkAuthenticationController(SmartIdDeviceLinkAuthenticationService smartIdDeviceLinkAuthenticationService,
                                                     DynamicContentService dynamicContentService) {
        this.smartIdDeviceLinkAuthenticationService = smartIdDeviceLinkAuthenticationService;
        this.dynamicContentService = dynamicContentService;
    }

    @PostMapping(value = "/device-link/start-authentication")
    public ModelAndView startAuthentication(@ModelAttribute("anonymousRequest") @Valid AnonymousRequest anonymousRequest,
                                            ModelMap model,
                                            HttpSession session) {
        smartIdDeviceLinkAuthenticationService.startAuthentication(session, anonymousRequest);
        model.addAttribute("activeTab", "rp-api-v3");
        return new ModelAndView("device-link/authentication", model);
    }

    @PostMapping(value = "/device-link/start-authentication-with-person-code")
    public ModelAndView startAuthentication(@ModelAttribute("userRequest") @Valid UserRequest userRequest,
                                            ModelMap model,
                                            HttpSession session,
                                            BindingResult bindingResult) {
        model.addAttribute("activeTab", "rp-api-v3");
        if (bindingResult.hasErrors()) {
            return new ModelAndView("main", "userRequest", userRequest);
        }
        smartIdDeviceLinkAuthenticationService.startAuthentication(session, userRequest);
        return new ModelAndView("device-link/authentication", model);
    }

    @PostMapping(value = "/device-link/start-authentication-with-document-number")
    public ModelAndView startAuthentication(@ModelAttribute("userDocumentNumberRequest") @Valid UserDocumentNumberRequest userDocumentNumberRequest,
                                            ModelMap model,
                                            HttpSession session,
                                            BindingResult bindingResult) {
        model.addAttribute("activeTab", "rp-api-v3");
        if (bindingResult.hasErrors()) {
            return new ModelAndView("main", "userDocumentNumberRequest", userDocumentNumberRequest);
        }
        smartIdDeviceLinkAuthenticationService.startAuthentication(session, userDocumentNumberRequest);
        return new ModelAndView("device-link/authentication", model);
    }


    @GetMapping(value = "/device-link/check-authentication-status")
    public ResponseEntity<Map<String, String>> checkAuthenticationStatus(HttpSession session) {
        boolean checkCompleted;
        try {
            checkCompleted = smartIdDeviceLinkAuthenticationService.checkAuthenticationStatus(session);
        } catch (SidOperationException ex) {
            logger.error("Error occurred while checking authentication status", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("errorMessage", ex.getMessage()));
        }
        if (checkCompleted) {
            logger.debug("Session status: COMPLETED");
            return ResponseEntity.ok(Map.of("sessionStatus", "COMPLETED"));
        }
        return ResponseEntity.ok(Map.of("sessionStatus", "RUNNING"));
    }

    @GetMapping(value = "/device-link/authentication/qr-code")
    public ResponseEntity<String> getAuthenticationQrCode(HttpSession session) {
        return ResponseEntity.ok(dynamicContentService.getQrCode(session, SessionType.AUTHENTICATION));
    }

    @GetMapping(value = "/device-link/authentication/url")
    public ResponseEntity<String> getAuthenticationDeviceLink(HttpSession session, HttpServletRequest request) {
        return ResponseEntity.ok(dynamicContentService.getDeviceLink(session, SessionType.AUTHENTICATION, request));
    }
}
