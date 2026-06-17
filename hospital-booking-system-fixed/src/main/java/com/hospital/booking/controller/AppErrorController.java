package com.hospital.booking.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Custom error controller — handles all errors routed to /error by Spring Boot.
 *
 * Named AppErrorController (not ErrorController) to avoid a compile-time
 * "already defined in this compilation unit" error: Java sees the class name
 * ErrorController and the implemented interface
 * org.springframework.boot.web.servlet.error.ErrorController as the same
 * simple name in the same compilation unit, which is illegal.
 */
@Controller
@Slf4j
public class AppErrorController
        implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusObj  = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object messageObj = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object pathObj    = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        int status = (statusObj != null) ? Integer.parseInt(statusObj.toString()) : 500;
        String message = (messageObj != null && !messageObj.toString().isBlank())
            ? messageObj.toString()
            : defaultMessage(status);
        String path = (pathObj != null) ? pathObj.toString() : "";

        model.addAttribute("status",  status);
        model.addAttribute("error",   HttpStatus.resolve(status) != null
            ? HttpStatus.resolve(status).getReasonPhrase() : "Error");
        model.addAttribute("message", message);
        model.addAttribute("path",    path);

        log.warn("Error page rendered: {} {} -> {}", status, path, message);
        return "error";
    }

    private String defaultMessage(int status) {
        return switch (status) {
            case 400 -> "The request was invalid. Please check your input.";
            case 401 -> "You need to log in to access this page.";
            case 403 -> "You don't have permission to access this page.";
            case 404 -> "The page you are looking for doesn't exist or has been moved.";
            case 405 -> "The HTTP method is not allowed for this endpoint.";
            case 408 -> "The request timed out. Please try again.";
            case 500 -> "Something went wrong on our end. Please try again in a moment.";
            case 502 -> "Bad gateway. The upstream server returned an invalid response.";
            case 503 -> "The service is temporarily unavailable. Please try again shortly.";
            default  -> "An unexpected error occurred.";
        };
    }
}
