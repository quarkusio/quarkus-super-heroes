package io.quarkus.sample.superheroes.impact;

import java.util.List;

final class Models {

  private Models() {
  }

  enum ChangeType {
    ADDED,
    REMOVED,
    MOVED,
    CONTRACT_CHANGED
  }

  record ApiOperation(
    String provider,
    String specificationFile,
    String method,
    String path,
    String operationId,
    String contractShape) {

    String key() {
      return method + " " + path;
    }
  }

  record ApiChange(
    ChangeType type,
    ApiOperation before,
    ApiOperation after,
    boolean breaking,
    String reason) {

    ApiOperation affectedOperation() {
      return before != null ? before : after;
    }
  }

  record ConsumerCall(
    String consumer,
    String sourceFile,
    String javaType,
    String javaMethod,
    String configKey,
    String targetUrl,
    String method,
    String path,
    String confidence) {

    String key() {
      return method + " " + path;
    }
  }

  record Impact(ApiChange change, ConsumerCall consumer) {
  }

  record Summary(
    int changedContracts,
    int breakingChanges,
    int potentiallyBreakingChanges,
    int additions,
    int discoveredConsumerCalls,
    int impactedConsumers) {
  }

  record AnalysisOutput(
    Summary summary,
    List<ApiChange> changes,
    List<ConsumerCall> dependencies,
    List<Impact> impacts) {
  }
}
