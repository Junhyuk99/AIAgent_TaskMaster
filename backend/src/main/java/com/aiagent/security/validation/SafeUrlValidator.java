package com.aiagent.security.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

public class SafeUrlValidator implements ConstraintValidator<SafeUrl, String> {

    private static final Set<String> ALLOWED_PROTOCOLS = Set.of("http", "https");
    private static final Set<String> DANGEROUS_HOSTS = Set.of(
            "169.254.169.254",  // AWS metadata
            "metadata.google.internal",  // GCP metadata
            "100.100.100.200"  // Azure metadata
    );

    private boolean allowLocalhost;

    @Override
    public void initialize(SafeUrl constraintAnnotation) {
        this.allowLocalhost = constraintAnnotation.allowLocalhost();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        try {
            URL url = new URL(value);

            // Check protocol
            String protocol = url.getProtocol().toLowerCase();
            if (!ALLOWED_PROTOCOLS.contains(protocol)) {
                return false;
            }

            // Check for dangerous hosts (SSRF prevention)
            String host = url.getHost().toLowerCase();
            if (DANGEROUS_HOSTS.contains(host)) {
                return false;
            }

            // Check localhost if not allowed
            if (!allowLocalhost) {
                if (host.equals("localhost") ||
                    host.equals("127.0.0.1") ||
                    host.startsWith("192.168.") ||
                    host.startsWith("10.") ||
                    host.equals("::1")) {
                    return false;
                }
            }

            // Check for IP address in private ranges
            if (isPrivateIp(host) && !allowLocalhost) {
                return false;
            }

            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private boolean isPrivateIp(String host) {
        // Simple check for common private IP patterns
        return host.matches("^10\\..*") ||
                host.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*") ||
                host.matches("^192\\.168\\..*") ||
                host.matches("^127\\..*");
    }
}
