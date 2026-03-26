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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.siddemo.services.DynamicContentService;
import ee.sk.siddemo.services.SmartIdDeviceLinkSignatureService;
import ee.sk.smartid.SessionType;
import jakarta.servlet.http.HttpSession;

@Controller
public class SmartIdDeviceLinkSignatureController {

    private final Logger logger = LoggerFactory.getLogger(SmartIdDeviceLinkSignatureController.class);

    private final SmartIdDeviceLinkSignatureService smartIdDeviceLinkSignatureService;
    private final DynamicContentService dynamicContentService;

    public SmartIdDeviceLinkSignatureController(SmartIdDeviceLinkSignatureService smartIdDeviceLinkSignatureService,
                                                DynamicContentService dynamicContentService) {
        this.smartIdDeviceLinkSignatureService = smartIdDeviceLinkSignatureService;
        this.dynamicContentService = dynamicContentService;
    }

    @PostMapping(value = "device-link/start-signing-with-document-number")
    public ModelAndView sendDeviceLinkSigningRequestWithDocumentNumber(@ModelAttribute("userDocumentNumberRequest") UserDocumentNumberRequest userDocumentNumberRequest,
                                                                       BindingResult bindingResult,
                                                                       ModelMap model,
                                                                       RedirectAttributes redirectAttributes,
                                                                       HttpSession session) {
        model.addAttribute("activeTab", "rp-api-v3");
        if (isFileMissing(userDocumentNumberRequest.getFile())) {
            bindingResult.rejectValue("file", "error.file", "Please select a file to upload");
        }

        if (bindingResult.hasErrors()) {
            logger.debug("Validation errors: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userDocumentNumberRequest", bindingResult);
            redirectAttributes.addFlashAttribute("userDocumentNumberRequest", userDocumentNumberRequest);
            return new ModelAndView("redirect:/rp-api-v3");
        }
        smartIdDeviceLinkSignatureService.startSigningWithDocumentNumber(session, userDocumentNumberRequest);
        return new ModelAndView("device-link/signing", model);
    }

    @PostMapping(value = "device-link/start-signing-with-person-code")
    public ModelAndView sendDeviceLinkSigningRequestWithPersonCode(@ModelAttribute("userRequest") UserRequest userRequest,
                                                                   @RequestParam("documentNumber") String documentNumber,
                                                                   BindingResult bindingResult,
                                                                   ModelMap model,
                                                                   RedirectAttributes redirectAttributes,
                                                                   HttpSession session) {
        model.addAttribute("activeTab", "rp-api-v3");
        if (isFileMissing(userRequest.getFile())) {
            bindingResult.rejectValue("file", "error.file", "Please select a file to upload");
        }

        if (bindingResult.hasErrors()) {
            logger.debug("Validation errors: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userRequest", bindingResult);
            redirectAttributes.addFlashAttribute("userRequest", userRequest);
            return new ModelAndView("redirect:/rp-api-v3");
        }
        session.setAttribute("documentNumber", documentNumber);

        smartIdDeviceLinkSignatureService.startSigningWithPersonCode(session, userRequest);
        return new ModelAndView("device-link/signing", model);
    }

    @GetMapping(value = "device-link/check-signing-status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> checkSigningStatus(HttpSession session) {
        boolean checkCompleted;
        try {
            checkCompleted = smartIdDeviceLinkSignatureService.checkSignatureStatus(session);
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

    @GetMapping(value = "/device-link/signature/qr-code")
    public ResponseEntity<String> getAuthenticationQrCode(HttpSession session) {
        return ResponseEntity.ok(dynamicContentService.getQrCode(session, SessionType.SIGNATURE));
    }

    @GetMapping(value = "/device-link/signature/url")
    public ResponseEntity<String> getAuthenticationDeviceLink(HttpSession session) {
        return ResponseEntity.ok(dynamicContentService.getDeviceLink(session, SessionType.SIGNATURE));
    }

    private static boolean isFileMissing(MultipartFile file) {
        return file == null || file.getOriginalFilename() == null || file.isEmpty();
    }
}
