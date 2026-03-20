package com.alberto.githubdashboard.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GithubApiException.class)
    public String handleGithubApiException(GithubApiException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "index";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        model.addAttribute("error", "An unexpected error occurred: " + ex.getMessage());
        return "index";
    }
}
