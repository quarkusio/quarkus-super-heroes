package io.quarkus.sample.superheroes.impact;

import static io.quarkus.sample.superheroes.impact.Models.AnalysisOutput;
import static io.quarkus.sample.superheroes.impact.Models.ApiChange;
import static io.quarkus.sample.superheroes.impact.Models.ApiOperation;
import static io.quarkus.sample.superheroes.impact.Models.ConsumerCall;
import static io.quarkus.sample.superheroes.impact.Models.Impact;
import static io.quarkus.sample.superheroes.impact.Models.Summary;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ApiImpactAnalyzer {

  private ApiImpactAnalyzer() {
  }

  public static void main(String[] args) {
    int exitCode = run(args, System.out, System.err);
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  static int run(String[] args, PrintStream out, PrintStream err) {
    try {
      Options options = Options.parse(args);
      Path repositoryRoot = options.repositoryRoot().toAbsolutePath().normalize();

      OpenApiContractReader contractReader = new OpenApiContractReader();
      GitContractDiff.DiffResult contractDiff = new GitContractDiff(
        repositoryRoot,
        options.baseReference(),
        contractReader).analyze();

      List<ConsumerCall> dependencies = new RestClientScanner().scan(repositoryRoot);
      List<Impact> impacts = matchImpacts(contractDiff.changes(), dependencies);
      Summary summary = summary(contractDiff.changedSpecifications().size(), contractDiff.changes(), dependencies, impacts);
      AnalysisOutput output = new AnalysisOutput(summary, contractDiff.changes(), dependencies, impacts);

      ReportWriter writer = new ReportWriter();
      writer.writeMarkdown(repositoryRoot.resolve(options.reportFile()), output);
      writer.writeJson(repositoryRoot.resolve(options.indexFile()), output);

      out.println("API impact analysis completed");
      out.println("Changed contract files: " + summary.changedContracts());
      out.println("Breaking changes: " + summary.breakingChanges());
      out.println("Discovered REST calls: " + summary.discoveredConsumerCalls());
      out.println("Impacted consumers: " + summary.impactedConsumers());
      out.println("Report: " + options.reportFile());
      out.println("Machine-readable index: " + options.indexFile());

      if (options.failOnBreaking() && summary.breakingChanges() > 0) {
        err.println("The check failed because breaking API changes were detected.");
        return 2;
      }
      return 0;
    }
    catch (IllegalArgumentException exception) {
      err.println(exception.getMessage());
      err.println(Options.usage());
      return 64;
    }
    catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }

  static List<Impact> matchImpacts(List<ApiChange> changes, List<ConsumerCall> dependencies) {
    List<Impact> impacts = new ArrayList<>();
    for (ApiChange change : changes) {
      if (!change.breaking() || change.before() == null) {
        continue;
      }
      ApiOperation operation = change.before();
      for (ConsumerCall dependency : dependencies) {
        if (dependency.key().equals(operation.key()) && targetsProvider(dependency, operation.provider())) {
          impacts.add(new Impact(change, dependency));
        }
      }
    }
    return impacts;
  }

  private static boolean targetsProvider(ConsumerCall call, String provider) {
    String target = RestClientScanner.targetServiceName(call.targetUrl());
    if (target.isBlank() || target.equals("localhost") || target.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
      return true;
    }
    return providerAliases(provider).contains(target);
  }

  private static Set<String> providerAliases(String provider) {
    String normalized = provider.toLowerCase(Locale.ROOT);
    String core = normalized.startsWith("rest-") ? normalized.substring("rest-".length()) : normalized;
    String singular;
    if (core.endsWith("oes")) {
      singular = core.substring(0, core.length() - 2);
    }
    else if (core.endsWith("ies")) {
      singular = core.substring(0, core.length() - 3) + "y";
    }
    else {
      singular = core.endsWith("s") ? core.substring(0, core.length() - 1) : core;
    }
    Set<String> aliases = new HashSet<>();
    aliases.add(normalized);
    aliases.add(core);
    aliases.add(singular);
    aliases.add(core + "-service");
    aliases.add(singular + "-service");
    return aliases;
  }

  private static Summary summary(
    int changedContracts,
    List<ApiChange> changes,
    List<ConsumerCall> dependencies,
    List<Impact> impacts) {
    int breaking = (int) changes.stream().filter(ApiChange::breaking).count();
    int additions = changes.size() - breaking;
    long impactedConsumers = impacts.stream().map(impact -> impact.consumer().consumer()).distinct().count();
    return new Summary(
      changedContracts,
      breaking,
      0,
      additions,
      dependencies.size(),
      Math.toIntExact(impactedConsumers));
  }

  record Options(
    Path repositoryRoot,
    String baseReference,
    Path reportFile,
    Path indexFile,
    boolean failOnBreaking) {

    static Options parse(String[] args) {
      Path repositoryRoot = Path.of(".");
      String baseReference = "HEAD";
      Path reportFile = Path.of("api-impact-report.md");
      Path indexFile = Path.of("api-dependency-index.json");
      boolean failOnBreaking = false;

      for (int index = 0; index < args.length; index++) {
        String argument = args[index];
        switch (argument) {
          case "--repo-root" -> repositoryRoot = Path.of(value(args, ++index, argument));
          case "--base-ref" -> baseReference = value(args, ++index, argument);
          case "--report" -> reportFile = Path.of(value(args, ++index, argument));
          case "--index" -> indexFile = Path.of(value(args, ++index, argument));
          case "--fail-on-breaking" -> failOnBreaking = true;
          case "--help", "-h" -> throw new IllegalArgumentException(usage());
          default -> throw new IllegalArgumentException("Unknown argument: " + argument);
        }
      }
      return new Options(repositoryRoot, baseReference, reportFile, indexFile, failOnBreaking);
    }

    private static String value(String[] args, int index, String option) {
      if (index >= args.length) {
        throw new IllegalArgumentException("Missing value for " + option);
      }
      return args[index];
    }

    static String usage() {
      return "Usage: java -jar api-impact-analyzer.jar "
        + "[--repo-root PATH] [--base-ref GIT_REF] [--report PATH] [--index PATH] [--fail-on-breaking]";
    }
  }
}
