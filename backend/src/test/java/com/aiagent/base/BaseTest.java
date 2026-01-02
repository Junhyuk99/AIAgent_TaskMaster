package com.aiagent.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for all tests providing common functionality.
 */
@ActiveProfiles("test")
public abstract class BaseTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @BeforeEach
    void logTestStart(TestInfo testInfo) {
        log.debug("Starting test: {}", testInfo.getDisplayName());
    }
}
