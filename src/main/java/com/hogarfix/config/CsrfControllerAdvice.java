package com.hogarfix.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CsrfControllerAdvice {

    @ModelAttribute("_csrf")
    public CsrfToken csrfToken(HttpServletRequest request) {
        Object token = request.getAttribute("_csrf");
        if (token instanceof CsrfToken) {
            return (CsrfToken) token;
        }
        return null;
    }
}
