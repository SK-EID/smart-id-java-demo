package ee.sk.siddemo;

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
        logger.warn("Generic error caught", exception);

        var model = new ModelMap();
        model.addAttribute("errorMessage", exception.getMessage());
        return new ModelAndView("error", model);
    }

}
