package ee.sk.siddemo.controller;

import java.util.HashMap;
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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.DynamicContent;
import ee.sk.siddemo.model.SigningResult;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.siddemo.services.DynamicContentService;
import ee.sk.siddemo.services.SmartIdV3DynamicLinkSignatureService;
import ee.sk.siddemo.services.SmartIdV3SessionsStatusService;
import ee.sk.smartid.v3.SessionType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class SmartIdV3DynamicLinkSignatureController {

    private final Logger logger = LoggerFactory.getLogger(SmartIdV3DynamicLinkSignatureController.class);

    private final SmartIdV3DynamicLinkSignatureService smartIdV3DynamicLinkSignatureService;
    private final SmartIdV3SessionsStatusService smartIdV3SessionsStatusService;
    private final DynamicContentService dynamicContentService;

    public SmartIdV3DynamicLinkSignatureController(SmartIdV3DynamicLinkSignatureService smartIdV3DynamicLinkSignatureService,
                                                   SmartIdV3SessionsStatusService smartIdV3SessionsStatusService,
                                                   DynamicContentService dynamicContentService) {
        this.smartIdV3DynamicLinkSignatureService = smartIdV3DynamicLinkSignatureService;
        this.smartIdV3SessionsStatusService = smartIdV3SessionsStatusService;
        this.dynamicContentService = dynamicContentService;
    }

    @PostMapping(value = "v3/dynamic-link/start-signing-with-document-number")
    public ModelAndView sendDynamicLinkSigningRequestWithDocumentNumber(@ModelAttribute("userDocumentNumberRequest") UserDocumentNumberRequest userDocumentNumberRequest,
                                                                        BindingResult bindingResult,
                                                                        ModelMap model,
                                                                        RedirectAttributes redirectAttributes,
                                                                        HttpServletRequest request) {
        model.addAttribute("activeTab", "rp-api-v3");
        if (userDocumentNumberRequest.getFile() == null || userDocumentNumberRequest.getFile().getOriginalFilename() == null || userDocumentNumberRequest.getFile().isEmpty()) {
            bindingResult.rejectValue("file", "error.file", "Please select a file to upload");
        }

        if (bindingResult.hasErrors()) {
            logger.debug("Validation errors: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userDocumentNumberRequest", bindingResult);
            redirectAttributes.addFlashAttribute("userDocumentNumberRequest", userDocumentNumberRequest);
            return new ModelAndView("redirect:/rp-api-v3");
        }
        HttpSession session = resetSession(request);
        smartIdV3DynamicLinkSignatureService.startSigningWithDocumentNumber(session, userDocumentNumberRequest);
        return new ModelAndView("v3/dynamic-link/signing", model);
    }

    @PostMapping(value = "v3/dynamic-link/start-signing-with-person-code")
    public ModelAndView sendDynamicLinkSigningRequestWithPersonCode(@ModelAttribute("userRequest") UserRequest userRequest,
                                                                    BindingResult bindingResult,
                                                                    ModelMap model,
                                                                    RedirectAttributes redirectAttributes,
                                                                    HttpServletRequest request) {
        model.addAttribute("activeTab", "rp-api-v3");
        if (userRequest.getFile() == null || userRequest.getFile().getOriginalFilename() == null || userRequest.getFile().isEmpty()) {
            bindingResult.rejectValue("file", "error.file", "Please select a file to upload");
        }

        if (bindingResult.hasErrors()) {
            logger.debug("Validation errors: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userRequest", bindingResult);
            redirectAttributes.addFlashAttribute("userRequest", userRequest);
            return new ModelAndView("redirect:/rp-api-v3");
        }
        HttpSession session = resetSession(request);
        smartIdV3DynamicLinkSignatureService.startSigningWithPersonCode(session, userRequest);
        return new ModelAndView("v3/dynamic-link/signing", model);
    }

    @GetMapping(value = "v3/dynamic-link/check-signing-status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> checkSigningStatus(HttpSession session) {
        boolean checkCompleted;
        try {
            checkCompleted = smartIdV3DynamicLinkSignatureService.checkSignatureStatus(session);
        } catch (SidOperationException ex) {
            logger.error("Error occurred while checking authentication status", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("errorMessage", ex.getMessage()));
        }
        if (checkCompleted) {
            logger.debug("Session status: COMPLETED");
            return ResponseEntity.ok(Map.of("sessionStatus", "COMPLETED"));
        }

        logger.debug("Generate dynamic content for session {}", session.getId());
        DynamicContent dynamicContent = dynamicContentService.getDynamicContent(session, SessionType.SIGNATURE);
        Map<String, String> content = new HashMap<>();
        content.put("dynamicLink", dynamicContent.getDynamicLink().toString());
        content.put("qrCode", dynamicContent.getQrCode());
        return ResponseEntity.ok(content);
    }

    @GetMapping(value = "/v3/dynamic-link/sign-session-error")
    public ModelAndView handleSigningSessionError(@RequestParam(value = "errorMessage", required = false) String errorMessage,
                                                  ModelMap model) {
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("activeTab", "rp-api-v3");
        return new ModelAndView("sidOperationError", model);
    }

    @GetMapping(value = "/v3/dynamic-link/signing-result")
    public ModelAndView toSigningResult(ModelMap model, HttpSession session) {
        SigningResult signingResult = smartIdV3DynamicLinkSignatureService.handleSignatureResult(session);
        model.addAttribute("signingResult", signingResult);
        model.addAttribute("activeTab", "rp-api-v3");
        return new ModelAndView("v3/dynamic-link/signing-result", model);
    }

    @GetMapping(value = "/v3/dynamic-link/cancel-signing")
    public ModelAndView cancelSigning(ModelMap model, HttpServletRequest request) {
        resetSession(request);
        return new ModelAndView("redirect:/rp-api-v3", model);
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
