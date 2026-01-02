package com.aiagent.base;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for service unit tests using Mockito.
 * Services are tested in isolation with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseServiceTest extends BaseTest {
}
