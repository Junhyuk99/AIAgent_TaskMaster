package com.aiagent;

import com.aiagent.base.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Application Context Tests")
class AiAgentApplicationTests extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    @DisplayName("Essential beans are present")
    void essentialBeansArePresent() {
        assertThat(applicationContext.containsBean("securityFilterChain")).isTrue();
        assertThat(applicationContext.containsBean("passwordEncoder")).isTrue();
    }
}
