package com.aiagent.security.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NoXssValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoXss {
    String message() default "Input contains potentially malicious content";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
