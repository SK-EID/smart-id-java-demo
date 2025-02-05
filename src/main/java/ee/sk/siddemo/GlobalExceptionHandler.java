package ee.sk.siddemo;

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
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import ee.sk.siddemo.exception.SidOperationException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SidOperationException.class)
    public ModelAndView handleSidOperationException(SidOperationException exception) {
        var model = new ModelMap();

        model.addAttribute("errorMessage", exception.getMessage());
        return new ModelAndView("sidOperationError", model);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleSmartIdException(Exception exception) {
        logger.warn("Generic error caught {}", exception.getMessage());

        var model = new ModelMap();
        model.addAttribute("errorMessage", exception.getMessage());
        return new ModelAndView("error", model);
    }

}
