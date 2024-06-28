import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class calculateModules {
	private static final String DEFAULT_TIMEFRAME = "24 hours";
	private static final List<String> MODULES = List.of(
		"event-statistics",
		"grpc-locations",
		"rest-fights",
		"rest-heroes",
		"rest-narration",
		"rest-villains",
		"ui-super-heroes"
	);

	private static boolean shouldIncludeFile(String fileName) {
    return Objects.nonNull(fileName) &&
	    !fileName.trim().isEmpty() &&
	    !fileName.trim().contains("deploy/");
  }

	private static Set<String> getChangedFiles(String timeframe) throws IOException, InterruptedException {
		var process = ProcessBuilder.startPipeline(
			List.of(
				new ProcessBuilder("git", "log", "--pretty=format:", "--since=\"%s ago\"".formatted(timeframe), "--name-only"),
				new ProcessBuilder("sort")
			)
		).getLast();

		process.waitFor();

		try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			var changedDirs = reader.lines()
				.filter(calculateModules::shouldIncludeFile)
				.map(fullFileName -> fullFileName.trim().split("/")[0])
				.collect(Collectors.toCollection(LinkedHashSet::new));

			changedDirs.retainAll(MODULES);
			return changedDirs;
		}
	}

	private static String createJson(Set<String> changedModules) {
		return "[%s]".formatted(
			changedModules.stream()
				.flatMap(calculateModules::convertModuleToJson)
				.collect(Collectors.joining(","))
		);
	}

	private static Stream<String> convertModuleToJson(String moduleName) {
		var moduleJson = "{ \"name\": \"%s\" }".formatted(moduleName);

		return "rest-narration".equals(moduleName) ?
		       Stream.of(
						 moduleJson,
			       "{ \"name\": \"%s\", \"openai-type\": \"azure-openai\" }".formatted(moduleName)
		       ) :
		       Stream.of(moduleJson);
	}

	public static void main(String... args) throws IOException, InterruptedException {
		var timeframe = ((args != null) && (args.length == 1)) ?
		                Optional.ofNullable(args[0]).map(String::trim).filter(s -> !s.isEmpty()).orElse(DEFAULT_TIMEFRAME) :
		                DEFAULT_TIMEFRAME;
		var changedFiles = getChangedFiles(timeframe);
		var json = createJson(changedFiles);
		System.out.println(json);
	}
}