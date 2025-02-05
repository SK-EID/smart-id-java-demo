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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.siddemo.services.SmartIdV3NotificationBasedCertificateChoiceService;
import ee.sk.siddemo.services.SmartIdV3SessionsStatusService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class SmartIdV3NotificationBasedCertificateChoiceController {

    private final Logger logger = LoggerFactory.getLogger(SmartIdV3NotificationBasedCertificateChoiceController.class);

    private final SmartIdV3NotificationBasedCertificateChoiceService smartIdV3NotificationBasedCertificateChoiceService;
    private final SmartIdV3SessionsStatusService smartIdV3SessionsStatusService;

    public SmartIdV3NotificationBasedCertificateChoiceController(SmartIdV3NotificationBasedCertificateChoiceService smartIdV3NotificationBasedCertificateChoiceService,
                                                                 SmartIdV3SessionsStatusService smartIdV3SessionsStatusService) {
        this.smartIdV3NotificationBasedCertificateChoiceService = smartIdV3NotificationBasedCertificateChoiceService;
        this.smartIdV3SessionsStatusService = smartIdV3SessionsStatusService;
    }

    @PostMapping(value = "/v3/notification-based/start-certificate-choice-with-person-code")
    public ModelAndView startNotificationCertificateChoiceWithPersonCode(ModelMap model,
                                                                         HttpServletRequest request,
                                                                         @ModelAttribute("userRequest") @Valid UserRequest userRequest,
                                                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return new ModelAndView("v3/main", "userRequest", userRequest);
        }
        HttpSession session = resetSession(request);
        smartIdV3NotificationBasedCertificateChoiceService.startCertificateChoice(session, userRequest);
        model.addAttribute("activeTab", "rp-api-v3");
        return new ModelAndView("v3/notification-based/certificate-choice", model);
    }

    @PostMapping(value = "/v3/notification-based/start-certificate-choice-with-document-number")
    public ModelAndView startNotificationCertificateChoiceWithDocumentNumber(ModelMap model,
                                                                             HttpServletRequest request,
                                                                             @ModelAttribute("userDocumentNumberRequest") @Valid UserDocumentNumberRequest userDocumentNumberRequest,
                                                                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return new ModelAndView("v3/main", "userDocumentNumberRequest", userDocumentNumberRequest);
        }
        HttpSession session = resetSession(request);
        smartIdV3NotificationBasedCertificateChoiceService.startCertificateChoice(session, userDocumentNumberRequest);
        model.addAttribute("activeTab", "rp-api-v3");
        return new ModelAndView("v3/notification-based/certificate-choice", model);
    }

    @GetMapping(value = "/v3/notification-based/check-certificate-choice-status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> checkCertificateChoiceStatus(HttpSession session) {
        boolean checkCompleted;
        try {
            checkCompleted = smartIdV3NotificationBasedCertificateChoiceService.checkCertificateChoiceStatus(session);
        } catch (SidOperationException ex) {
            logger.error("Error occurred while checking authentication status", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("errorMessage", ex.getMessage()));
        }
        if (checkCompleted) {
            logger.debug("Session status: COMPLETED");
            return ResponseEntity.ok(Map.of("sessionStatus", "COMPLETED"));
        }
        return ResponseEntity.ok(Map.of("sessionStatus", "IN_PROGRESS"));
    }

    @GetMapping(value = "/v3/notification-based/certificate-choice-result")
    public ModelAndView toCertificateChoiceResult(ModelMap model, HttpSession session) {
        String distinguishedName = (String) session.getAttribute("distinguishedName");
        if (distinguishedName == null) {
            return new ModelAndView("v3/main", model);
        }
        model.addAttribute("distinguishedName", distinguishedName);
        model.addAttribute("activeTab", "rp-api-v3");
        return new ModelAndView("v3/notification-based/certificate-choice-result", model);
    }

    @GetMapping(value = "/v3/notification-based/cancel-certificate-choice")
    public ModelAndView cancelCertificateChoice(ModelMap model, HttpServletRequest request) {
        resetSession(request);
        model.addAttribute("activeTab", "rp-api-v3");
        return new ModelAndView("v3/main", model);
    }

    private HttpSession resetSession(HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (session != null) {
            smartIdV3SessionsStatusService.cancelPolling(session.getId());
            session.invalidate();
        }
        // Create a new session
        session = request.getSession(true);
        return session;
    }
}
