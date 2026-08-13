#!/usr/bin/env bash
set -euo pipefail

analyzer_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
target_dir="${analyzer_dir}/target"
main_classes="${target_dir}/classes"
test_classes="${target_dir}/test-classes"
jdk_bin="${API_IMPACT_JDK_BIN:-}"

if [[ -n "${jdk_bin}" ]]; then
  javac_command="${jdk_bin}/javac"
  jar_command="${jdk_bin}/jar"
  java_command="${jdk_bin}/java"
else
  javac_command="javac"
  jar_command="jar"
  java_command="java"
fi

mkdir -p "${main_classes}" "${test_classes}"

mapfile -t main_sources < <(find "${analyzer_dir}/src/main/java" -name '*.java' -type f | sort)
"${javac_command}" --release 17 -encoding UTF-8 -d "${main_classes}" "${main_sources[@]}"

"${jar_command}" --create \
  --file "${target_dir}/api-impact-analyzer.jar" \
  --main-class io.quarkus.sample.superheroes.impact.ApiImpactAnalyzer \
  -C "${main_classes}" .

mapfile -t test_sources < <(find "${analyzer_dir}/src/test/java" -name '*.java' -type f | sort)
"${javac_command}" --release 17 -encoding UTF-8 -cp "${main_classes}" -d "${test_classes}" "${test_sources[@]}"
"${java_command}" -ea -cp "${main_classes}:${test_classes}" io.quarkus.sample.superheroes.impact.AnalyzerSelfTest

echo "Built ${target_dir}/api-impact-analyzer.jar"
