package io.quarkus.sample.superheroes.impact;

import static io.quarkus.sample.superheroes.impact.Models.ConsumerCall;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Discovers declarative Quarkus REST clients from annotations and application.properties.
 */
final class RestClientScanner {

  private static final Set<String> HTTP_METHODS = Set.of(
    "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");
  private static final Pattern PATH_ANNOTATION = Pattern.compile("@Path\\(\\s*\"([^\"]*)\"\\s*\\)");
  private static final Pattern CONFIG_KEY = Pattern.compile("configKey\\s*=\\s*\"([^\"]+)\"");
  private static final Pattern TYPE_DECLARATION = Pattern.compile(".*\\b(?:interface|class)\\s+(\\w+).*\\{.*");
  private static final Pattern PACKAGE_DECLARATION = Pattern.compile("package\\s+([\\w.]+)\\s*;");
  private static final Pattern METHOD_NAME = Pattern.compile("(?:<[^>]+>\\s+)?[\\w<>, ?.@\\[\\]]+\\s+(\\w+)\\s*\\(");

  List<ConsumerCall> scan(Path repositoryRoot) throws IOException {
    List<Path> sourceFiles;
    try (Stream<Path> paths = Files.walk(repositoryRoot)) {
      sourceFiles = paths
        .filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".java"))
        .filter(path -> normalized(path).contains("/src/main/java/"))
        .filter(path -> !normalized(path).contains("/api-impact-analyzer/"))
        .sorted()
        .toList();
    }

    Map<String, Map<String, String>> serviceProperties = new HashMap<>();
    List<ConsumerCall> calls = new ArrayList<>();
    for (Path sourceFile : sourceFiles) {
      String source = Files.readString(sourceFile);
      if (!source.contains("@RegisterRestClient")) {
        continue;
      }
      calls.addAll(scanFile(repositoryRoot, sourceFile, source, serviceProperties));
    }
    calls.sort(Comparator
      .comparing(ConsumerCall::consumer)
      .thenComparing(ConsumerCall::sourceFile)
      .thenComparing(ConsumerCall::javaMethod));
    return calls;
  }

  private List<ConsumerCall> scanFile(
    Path repositoryRoot,
    Path sourceFile,
    String source,
    Map<String, Map<String, String>> serviceProperties) {
    Path relativeSource = repositoryRoot.relativize(sourceFile);
    String consumer = relativeSource.getName(0).toString();
    String packageName = "";
    String typeName = "";
    String configKey = "";
    String classPath = "";
    String pendingPath = "";
    String pendingHttpMethod = "";
    boolean registrationSeen = false;
    boolean insideClientType = false;
    List<ConsumerCall> calls = new ArrayList<>();

    for (String rawLine : source.split("\\R")) {
      String line = rawLine.strip();
      Matcher packageMatcher = PACKAGE_DECLARATION.matcher(line);
      if (packageMatcher.matches()) {
        packageName = packageMatcher.group(1);
      }

      Matcher pathMatcher = PATH_ANNOTATION.matcher(line);
      if (pathMatcher.find()) {
        pendingPath = pathMatcher.group(1);
      }
      if (line.contains("@RegisterRestClient")) {
        registrationSeen = true;
        Matcher keyMatcher = CONFIG_KEY.matcher(line);
        if (keyMatcher.find()) {
          configKey = keyMatcher.group(1);
        }
      }

      Matcher typeMatcher = TYPE_DECLARATION.matcher(line);
      if (!insideClientType && registrationSeen && typeMatcher.matches()) {
        typeName = typeMatcher.group(1);
        if (configKey.isBlank()) {
          configKey = packageName.isBlank() ? typeName : packageName + "." + typeName;
        }
        classPath = pendingPath;
        pendingPath = "";
        insideClientType = true;
        continue;
      }

      if (!insideClientType) {
        continue;
      }

      for (String httpMethod : HTTP_METHODS) {
        if (line.matches(".*@" + httpMethod + "(?:\\s|\\(|$).*") || line.equals("@" + httpMethod)) {
          pendingHttpMethod = httpMethod;
          break;
        }
      }

      if (!pendingHttpMethod.isBlank() && line.contains("(") && !line.startsWith("@")) {
        Matcher methodMatcher = METHOD_NAME.matcher(line);
        if (methodMatcher.find()) {
          String methodName = methodMatcher.group(1);
          String fullPath = joinPaths(classPath, pendingPath);
          Map<String, String> properties = serviceProperties.computeIfAbsent(
            consumer,
            ignored -> readApplicationProperties(repositoryRoot.resolve(consumer)));
          String targetUrl = findTargetUrl(properties, configKey);
          calls.add(new ConsumerCall(
            consumer,
            normalized(relativeSource),
            typeName,
            methodName,
            configKey,
            targetUrl,
            pendingHttpMethod,
            fullPath,
            targetUrl.isBlank() ? "MEDIUM" : "HIGH"));
          pendingHttpMethod = "";
          pendingPath = "";
        }
      }
    }
    return calls;
  }

  private Map<String, String> readApplicationProperties(Path serviceRoot) {
    Path propertiesPath = serviceRoot.resolve("src/main/resources/application.properties");
    if (!Files.isRegularFile(propertiesPath)) {
      return Map.of();
    }
    try (Stream<String> lines = Files.lines(propertiesPath)) {
      Map<String, String> properties = new HashMap<>();
      lines.map(String::strip)
        .filter(line -> !line.isEmpty() && !line.startsWith("#") && line.contains("="))
        .forEach(line -> {
          int separator = line.indexOf('=');
          properties.putIfAbsent(line.substring(0, separator).strip(), line.substring(separator + 1).strip());
        });
      return properties;
    }
    catch (IOException ignored) {
      return Map.of();
    }
  }

  private String findTargetUrl(Map<String, String> properties, String configKey) {
    for (String key : List.of(
      "quarkus.rest-client." + configKey + ".url",
      "quarkus.rest-client.\"" + configKey + "\".url",
      "quarkus.rest-client." + configKey + ".uri",
      "quarkus.rest-client.\"" + configKey + "\".uri",
      configKey + "/mp-rest/url",
      configKey + "/mp-rest/uri")) {
      if (properties.containsKey(key)) {
        return properties.get(key);
      }
    }
    return "";
  }

  static String joinPaths(String first, String second) {
    String left = first == null ? "" : first.strip();
    String right = second == null ? "" : second.strip();
    return OpenApiContractReader.normalizePath(left + "/" + right);
  }

  static String targetServiceName(String targetUrl) {
    if (targetUrl == null || targetUrl.isBlank() || targetUrl.contains("${")) {
      return "";
    }
    try {
      URI uri = URI.create(targetUrl);
      return uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
    }
    catch (IllegalArgumentException ignored) {
      return "";
    }
  }

  private static String normalized(Path path) {
    return path.toString().replace('\\', '/');
  }
}
