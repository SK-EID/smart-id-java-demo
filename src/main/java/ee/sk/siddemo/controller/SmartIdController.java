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

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import ee.sk.siddemo.model.AnonymousRequest;
import ee.sk.siddemo.model.LinkedSigningRequest;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;

@Controller
public class SmartIdController {

    @ModelAttribute("anonymousRequest")
    public AnonymousRequest anonymousRequest() {
        return new AnonymousRequest();
    }

    @ModelAttribute("userRequest")
    public UserRequest userRequest() {
        return new UserRequest();
    }

    @ModelAttribute("userDocumentNumberRequest")
    public UserDocumentNumberRequest userDocumentNumberRequest() {
        return new UserDocumentNumberRequest();
    }

    @ModelAttribute("linkedSigningRequest")
    public LinkedSigningRequest linkedSigningRequest() {
        return new LinkedSigningRequest();
    }

    @GetMapping(value = "/rp-api-v3")
    public String viewRpApiV3Tab(Model model) {
        model.addAttribute("activeTab", "rp-api-v3");
        return "main";
    }
}
