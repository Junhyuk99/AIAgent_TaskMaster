package com.aiagent.security.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SafeUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeUrl {
    String message() default "Invalid or unsafe URL";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    boolean allowLocalhost() default true;
}
