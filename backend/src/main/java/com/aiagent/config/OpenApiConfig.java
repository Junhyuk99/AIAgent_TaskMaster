package com.aiagent.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String JWT_SECURITY_SCHEME = "bearerAuth";
    private static final String API_KEY_SECURITY_SCHEME = "apiKeyAuth";

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .externalDocs(externalDocs())
                .servers(servers())
                .tags(tags())
                .addSecurityItem(new SecurityRequirement()
                        .addList(JWT_SECURITY_SCHEME)
                        .addList(API_KEY_SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(JWT_SECURITY_SCHEME, jwtSecurityScheme())
                        .addSecuritySchemes(API_KEY_SECURITY_SCHEME, apiKeySecurityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("AI Agent Platform API")
                .description("""
                        AI Agent Management Platform API Documentation.

                        This API allows you to:
                        - Manage AI agents with custom configurations
                        - Connect to LLM servers (Ollama, OpenAI, etc.)
                        - Create and manage knowledge bases with RAG support
                        - Define custom functions for agent tool use
                        - Chat with agents via REST API or streaming

                        **Authentication:**
                        - Use JWT Bearer token for web application access
                        - Use X-API-Key header for external/programmatic access
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("AI Agent Platform")
                        .email("support@aiagent.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private ExternalDocumentation externalDocs() {
        return new ExternalDocumentation()
                .description("AI Agent Platform Documentation")
                .url("https://docs.aiagent.com");
    }

    private List<Server> servers() {
        return Arrays.asList(
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local Development Server"),
                new Server()
                        .url("https://api.aiagent.com")
                        .description("Production Server")
        );
    }

    private List<Tag> tags() {
        return Arrays.asList(
                new Tag().name("Authentication").description("User authentication and authorization"),
                new Tag().name("Agents").description("AI agent management"),
                new Tag().name("LLM Servers").description("LLM server configuration"),
                new Tag().name("Knowledge Bases").description("Knowledge base and document management"),
                new Tag().name("Functions").description("Agent function definitions"),
                new Tag().name("Chat").description("Agent chat interactions"),
                new Tag().name("Conversations").description("Conversation history management"),
                new Tag().name("API Keys").description("API key management"),
                new Tag().name("External API").description("External API for programmatic agent access")
        );
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(JWT_SECURITY_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT authentication for web application access. " +
                        "Obtain token from /api/auth/login endpoint.");
    }

    private SecurityScheme apiKeySecurityScheme() {
        return new SecurityScheme()
                .name(API_KEY_SECURITY_SCHEME)
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("API key authentication for external/programmatic access. " +
                        "Create API keys from the API Keys management page.");
    }
}
