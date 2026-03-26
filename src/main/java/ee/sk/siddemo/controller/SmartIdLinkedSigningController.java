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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.LinkedSigningRequest;
import ee.sk.siddemo.services.DynamicContentService;
import ee.sk.siddemo.services.SmartIdLinkedSigningService;
import ee.sk.smartid.SessionType;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class SmartIdLinkedSigningController {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdDeviceLinkAuthenticationController.class);

    private final SmartIdLinkedSigningService smartIdLinkedSigningService;
    private final DynamicContentService dynamicContentService;

    public SmartIdLinkedSigningController(SmartIdLinkedSigningService smartIdLinkedSigningService,
                                          DynamicContentService dynamicContentService) {
        this.smartIdLinkedSigningService = smartIdLinkedSigningService;
        this.dynamicContentService = dynamicContentService;
    }

    @PostMapping(value = "/linked/start-signing")
    public ModelAndView startSigning(@ModelAttribute("linkedSigningRequest") @Valid LinkedSigningRequest linkedSigningRequest,
                                     ModelMap model,
                                     HttpSession session,
                                     BindingResult bindingResult) {
        model.addAttribute("activeTab", "rp-api-v3");
        if (isFileMissing(linkedSigningRequest.getFile())) {
            bindingResult.rejectValue("file", "error.file", "Please select a file to upload");
        }
        if (bindingResult.hasErrors()) {
            return new ModelAndView("main", "linkedSigningRequest", linkedSigningRequest);
        }
        smartIdLinkedSigningService.startSigning(session, linkedSigningRequest);
        return new ModelAndView("linked/certificate-choice", model);
    }

    @GetMapping(value = "/linked/check-certificate-choice-status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> checkCertificateChoiceStatus(HttpSession session) {
        boolean checkCompleted;
        try {
            checkCompleted = smartIdLinkedSigningService.checkCertificateChoiceStatus(session);
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

    @GetMapping(value = "/linked/cert-choice/qr-code")
    public ResponseEntity<String> getAuthenticationQrCode(HttpSession session) {
        return ResponseEntity.ok(dynamicContentService.getQrCode(session, SessionType.CERTIFICATE_CHOICE));
    }

    @GetMapping(value = "/linked/cert-choice/url")
    public ResponseEntity<String> getAuthenticationDeviceLink(HttpSession session) {
        return ResponseEntity.ok(dynamicContentService.getDeviceLink(session, SessionType.CERTIFICATE_CHOICE));
    }

    @GetMapping(value = "/linked/continue-signing")
    @ResponseBody
    public ResponseEntity checkSigningStatus(HttpSession session) {
        logger.info("Continuing signing for session {}", session.getId());
        smartIdLinkedSigningService.continueSigning(session);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping(value = "/linked/check-signature-status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> checkSignatureStatus(HttpSession session) {
        boolean checkCompleted;
        try {
            checkCompleted = smartIdLinkedSigningService.checkSignatureSessionStatus(session);
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

    @GetMapping(value = "/linked/signing")
    public ModelAndView continueSigning(ModelMap model) {
        logger.info("Redirecting to signing page");
        return new ModelAndView("linked/signing", model);
    }

    private static boolean isFileMissing(MultipartFile file) {
        return file == null || file.getOriginalFilename() == null || file.isEmpty();
    }
}
