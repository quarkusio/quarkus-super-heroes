package io.quarkus.sample.superheroes.impact;

import static io.quarkus.sample.superheroes.impact.Models.ApiOperation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the small, deterministic OpenAPI subset needed by the PoC without a YAML library.
 * It recognizes operations under {@code paths} and fingerprints request/response structure.
 */
final class OpenApiContractReader {

  private static final Set<String> HTTP_METHODS = Set.of(
    "get", "post", "put", "patch", "delete", "head", "options", "trace");
  private static final Pattern PATH_LINE = Pattern.compile("^ {2}(/.+):\\s*$");
  private static final Pattern METHOD_LINE = Pattern.compile("^ {4}([a-zA-Z]+):\\s*$");
  private static final Pattern OPERATION_ID = Pattern.compile("^operationId:\\s*(.+?)\\s*$");
  private static final Set<String> CONTRACT_KEYS = Set.of(
    "operationId", "parameters", "requestBody", "responses", "security",
    "name", "in", "required", "schema", "$ref", "type", "format",
    "content", "items", "properties", "additionalProperties", "nullable",
    "minimum", "maximum", "minLength", "maxLength", "pattern", "enum",
    "oneOf", "allOf", "anyOf", "not");

  List<ApiOperation> read(byte[] content, String provider, String specificationFile) {
    if (content == null || content.length == 0) {
      return List.of();
    }

    String currentPath = null;
    String currentMethod = null;
    String currentOperationId = "";
    StringBuilder shape = new StringBuilder();
    List<ApiOperation> operations = new ArrayList<>();

    for (String rawLine : new String(content, StandardCharsets.UTF_8).split("\\R")) {
      Matcher pathMatcher = PATH_LINE.matcher(rawLine);
      if (pathMatcher.matches()) {
        addOperation(operations, provider, specificationFile, currentMethod, currentPath, currentOperationId, shape);
        currentPath = normalizePath(unquote(pathMatcher.group(1)));
        currentMethod = null;
        currentOperationId = "";
        shape.setLength(0);
        continue;
      }

      Matcher methodMatcher = METHOD_LINE.matcher(rawLine);
      if (currentPath != null && methodMatcher.matches()) {
        String candidate = methodMatcher.group(1).toLowerCase(Locale.ROOT);
        if (HTTP_METHODS.contains(candidate)) {
          addOperation(operations, provider, specificationFile, currentMethod, currentPath, currentOperationId, shape);
          currentMethod = candidate.toUpperCase(Locale.ROOT);
          currentOperationId = "";
          shape.setLength(0);
          continue;
        }
      }

      if (currentMethod == null) {
        continue;
      }

      String trimmed = rawLine.strip();
      Matcher operationIdMatcher = OPERATION_ID.matcher(trimmed);
      if (operationIdMatcher.matches()) {
        currentOperationId = unquote(operationIdMatcher.group(1));
      }
      if (isContractLine(trimmed)) {
        shape.append(trimmed.replaceAll("\\s+", " ")).append('\n');
      }
    }
    addOperation(operations, provider, specificationFile, currentMethod, currentPath, currentOperationId, shape);
    return operations;
  }

  private void addOperation(
    List<ApiOperation> operations,
    String provider,
    String specificationFile,
    String method,
    String path,
    String operationId,
    StringBuilder shape) {
    if (method == null || path == null) {
      return;
    }
    operations.add(new ApiOperation(
      provider,
      specificationFile,
      method,
      path,
      operationId,
      shape.toString()));
  }

  private boolean isContractLine(String trimmed) {
    if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("description:")
      || trimmed.startsWith("summary:") || trimmed.startsWith("example:")
      || trimmed.startsWith("examples:") || trimmed.startsWith("tags:")) {
      return false;
    }
    int separator = trimmed.indexOf(':');
    if (separator < 0) {
      return trimmed.startsWith("-");
    }
    String key = unquote(trimmed.substring(0, separator).strip());
    return CONTRACT_KEYS.contains(key)
      || key.matches("[1-5][0-9][0-9]")
      || key.contains("/");
  }

  static String normalizePath(String path) {
    if (path == null || path.isBlank() || "/".equals(path.strip())) {
      return "/";
    }
    String normalized = path.strip().replaceAll("/{2,}", "/");
    if (!normalized.startsWith("/")) {
      normalized = "/" + normalized;
    }
    if (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private static String unquote(String value) {
    String stripped = value.strip();
    if (stripped.length() >= 2
      && ((stripped.startsWith("\"") && stripped.endsWith("\""))
      || (stripped.startsWith("'") && stripped.endsWith("'")))) {
      return stripped.substring(1, stripped.length() - 1);
    }
    return stripped;
  }
}
