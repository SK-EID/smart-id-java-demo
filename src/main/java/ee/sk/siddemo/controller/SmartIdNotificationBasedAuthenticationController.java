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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.siddemo.services.SmartIdNotificationBasedAuthenticationService;
import jakarta.servlet.http.HttpSession;

@Controller
public class SmartIdNotificationBasedAuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdNotificationBasedAuthenticationController.class);

    private final SmartIdNotificationBasedAuthenticationService smartIdNotificationBasedAuthenticationService;

    public SmartIdNotificationBasedAuthenticationController(SmartIdNotificationBasedAuthenticationService smartIdNotificationBasedAuthenticationService) {
        this.smartIdNotificationBasedAuthenticationService = smartIdNotificationBasedAuthenticationService;
    }

    @PostMapping("notification-based/start-authentication-with-person-code")
    public ModelAndView startAuthenticationWithPersonCode(@ModelAttribute("userRequest") UserRequest userRequest,
                                                          BindingResult bindingResult,
                                                          ModelMap model,
                                                          RedirectAttributes redirectAttributes,
                                                          HttpSession session) {
        model.addAttribute("activeTab", "rp-api-v3");
        if (bindingResult.hasErrors()) {
            logger.debug("Validation errors: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userRequest", bindingResult);
            redirectAttributes.addFlashAttribute("userRequest", userRequest);
            return new ModelAndView("redirect:/rp-api-v3");
        }

        String verificationCode = smartIdNotificationBasedAuthenticationService.startAuthenticationWithPersonCode(session, userRequest);
        model.addAttribute("verificationCode", verificationCode);
        return new ModelAndView("notification-based/authentication", model);
    }

    @PostMapping("notification-based/start-authentication-with-document-number")
    public ModelAndView startAuthenticationWithDocumentNumber(@ModelAttribute("userDocumentNumberRequest") UserDocumentNumberRequest userDocumentNumberRequest,
                                                              BindingResult bindingResult,
                                                              ModelMap model,
                                                              RedirectAttributes redirectAttributes,
                                                              HttpSession session) {
        model.addAttribute("activeTab", "rp-api-v3");
        if (bindingResult.hasErrors()) {
            logger.debug("Validation errors: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userDocumentNumberRequest", bindingResult);
            redirectAttributes.addFlashAttribute("userDocumentNumberRequest", userDocumentNumberRequest);
            return new ModelAndView("redirect:/rp-api-v3");
        }

        String verificationCode = smartIdNotificationBasedAuthenticationService.startAuthenticationWithDocumentNumber(session, userDocumentNumberRequest);
        model.addAttribute("verificationCode", verificationCode);
        return new ModelAndView("notification-based/authentication", model);
    }

    @GetMapping(value = "notification-based/check-authentication-status")
    @ResponseBody
    public ResponseEntity<?> checkNotificationSigningStatus(HttpSession session) {
        try {
            smartIdNotificationBasedAuthenticationService.checkAuthenticationStatus(session);
            return ResponseEntity.ok().build();
        } catch (SidOperationException ex) {
            logger.error("Error occurred while checking authentication status", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }
}
