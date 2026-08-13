package io.quarkus.sample.superheroes.impact;

import static io.quarkus.sample.superheroes.impact.Models.AnalysisOutput;
import static io.quarkus.sample.superheroes.impact.Models.ApiChange;
import static io.quarkus.sample.superheroes.impact.Models.ApiOperation;
import static io.quarkus.sample.superheroes.impact.Models.ConsumerCall;
import static io.quarkus.sample.superheroes.impact.Models.Impact;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class ReportWriter {

  void writeMarkdown(Path destination, AnalysisOutput output) throws IOException {
    StringBuilder report = new StringBuilder();
    report.append("# API Impact Report\n\n");
    report.append("This report was generated deterministically from the OpenAPI contract diff and Quarkus REST client declarations.\n\n");
    report.append("## Summary\n\n");
    report.append("| Metric | Count |\n|---|---:|\n");
    report.append("| Changed OpenAPI operations | ").append(output.changes().size()).append(" |\n");
    report.append("| Breaking changes | ").append(output.summary().breakingChanges()).append(" |\n");
    report.append("| Discovered REST client calls | ").append(output.summary().discoveredConsumerCalls()).append(" |\n");
    report.append("| Impacted consumers | ").append(output.summary().impactedConsumers()).append(" |\n\n");

    if (output.changes().isEmpty()) {
      report.append("No OpenAPI contract changes were detected relative to the selected base reference.\n");
      write(destination, report.toString());
      return;
    }

    List<ApiChange> breakingChanges = output.changes().stream().filter(ApiChange::breaking).toList();
    if (!breakingChanges.isEmpty()) {
      report.append("## Breaking or potentially breaking changes\n\n");
      for (ApiChange change : breakingChanges) {
        appendChange(report, change, output.impacts());
      }
    }

    List<ApiChange> additions = output.changes().stream().filter(change -> !change.breaking()).toList();
    if (!additions.isEmpty()) {
      report.append("## Additions\n\n");
      for (ApiChange addition : additions) {
        ApiOperation operation = addition.after();
        report.append("- `").append(operation.key()).append("` in `")
          .append(operation.provider()).append("` (`")
          .append(operation.operationId()).append("`)\n");
      }
      report.append('\n');
    }

    report.append("## Discovered REST dependencies\n\n");
    if (output.dependencies().isEmpty()) {
      report.append("No declarative Quarkus REST clients were discovered.\n");
    }
    else {
      report.append("| Consumer | Call | Target | Evidence | Confidence |\n");
      report.append("|---|---|---|---|---|\n");
      for (ConsumerCall dependency : output.dependencies()) {
        report.append("| ").append(cell(dependency.consumer()))
          .append(" | `").append(dependency.key()).append("`")
          .append(" | `").append(cell(dependency.targetUrl())).append("`")
          .append(" | `").append(cell(dependency.sourceFile())).append("`")
          .append(" | ").append(dependency.confidence()).append(" |\n");
      }
    }
    report.append("\n> Copilot should explain this report and suggest local adaptations, but it must not invent dependencies that are not listed here.\n");
    write(destination, report.toString());
  }

  void writeJson(Path destination, AnalysisOutput output) throws IOException {
    createParent(destination);
    StringBuilder json = new StringBuilder();
    json.append("{\n  \"summary\": {\n")
      .append("    \"changedContracts\": ").append(output.summary().changedContracts()).append(",\n")
      .append("    \"breakingChanges\": ").append(output.summary().breakingChanges()).append(",\n")
      .append("    \"additions\": ").append(output.summary().additions()).append(",\n")
      .append("    \"discoveredConsumerCalls\": ").append(output.summary().discoveredConsumerCalls()).append(",\n")
      .append("    \"impactedConsumers\": ").append(output.summary().impactedConsumers()).append("\n")
      .append("  },\n  \"dependencies\": [\n");
    for (int index = 0; index < output.dependencies().size(); index++) {
      ConsumerCall call = output.dependencies().get(index);
      json.append("    {\n")
        .append(jsonField("consumer", call.consumer(), 6)).append(",\n")
        .append(jsonField("sourceFile", call.sourceFile(), 6)).append(",\n")
        .append(jsonField("javaType", call.javaType(), 6)).append(",\n")
        .append(jsonField("javaMethod", call.javaMethod(), 6)).append(",\n")
        .append(jsonField("configKey", call.configKey(), 6)).append(",\n")
        .append(jsonField("targetUrl", call.targetUrl(), 6)).append(",\n")
        .append(jsonField("method", call.method(), 6)).append(",\n")
        .append(jsonField("path", call.path(), 6)).append(",\n")
        .append(jsonField("confidence", call.confidence(), 6)).append("\n")
        .append("    }").append(index + 1 == output.dependencies().size() ? "\n" : ",\n");
    }
    json.append("  ],\n  \"impacts\": [\n");
    for (int index = 0; index < output.impacts().size(); index++) {
      Impact impact = output.impacts().get(index);
      json.append("    {\n")
        .append(jsonField("changeType", impact.change().type().name(), 6)).append(",\n")
        .append(jsonField("provider", impact.change().before().provider(), 6)).append(",\n")
        .append(jsonField("operation", impact.change().before().key(), 6)).append(",\n")
        .append(jsonField("consumer", impact.consumer().consumer(), 6)).append(",\n")
        .append(jsonField("evidence", impact.consumer().sourceFile(), 6)).append("\n")
        .append("    }").append(index + 1 == output.impacts().size() ? "\n" : ",\n");
    }
    json.append("  ]\n}\n");
    Files.writeString(destination, json.toString(), StandardCharsets.UTF_8);
  }

  private void appendChange(StringBuilder report, ApiChange change, List<Impact> allImpacts) {
    ApiOperation before = change.before();
    report.append("### ").append(change.type()).append(": `")
      .append(before.key()).append("`\n\n");
    report.append("- Provider: `").append(before.provider()).append("`\n");
    report.append("- Operation ID: `").append(before.operationId()).append("`\n");
    report.append("- Contract: `").append(before.specificationFile()).append("`\n");
    report.append("- Reason: ").append(change.reason()).append("\n");
    if (change.after() != null && !change.after().key().equals(before.key())) {
      report.append("- Candidate replacement: `").append(change.after().key()).append("`\n");
    }
    report.append('\n');

    List<Impact> impacts = allImpacts.stream().filter(impact -> impact.change().equals(change)).toList();
    if (impacts.isEmpty()) {
      report.append("No consumer was found by the current static scanner. Human verification is still required.\n\n");
      return;
    }

    report.append("Affected consumers:\n\n");
    for (Impact impact : impacts) {
      ConsumerCall consumer = impact.consumer();
      report.append("- **").append(consumer.consumer()).append("** — `")
        .append(consumer.sourceFile()).append("`\n")
        .append("  - Java call: `").append(consumer.javaType()).append('.')
        .append(consumer.javaMethod()).append("()`\n")
        .append("  - Config key: `").append(consumer.configKey()).append("`\n")
        .append("  - Target: `").append(consumer.targetUrl()).append("`\n")
        .append("  - Confidence: **").append(consumer.confidence()).append("**\n");
    }
    report.append("\nAction required: update or verify the listed clients and their contract tests before merging.\n\n");
  }

  private void write(Path destination, String content) throws IOException {
    createParent(destination);
    Files.writeString(destination, content, StandardCharsets.UTF_8);
  }

  private void createParent(Path destination) throws IOException {
    Path parent = destination.toAbsolutePath().getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
  }

  private String cell(String value) {
    return value == null || value.isBlank() ? "—" : value.replace("|", "\\|");
  }

  private String jsonField(String name, String value, int indentation) {
    return " ".repeat(indentation) + "\"" + escapeJson(name) + "\": \"" + escapeJson(value) + "\"";
  }

  private String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t");
  }
}
