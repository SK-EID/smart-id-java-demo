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
import ee.sk.siddemo.model.UserRequest;
import ee.sk.siddemo.services.SmartIdNotificationBasedCertificateChoiceService;
import ee.sk.siddemo.services.SmartIdSessionsStatusService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class SmartIdNotificationBasedCertificateChoiceController {

    private final Logger logger = LoggerFactory.getLogger(SmartIdNotificationBasedCertificateChoiceController.class);

    private final SmartIdNotificationBasedCertificateChoiceService smartIdNotificationBasedCertificateChoiceService;
    private final SmartIdSessionsStatusService smartIdSessionsStatusService;

    public SmartIdNotificationBasedCertificateChoiceController(SmartIdNotificationBasedCertificateChoiceService smartIdNotificationBasedCertificateChoiceService,
                                                               SmartIdSessionsStatusService smartIdSessionsStatusService) {
        this.smartIdNotificationBasedCertificateChoiceService = smartIdNotificationBasedCertificateChoiceService;
        this.smartIdSessionsStatusService = smartIdSessionsStatusService;
    }

    @PostMapping(value = "/notification-based/start-certificate-choice-with-person-code")
    public ModelAndView startNotificationCertificateChoiceWithPersonCode(ModelMap model,
                                                                         HttpSession session,
                                                                         @ModelAttribute("userRequest") @Valid UserRequest userRequest,
                                                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return new ModelAndView("main", "userRequest", userRequest);
        }
        smartIdNotificationBasedCertificateChoiceService.startCertificateChoice(session, userRequest);
        model.addAttribute("activeTab", "rp-api-v3");
        return new ModelAndView("notification-based/certificate-choice", model);
    }

    @GetMapping(value = "/notification-based/check-certificate-choice-status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> checkCertificateChoiceStatus(HttpSession session) {
        boolean checkCompleted;
        try {
            checkCompleted = smartIdNotificationBasedCertificateChoiceService.checkCertificateChoiceStatus(session);
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

    @GetMapping(value = "/notification-based/certificate-choice-result")
    public ModelAndView toCertificateChoiceResult(ModelMap model, HttpSession session) {
        String distinguishedName = (String) session.getAttribute("distinguishedName");
        if (distinguishedName == null) {
            return new ModelAndView("main", model);
        }
        model.addAttribute("distinguishedName", distinguishedName);
        model.addAttribute("activeTab", "rp-api-v3");
        return new ModelAndView("notification-based/certificate-choice-result", model);
    }
}
