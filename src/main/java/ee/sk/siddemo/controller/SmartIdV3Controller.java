package ee.sk.siddemo.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import ee.sk.siddemo.model.DynamicContent;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.siddemo.services.DynamicContentService;
import ee.sk.siddemo.services.SessionStatusStore;
import ee.sk.siddemo.services.SmartIdV3AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class SmartIdV3Controller {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV3Controller.class);

    private final SmartIdV3AuthenticationService smartIdV3AuthenticationService;
    private final DynamicContentService dynamicContentService;
    private final SessionStatusStore sessionStatusStore;

    public SmartIdV3Controller(SmartIdV3AuthenticationService smartIdV3AuthenticationService,
                               DynamicContentService dynamicContentService,
                               SessionStatusStore sessionStatusStore) {
        this.smartIdV3AuthenticationService = smartIdV3AuthenticationService;
        this.dynamicContentService = dynamicContentService;
        this.sessionStatusStore = sessionStatusStore;
    }

    @ModelAttribute("userRequest")
    public UserRequest userRequest() {
        return new UserRequest();
    }

    @ModelAttribute("userDocumentNumberRequest")
    public UserDocumentNumberRequest userDocumentNumberRequest() {
        return new UserDocumentNumberRequest();
    }

    @GetMapping(value = "/rp-api-v3")
    public String userRequestForm(Model model) {
        model.addAttribute("activeTab", "rp-api-v3");
        return "v3/main";
    }

    @GetMapping(value = "/v3/start-authentication")
    public ModelAndView startAuthentication(ModelMap model, HttpServletRequest request) {
        HttpSession session = resetSession(request);
        smartIdV3AuthenticationService.startAuthentication(session);
        model.addAttribute("activeTab", "rp-api-v3");
        return new ModelAndView("v3/authentication", model);
    }

    @PostMapping(value = "/v3/start-authentication-with-person-code")
    public ModelAndView startAuthentication(ModelMap model, HttpServletRequest request,
                                            @ModelAttribute("userRequest") @Valid UserRequest userRequest,
                                            BindingResult bindingResult) {
        model.addAttribute("activeTab", "rp-api-v3");
        if (bindingResult.hasErrors()) {
            return new ModelAndView("v3/main", "userRequest", userRequest);
        }
        HttpSession session = resetSession(request);
        smartIdV3AuthenticationService.startAuthentication(session, userRequest);
        return new ModelAndView("v3/authentication", model);
    }

    @PostMapping(value = "/v3/start-authentication-with-document-number")
    public ModelAndView startAuthentication(ModelMap model, HttpServletRequest request,
                                            @ModelAttribute("userDocumentNumberRequest") @Valid UserDocumentNumberRequest userDocumentNumberRequest,
                                            BindingResult bindingResult) {
        model.addAttribute("activeTab", "rp-api-v3");
        if (bindingResult.hasErrors()) {
            return new ModelAndView("v3/main", "userDocumentNumberRequest", userDocumentNumberRequest);
        }
        HttpSession session = resetSession(request);
        smartIdV3AuthenticationService.startAuthentication(session, userDocumentNumberRequest);
        return new ModelAndView("v3/authentication", model);
    }

    @GetMapping(value = "/v3/check-authentication-status")
    @ResponseBody
    public Map<String, Object> checkAuthenticationStatus(HttpSession session) {
        boolean checkCompleted = smartIdV3AuthenticationService.checkAuthenticationStatus(session);
        if (checkCompleted) {
            logger.debug("Session status: COMPLETED");
            return Map.of("sessionStatus", "COMPLETED");
        }
        logger.debug("Update dynamic content");
        DynamicContent dynamicContent = dynamicContentService.getDynamicContent(session);
        return Map.of("dynamicLink", dynamicContent.getDynamicLink().toString(), "qrCode", dynamicContent.getQrCode());
    }

    @GetMapping(value = "/v3/authentication-result")
    public ModelAndView getAuthenticationResult(ModelMap model, HttpSession session) {
        smartIdV3AuthenticationService.authenticate(session);
        return new ModelAndView("v3/authentication-result", model);
    }

    @GetMapping(value = "/v3/cancel-authentication")
    public ModelAndView cancelAuthentication(ModelMap model, HttpServletRequest request) {
        resetSession(request);
        return new ModelAndView("v3/main", model);
    }

    private HttpSession resetSession(HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (session != null) {
            sessionStatusStore.removeSession(session.getId());
            session.invalidate();
        }
        // Create a new session
        session = request.getSession(true);
        return session;
    }
}
