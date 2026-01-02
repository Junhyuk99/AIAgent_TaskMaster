package com.aiagent.function;

import com.aiagent.entity.Function;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Converts Function entities to OpenAI/Ollama compatible JSON Schema format
 * for use with LLM function calling (tools).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FunctionSchemaConverter {

    private final ObjectMapper objectMapper;

    /**
     * Convert a set of functions to the tools array format for LLM API.
     */
    public List<Map<String, Object>> convertToTools(Set<Function> functions) {
        if (functions == null || functions.isEmpty()) {
            return Collections.emptyList();
        }

        return functions.stream()
                .filter(Function::getIsActive)
                .map(this::convertToTool)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Convert a single function to tool format.
     */
    public Map<String, Object> convertToTool(Function function) {
        try {
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("type", "function");

            Map<String, Object> functionDef = new LinkedHashMap<>();
            functionDef.put("name", sanitizeFunctionName(function.getName()));
            functionDef.put("description", function.getDescription() != null ?
                    function.getDescription() : "Function: " + function.getName());

            // Parse and convert parameters schema
            Map<String, Object> parameters = convertParametersSchema(function.getParametersSchema());
            functionDef.put("parameters", parameters);

            tool.put("function", functionDef);
            return tool;

        } catch (Exception e) {
            log.error("Failed to convert function {} to tool schema: {}",
                    function.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Convert the stored parameters schema to OpenAI-compatible format.
     * The stored schema is expected to be in a simplified format with parameter definitions.
     */
    private Map<String, Object> convertParametersSchema(String parametersSchemaJson) throws JsonProcessingException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "object");

        if (parametersSchemaJson == null || parametersSchemaJson.isBlank()) {
            result.put("properties", Collections.emptyMap());
            result.put("required", Collections.emptyList());
            return result;
        }

        JsonNode schemaNode = objectMapper.readTree(parametersSchemaJson);

        // Handle if it's already in JSON Schema format
        if (schemaNode.has("type") && "object".equals(schemaNode.get("type").asText())) {
            return objectMapper.convertValue(schemaNode, Map.class);
        }

        // Handle array format (list of parameter definitions)
        if (schemaNode.isArray()) {
            return convertParameterArrayToSchema((ArrayNode) schemaNode);
        }

        // Handle object format with properties
        if (schemaNode.isObject()) {
            return convertParameterObjectToSchema((ObjectNode) schemaNode);
        }

        result.put("properties", Collections.emptyMap());
        result.put("required", Collections.emptyList());
        return result;
    }

    /**
     * Convert array of parameter definitions to JSON Schema.
     * Expected format: [{"name": "param1", "type": "string", "description": "...", "required": true}, ...]
     */
    private Map<String, Object> convertParameterArrayToSchema(ArrayNode parametersArray) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (JsonNode param : parametersArray) {
            String name = param.has("name") ? param.get("name").asText() : null;
            if (name == null || name.isEmpty()) continue;

            Map<String, Object> propertyDef = new LinkedHashMap<>();

            // Get type, default to string
            String type = param.has("type") ? param.get("type").asText() : "string";
            propertyDef.put("type", mapToJsonSchemaType(type));

            // Add description if present
            if (param.has("description") && !param.get("description").isNull()) {
                propertyDef.put("description", param.get("description").asText());
            }

            // Handle enum values
            if (param.has("enum") && param.get("enum").isArray()) {
                List<String> enumValues = new ArrayList<>();
                for (JsonNode enumValue : param.get("enum")) {
                    enumValues.add(enumValue.asText());
                }
                propertyDef.put("enum", enumValues);
            }

            // Handle array items type
            if ("array".equals(type) && param.has("items")) {
                JsonNode items = param.get("items");
                if (items.isObject()) {
                    propertyDef.put("items", objectMapper.convertValue(items, Map.class));
                } else {
                    propertyDef.put("items", Map.of("type", items.asText()));
                }
            }

            // Handle default value
            if (param.has("default") && !param.get("default").isNull()) {
                propertyDef.put("default", objectMapper.convertValue(param.get("default"), Object.class));
            }

            properties.put(name, propertyDef);

            // Check if required
            if (param.has("required") && param.get("required").asBoolean()) {
                required.add(name);
            }
        }

        result.put("properties", properties);
        if (!required.isEmpty()) {
            result.put("required", required);
        }

        return result;
    }

    /**
     * Convert object format parameters to JSON Schema.
     * Expected format: {"param1": {"type": "string", ...}, ...}
     */
    private Map<String, Object> convertParameterObjectToSchema(ObjectNode parametersObject) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        Iterator<Map.Entry<String, JsonNode>> fields = parametersObject.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String name = field.getKey();
            JsonNode value = field.getValue();

            if (value.isObject()) {
                Map<String, Object> propertyDef = new LinkedHashMap<>();

                String type = value.has("type") ? value.get("type").asText() : "string";
                propertyDef.put("type", mapToJsonSchemaType(type));

                if (value.has("description")) {
                    propertyDef.put("description", value.get("description").asText());
                }

                if (value.has("enum") && value.get("enum").isArray()) {
                    propertyDef.put("enum", objectMapper.convertValue(value.get("enum"), List.class));
                }

                if (value.has("required") && value.get("required").asBoolean()) {
                    required.add(name);
                }

                properties.put(name, propertyDef);
            }
        }

        result.put("properties", properties);
        if (!required.isEmpty()) {
            result.put("required", required);
        }

        return result;
    }

    /**
     * Map common type names to JSON Schema types.
     */
    private String mapToJsonSchemaType(String type) {
        if (type == null) return "string";

        return switch (type.toLowerCase()) {
            case "string", "text" -> "string";
            case "integer", "int", "long" -> "integer";
            case "number", "float", "double", "decimal" -> "number";
            case "boolean", "bool" -> "boolean";
            case "array", "list" -> "array";
            case "object", "map" -> "object";
            default -> "string";
        };
    }

    /**
     * Sanitize function name to be compatible with LLM API requirements.
     * Most LLM APIs require alphanumeric characters and underscores only.
     */
    private String sanitizeFunctionName(String name) {
        if (name == null) return "unnamed_function";
        // Replace spaces and special characters with underscores
        return name.replaceAll("[^a-zA-Z0-9_]", "_")
                   .replaceAll("_+", "_")
                   .replaceAll("^_|_$", "");
    }

    /**
     * Find a function by name from a set of functions.
     */
    public Optional<Function> findFunctionByName(Set<Function> functions, String name) {
        if (functions == null || name == null) return Optional.empty();

        String sanitizedName = sanitizeFunctionName(name);
        return functions.stream()
                .filter(f -> sanitizeFunctionName(f.getName()).equals(sanitizedName))
                .findFirst();
    }
}
