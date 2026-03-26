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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.CallbackPayload;
import ee.sk.siddemo.services.SmartIdValidateCallbackService;

@RestController
public class SmartIdValidateCallbackController {

    private final Logger logger = LoggerFactory.getLogger(SmartIdValidateCallbackController.class);

    private final SmartIdValidateCallbackService smartIdValidateCallbackService;

    public SmartIdValidateCallbackController(SmartIdValidateCallbackService smartIdValidateCallbackService) {
        this.smartIdValidateCallbackService = smartIdValidateCallbackService;
    }

    @PostMapping(value = "/callback/validate")
    public ResponseEntity<Void> callback(@RequestBody CallbackPayload body) {
        logger.info("Received callback to validate: {}", body);
        try {
            smartIdValidateCallbackService.validate(body);
        } catch (SidOperationException ex){
            logger.error("Callback validation failed: {}", ex.getMessage());
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
}
