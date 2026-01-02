package com.aiagent.service;

import com.aiagent.dto.function.FunctionRequest;
import com.aiagent.dto.function.FunctionResponse;
import com.aiagent.dto.function.FunctionTestRequest;
import com.aiagent.dto.function.FunctionTestResponse;
import com.aiagent.entity.Function;
import com.aiagent.entity.User;
import com.aiagent.executor.FunctionExecutionResult;
import com.aiagent.executor.http.HttpApiFunctionExecutor;
import com.aiagent.executor.template.TemplateFunctionExecutor;
import com.aiagent.executor.code.CodeFunctionExecutor;
import com.aiagent.repository.FunctionRepository;
import com.aiagent.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionService {

    private final FunctionRepository functionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final HttpApiFunctionExecutor httpApiFunctionExecutor;
    private final TemplateFunctionExecutor templateFunctionExecutor;
    private final CodeFunctionExecutor codeFunctionExecutor;

    @Transactional(readOnly = true)
    public List<FunctionResponse> getAllFunctions(Long userId) {
        return functionRepository.findByUserId(userId).stream()
                .map(FunctionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<FunctionResponse> getFunctionsPaged(Long userId, Pageable pageable) {
        return functionRepository.findByUserId(userId, pageable)
                .map(FunctionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<FunctionResponse> getActiveFunctions(Long userId) {
        return functionRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(FunctionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FunctionResponse getFunction(Long id, Long userId) {
        Function function = findFunctionByIdAndUser(id, userId);
        return FunctionResponse.from(function);
    }

    @Transactional
    public FunctionResponse createFunction(FunctionRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Function function = Function.builder()
                .name(request.getName())
                .description(request.getDescription())
                .parametersSchema(request.getParametersSchema())
                .returnType(request.getReturnType())
                .implementationType(request.getImplementationType())
                .implementationConfig(request.getImplementationConfig())
                .isActive(true)
                .user(user)
                .build();

        Function saved = functionRepository.save(function);
        log.info("Created function: {} for user: {}", saved.getName(), userId);
        return FunctionResponse.from(saved);
    }

    @Transactional
    public FunctionResponse updateFunction(Long id, FunctionRequest request, Long userId) {
        Function function = findFunctionByIdAndUser(id, userId);

        function.setName(request.getName());
        function.setDescription(request.getDescription());
        function.setParametersSchema(request.getParametersSchema());
        function.setReturnType(request.getReturnType());
        function.setImplementationType(request.getImplementationType());
        function.setImplementationConfig(request.getImplementationConfig());

        Function updated = functionRepository.save(function);
        log.info("Updated function: {}", updated.getName());
        return FunctionResponse.from(updated);
    }

    @Transactional
    public void deleteFunction(Long id, Long userId) {
        Function function = findFunctionByIdAndUser(id, userId);
        functionRepository.delete(function);
        log.info("Deleted function: {}", function.getName());
    }

    @Transactional
    public FunctionResponse toggleActive(Long id, Long userId) {
        Function function = findFunctionByIdAndUser(id, userId);
        function.setIsActive(!function.getIsActive());
        Function updated = functionRepository.save(function);
        log.info("Toggled function active status: {} -> {}", function.getName(), updated.getIsActive());
        return FunctionResponse.from(updated);
    }

    public FunctionTestResponse testFunction(Long id, Long userId, FunctionTestRequest request) {
        Function function = findFunctionByIdAndUser(id, userId);

        FunctionExecutionResult result = executeFunction(function, request.getParameters());

        return FunctionTestResponse.builder()
                .success(result.isSuccess())
                .result(result.isSuccess() ? result.getData() : null)
                .error(result.getError())
                .executionTimeMs(result.getExecutionTimeMs())
                .build();
    }

    /**
     * Execute a function with the given parameters.
     * Delegates to the appropriate executor based on implementation type.
     */
    public FunctionExecutionResult executeFunction(Function function, Map<String, Object> parameters) {
        String config = function.getImplementationConfig();

        try {
            return switch (function.getImplementationType()) {
                case HTTP_API -> httpApiFunctionExecutor.execute(config, parameters);
                case CODE -> codeFunctionExecutor.execute(config, parameters);
                case TEMPLATE -> templateFunctionExecutor.execute(config, parameters);
            };
        } catch (Exception e) {
            log.error("Function execution failed for {}: {}", function.getName(), e.getMessage());
            return FunctionExecutionResult.failure(e.getMessage(), 0);
        }
    }

    /**
     * Execute a function by ID with the given parameters.
     * Used for agent function calls.
     */
    public FunctionExecutionResult executeFunctionById(Long id, Long userId, Map<String, Object> parameters) {
        Function function = findFunctionByIdAndUser(id, userId);
        return executeFunction(function, parameters);
    }

    @Transactional(readOnly = true)
    public List<FunctionResponse> searchFunctions(Long userId, String query) {
        return functionRepository.findByUserId(userId).stream()
                .filter(func -> func.getName().toLowerCase().contains(query.toLowerCase()) ||
                        (func.getDescription() != null &&
                         func.getDescription().toLowerCase().contains(query.toLowerCase())))
                .map(FunctionResponse::from)
                .toList();
    }

    private Function findFunctionByIdAndUser(Long id, Long userId) {
        return functionRepository.findById(id)
                .filter(function -> function.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Function not found or access denied"));
    }
}
