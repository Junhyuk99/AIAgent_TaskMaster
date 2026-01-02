package com.aiagent.parser;

import java.io.InputStream;
import java.util.Set;

public interface DocumentParser {

    Set<String> getSupportedExtensions();

    Set<String> getSupportedMimeTypes();

    ParseResult parse(InputStream inputStream, String fileName) throws Exception;
}
