package ee.sk.siddemo;

import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import ee.sk.siddemo.exception.SidOperationException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SidOperationException.class)
    public ModelAndView handleSidOperationException(SidOperationException exception) {
        var model = new ModelMap();

        model.addAttribute("errorMessage", exception.getMessage());
        return new ModelAndView("sidOperationError", model);
    }
}
