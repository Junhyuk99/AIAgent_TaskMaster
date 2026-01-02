package com.aiagent.security.validation;

import com.aiagent.security.XssSanitizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NoXssValidator implements ConstraintValidator<NoXss, String> {

    private static XssSanitizer xssSanitizer;

    @Autowired
    public void setXssSanitizer(XssSanitizer sanitizer) {
        NoXssValidator.xssSanitizer = sanitizer;
    }

    @Override
    public void initialize(NoXss constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        if (xssSanitizer != null) {
            return !xssSanitizer.containsXssPatterns(value);
        }

        // Fallback check if sanitizer is not injected
        String lowerValue = value.toLowerCase();
        return !lowerValue.contains("<script") &&
                !lowerValue.contains("javascript:") &&
                !lowerValue.contains("onerror=") &&
                !lowerValue.contains("onload=");
    }
}
