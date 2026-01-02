package com.aiagent.security;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

@Component
public class XssSanitizer {

    private static final PolicyFactory BASIC_POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.TABLES);

    private static final PolicyFactory STRICT_POLICY = new HtmlPolicyBuilder().toFactory();

    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return BASIC_POLICY.sanitize(input);
    }

    public String sanitizeStrict(String input) {
        if (input == null) {
            return null;
        }
        return STRICT_POLICY.sanitize(input);
    }

    public String stripAllHtml(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<[^>]*>", "")
                .replaceAll("&[^;]+;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    public boolean containsXssPatterns(String input) {
        if (input == null) {
            return false;
        }
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("<script") ||
                lowerInput.contains("javascript:") ||
                lowerInput.contains("onerror=") ||
                lowerInput.contains("onload=") ||
                lowerInput.contains("onclick=") ||
                lowerInput.contains("onmouseover=") ||
                lowerInput.contains("eval(") ||
                lowerInput.contains("expression(");
    }
}
