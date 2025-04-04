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

import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import ee.sk.siddemo.services.SmartIdV3AuthenticationService;
import ee.sk.smartid.AuthenticationIdentity;
import jakarta.servlet.http.HttpSession;

@RestController
public class SmartIdV3AuthenticationController {

    private final SmartIdV3AuthenticationService smartIdV3AuthenticationService;

    public SmartIdV3AuthenticationController(SmartIdV3AuthenticationService smartIdV3AuthenticationService) {
        this.smartIdV3AuthenticationService = smartIdV3AuthenticationService;
    }

    @GetMapping(value = "/v3/authentication-result")
    public ModelAndView getAuthenticationResult(ModelMap model, HttpSession session) {
        AuthenticationIdentity authenticationIdentity = smartIdV3AuthenticationService.authenticate(session);
        model.addAttribute("person", authenticationIdentity);
        return new ModelAndView("v3/authentication-result", model);
    }
}
