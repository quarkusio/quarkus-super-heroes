package io.quarkus.sample.superheroes.impact;

import static io.quarkus.sample.superheroes.impact.Models.ApiChange;
import static io.quarkus.sample.superheroes.impact.Models.ApiOperation;
import static io.quarkus.sample.superheroes.impact.Models.ChangeType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GitContractDiff {

  private final Path repositoryRoot;
  private final String baseReference;
  private final OpenApiContractReader reader;

  GitContractDiff(Path repositoryRoot, String baseReference, OpenApiContractReader reader) {
    this.repositoryRoot = repositoryRoot;
    this.baseReference = baseReference;
    this.reader = reader;
  }

  DiffResult analyze() throws IOException, InterruptedException {
    List<String> changedSpecifications = changedOpenApiSpecifications();
    List<ApiChange> allChanges = new ArrayList<>();
    for (String specification : changedSpecifications) {
      String provider = providerFrom(specification);
      byte[] beforeContent = gitShow(baseReference, specification);
      Path currentFile = repositoryRoot.resolve(specification);
      byte[] afterContent = Files.isRegularFile(currentFile) ? Files.readAllBytes(currentFile) : new byte[0];
      List<ApiOperation> before = reader.read(beforeContent, provider, specification);
      List<ApiOperation> after = reader.read(afterContent, provider, specification);
      allChanges.addAll(compare(before, after));
    }
    return new DiffResult(changedSpecifications, allChanges);
  }

  List<ApiChange> compare(List<ApiOperation> before, List<ApiOperation> after) {
    Map<String, ApiOperation> beforeByKey = indexByKey(before);
    Map<String, ApiOperation> afterByKey = indexByKey(after);
    List<ApiChange> changes = new ArrayList<>();
    Set<String> handledBefore = new HashSet<>();
    Set<String> handledAfter = new HashSet<>();

    for (Map.Entry<String, ApiOperation> entry : beforeByKey.entrySet()) {
      ApiOperation current = afterByKey.get(entry.getKey());
      if (current != null) {
        handledBefore.add(entry.getKey());
        handledAfter.add(entry.getKey());
        if (!entry.getValue().contractShape().equals(current.contractShape())) {
          changes.add(new ApiChange(
            ChangeType.CONTRACT_CHANGED,
            entry.getValue(),
            current,
            true,
            "Request, response, parameter, security, or operation identifier changed"));
        }
      }
    }

    Map<String, ApiOperation> unmatchedAddedByOperationId = new HashMap<>();
    after.stream()
      .filter(operation -> !handledAfter.contains(operation.key()))
      .filter(operation -> !operation.operationId().isBlank())
      .forEach(operation -> unmatchedAddedByOperationId.putIfAbsent(operation.operationId(), operation));

    for (ApiOperation removed : before) {
      if (handledBefore.contains(removed.key())) {
        continue;
      }
      ApiOperation movedTo = removed.operationId().isBlank()
                             ? null
                             : unmatchedAddedByOperationId.get(removed.operationId());
      if (movedTo != null && !handledAfter.contains(movedTo.key())) {
        handledBefore.add(removed.key());
        handledAfter.add(movedTo.key());
        changes.add(new ApiChange(
          ChangeType.MOVED,
          removed,
          movedTo,
          true,
          "HTTP method or path changed while operationId remained the same"));
      }
      else {
        handledBefore.add(removed.key());
        changes.add(new ApiChange(
          ChangeType.REMOVED,
          removed,
          null,
          true,
          "Operation no longer exists in the current contract"));
      }
    }

    for (ApiOperation added : after) {
      if (!handledAfter.contains(added.key())) {
        changes.add(new ApiChange(
          ChangeType.ADDED,
          null,
          added,
          false,
          "New API operation"));
      }
    }

    return changes;
  }

  private List<String> changedOpenApiSpecifications() throws IOException, InterruptedException {
    ProcessResult result = runGit("diff", "--name-status", "--find-renames", baseReference, "--");
    if (result.exitCode() != 0) {
      throw new IOException("Unable to compare against Git reference '" + baseReference + "': " + result.stderr());
    }

    Set<String> specifications = new HashSet<>();
    for (String line : result.stdout().lines().toList()) {
      if (line.isBlank()) {
        continue;
      }
      String[] columns = line.split("\\t");
      for (int index = 1; index < columns.length; index++) {
        String candidate = columns[index].replace('\\', '/');
        if (isOpenApiSpecification(candidate)) {
          specifications.add(candidate);
        }
      }
    }
    return specifications.stream().sorted().toList();
  }

  private boolean isOpenApiSpecification(String path) {
    String lower = path.toLowerCase();
    boolean supportedExtension = lower.endsWith(".yml")
      || lower.endsWith(".yaml")
      || lower.endsWith(".json");
    return supportedExtension
      && (lower.contains("/openapi/") || lower.contains("/meta-inf/"))
      && Path.of(path).getFileName().toString().toLowerCase().startsWith("openapi");
  }

  private byte[] gitShow(String reference, String path) throws IOException, InterruptedException {
    ProcessResult result = runGitBytes("show", reference + ":" + path);
    if (result.exitCode() != 0) {
      return new byte[0];
    }
    return result.stdoutBytes();
  }

  private ProcessResult runGit(String... arguments) throws IOException, InterruptedException {
    return runGitBytes(arguments);
  }

  private ProcessResult runGitBytes(String... arguments) throws IOException, InterruptedException {
    List<String> command = new ArrayList<>();
    command.add("git");
    command.addAll(List.of(arguments));
    Process process = new ProcessBuilder(command)
      .directory(repositoryRoot.toFile())
      .start();
    byte[] stdout = readAll(process.getInputStream());
    byte[] stderr = readAll(process.getErrorStream());
    int exitCode = process.waitFor();
    return new ProcessResult(exitCode, stdout, new String(stderr, StandardCharsets.UTF_8));
  }

  private byte[] readAll(InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    input.transferTo(output);
    return output.toByteArray();
  }

  private Map<String, ApiOperation> indexByKey(List<ApiOperation> operations) {
    Map<String, ApiOperation> result = new LinkedHashMap<>();
    operations.forEach(operation -> result.put(operation.key(), operation));
    return result;
  }

  private String providerFrom(String specification) {
    Path relative = Path.of(specification);
    return relative.getNameCount() == 0 ? "unknown" : relative.getName(0).toString();
  }

  record DiffResult(List<String> changedSpecifications, List<ApiChange> changes) {
  }

  private record ProcessResult(int exitCode, byte[] stdoutBytes, String stderr) {
    String stdout() {
      return new String(stdoutBytes, StandardCharsets.UTF_8);
    }
  }
}
