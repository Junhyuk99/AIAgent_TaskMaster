package com.aiagent.base;

import com.aiagent.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for controller tests using MockMvc.
 * Uses @WebMvcTest for slice testing of web layer.
 */
@WebMvcTest
@ActiveProfiles("test")
public abstract class BaseControllerTest extends BaseTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected JwtTokenProvider jwtTokenProvider;

    @MockBean
    protected UserDetailsService userDetailsService;

    protected String asJsonString(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
