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
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.DynamicContent;
import ee.sk.siddemo.services.DynamicContentService;
import ee.sk.siddemo.services.SmartIdV3DynamicLinkCertificateChoiceService;
import ee.sk.siddemo.services.SmartIdV3SessionsStatusService;
import ee.sk.smartid.v3.SessionType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class SmartIdV3DynamicLinkCertificateChoiceController {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV3DynamicLinkCertificateChoiceController.class);

    private final SmartIdV3DynamicLinkCertificateChoiceService smartIdV3DynamicLinkCertificateChoiceService;
    private final SmartIdV3SessionsStatusService smartIdV3SessionsStatusService;
    private final DynamicContentService dynamicContentService;

    public SmartIdV3DynamicLinkCertificateChoiceController(SmartIdV3DynamicLinkCertificateChoiceService smartIdV3DynamicLinkCertificateChoiceService,
                                                           SmartIdV3SessionsStatusService smartIdV3SessionsStatusService,
                                                           DynamicContentService dynamicContentService) {
        this.smartIdV3DynamicLinkCertificateChoiceService = smartIdV3DynamicLinkCertificateChoiceService;
        this.smartIdV3SessionsStatusService = smartIdV3SessionsStatusService;
        this.dynamicContentService = dynamicContentService;
    }

    @GetMapping(value = "/v3/dynamic-link/start-certificate-choice")
    public ModelAndView startDynamicCertificateChoice(ModelMap model, HttpServletRequest request) {
        HttpSession session = resetSession(request);
        smartIdV3DynamicLinkCertificateChoiceService.startCertificateChoice(session);
        model.addAttribute("activeTab", "rp-api-v3");
        return new ModelAndView("v3/dynamic-link/certificate-choice", model);
    }

    @GetMapping(value = "/v3/dynamic-link/check-certificate-choice-status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> checkCertificateChoiceStatus(HttpSession session) {
        boolean checkCompleted;
        try {
            checkCompleted = smartIdV3DynamicLinkCertificateChoiceService.checkCertificateChoiceStatus(session);
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
        DynamicContent dynamicContent = dynamicContentService.getDynamicContent(session, SessionType.CERTIFICATE_CHOICE);
        Map<String, String> content = new HashMap<>();
        content.put("dynamicLink", dynamicContent.getDynamicLink().toString());
        content.put("qrCode", dynamicContent.getQrCode());
        return ResponseEntity.ok(content);
    }

    @GetMapping(value = "/v3/dynamic-link/certificate-choice-session-error")
    public ModelAndView handleCertificateChoiceSessionsError(@RequestParam(value = "errorMessage", required = false) String errorMessage,
                                                             ModelMap model) {
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("activeTab", "rp-api-v3");
        return new ModelAndView("sidOperationError", model);
    }

    @GetMapping(value = "/v3/dynamic-link/certificate-choice-result")
    public ModelAndView toCertificateChoiceResult(ModelMap model, HttpSession session) {
        String documentNumber = (String) session.getAttribute("documentNumber");
        String distinguishedName = (String) session.getAttribute("distinguishedName");
        model.addAttribute("documentNumber", documentNumber);
        model.addAttribute("distinguishedName", distinguishedName);
        model.addAttribute("activeTab", "rp-api-v3");
        return new ModelAndView("v3/dynamic-link/certificate-choice-result", model);
    }

    @GetMapping(value = "/v3/dynamic-link/cancel-certificate-choice")
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
