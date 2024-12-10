package ee.sk.siddemo.controller;

/*-
 * #%L
 * Smart-ID sample Java client
 * %%
 * Copyright (C) 2018 - 2019 SK ID Solutions AS
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import ee.sk.siddemo.exception.FileUploadException;
import ee.sk.siddemo.model.AuthenticationSessionInfo;
import ee.sk.siddemo.model.SigningResult;
import ee.sk.siddemo.model.SigningSessionInfo;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.siddemo.model.UserSidSession;
import ee.sk.siddemo.services.SmartIdV2AuthenticationService;
import ee.sk.siddemo.services.SmartIdV2SignatureService;
import ee.sk.smartid.AuthenticationIdentity;
import jakarta.validation.Valid;

@RestController
public class SmartIdV2Controller {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV2Controller.class);

    private final SmartIdV2SignatureService signatureService;
    private final SmartIdV2AuthenticationService authenticationService;
    private final UserSidSession userSidSession;

    @Autowired
    public SmartIdV2Controller(SmartIdV2SignatureService signatureService, SmartIdV2AuthenticationService authenticationService, UserSidSession userSidSession) {
        this.signatureService = signatureService;
        this.authenticationService = authenticationService;
        this.userSidSession = userSidSession; // session scope, autowired
    }

    @GetMapping(value = "/rp-api-v2")
    public ModelAndView userRequestForm(Model model) {
        model.addAttribute("activeTab", "rp-api-v2");
        return new ModelAndView("v2/main", "userRequest", new UserRequest());
    }

    @PostMapping(value = "/v2/signatureRequest")
    public ModelAndView sendSignatureRequest(@ModelAttribute("userRequest") UserRequest userRequest,
                                             BindingResult bindingResult, ModelMap model) {

        if (userRequest.getFile() == null || userRequest.getFile().getOriginalFilename() == null || userRequest.getFile().isEmpty()) {
            bindingResult.rejectValue("file", "error.file", "Please select a file to upload");
        }

        if (bindingResult.hasErrors()) {
            return new ModelAndView("index", "userRequest", userRequest);
        }

        SigningSessionInfo signingSessionInfo = signatureService.sendSignatureRequest(userRequest);

        userSidSession.setSigningSessionInfo(signingSessionInfo);

        model.addAttribute("signingSessionInfo", signingSessionInfo);
        model.addAttribute("activeTab", "rp-api-v2");

        return new ModelAndView("v2/signature", model);
    }

    @PostMapping(value = "/v2/sign")
    public ModelAndView sign(ModelMap model) {

        SigningResult signingResult = signatureService.sign(userSidSession.getSigningSessionInfo());

        userSidSession.clearSigningSession();

        model.addAttribute("signingResult", signingResult);
        model.addAttribute("activeTab", "rp-api-v2");

        return new ModelAndView("v2/signingResult", model);
    }

    @PostMapping(value = "/v2/authenticationRequest")
    public ModelAndView sendAuthenticationRequest(@ModelAttribute("userRequest") @Valid UserRequest userRequest,
                                                  BindingResult bindingResult, ModelMap model) {

        if (bindingResult.hasErrors()) {
            System.out.println("Input validation error");
            return new ModelAndView("index", "userRequest", userRequest);
        }

        AuthenticationSessionInfo authenticationSessionInfo = authenticationService.startAuthentication(userRequest);
        userSidSession.setAuthenticationSessionInfo(authenticationSessionInfo);

        model.addAttribute("verificationCode", authenticationSessionInfo.getVerificationCode());
        model.addAttribute("activeTab", "rp-api-v2");

        return new ModelAndView("v2/authentication", model);
    }

    @PostMapping(value = "/v2/authenticate")
    public ModelAndView authenticate(ModelMap model) {
        AuthenticationIdentity person = authenticationService.authenticate(userSidSession.getAuthenticationSessionInfo());
        model.addAttribute("person", person);

        userSidSession.clearAuthenticationSessionInfo();
        model.addAttribute("activeTab", "rp-api-v2");
        return new ModelAndView("v2/authenticationResult", model);
    }

    @ExceptionHandler(FileUploadException.class)
    public ModelAndView handleFileUploadException(FileUploadException exception) {
        var model = new ModelMap();

        model.addAttribute("errorMessage", "File upload error");
        return new ModelAndView("sidOperationError", model);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleSmartIdException(Exception exception) {
        logger.warn("Generic error caught", exception);

        var model = new ModelMap();

        model.addAttribute("errorMessage", exception.getMessage());

        return new ModelAndView("error", model);
    }
}
