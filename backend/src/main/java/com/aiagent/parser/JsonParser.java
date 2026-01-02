package com.aiagent.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonParser implements DocumentParser {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("json");
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "application/json",
            "text/json"
    );

    private final ObjectMapper objectMapper;

    @Override
    public Set<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    @Override
    public Set<String> getSupportedMimeTypes() {
        return SUPPORTED_MIME_TYPES;
    }

    @Override
    public ParseResult parse(InputStream inputStream, String fileName) throws Exception {
        JsonNode rootNode = objectMapper.readTree(inputStream);

        StringBuilder content = new StringBuilder();
        List<String> topLevelKeys = new ArrayList<>();
        boolean isArray = rootNode.isArray();

        flattenJson(rootNode, "", content, topLevelKeys);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("isArray", isArray);
        metadata.put("topLevelKeys", topLevelKeys);
        metadata.put("format", "JSON");

        if (isArray) {
            metadata.put("arrayLength", rootNode.size());
        }

        String contentStr = content.toString().trim();

        log.info("Parsed JSON: {} - {} characters, isArray: {}",
                fileName, contentStr.length(), isArray);

        return ParseResult.builder()
                .content(contentStr)
                .metadata(metadata)
                .pageCount(1)
                .sections(topLevelKeys)
                .characterCount(contentStr.length())
                .wordCount(countWords(contentStr))
                .build();
    }

    private void flattenJson(JsonNode node, String prefix, StringBuilder content, List<String> topLevelKeys) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;

                if (prefix.isEmpty()) {
                    topLevelKeys.add(key);
                }

                flattenJson(field.getValue(), newPrefix, content, topLevelKeys);
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                String newPrefix = prefix + "[" + i + "]";
                flattenJson(arrayNode.get(i), newPrefix, content, topLevelKeys);
            }
        } else {
            // Leaf node - append to content
            String value = node.isTextual() ? node.asText() :
                          node.isNull() ? "null" : node.toString();
            content.append(prefix).append(": ").append(value).append("\n");
        }
    }

    private long countWords(String content) {
        if (content == null || content.isBlank()) return 0;
        return content.split("\\s+").length;
    }
}
