package com.aiagent.base;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for repository tests using H2 in-memory database.
 * Uses @DataJpaTest for slice testing of JPA repositories.
 */
@DataJpaTest
@ActiveProfiles("test")
public abstract class BaseRepositoryTest extends BaseTest {
}
