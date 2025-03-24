#!/bin/bash -e

for project in "event-statistics" "rest-fights" "rest-heroes" "rest-villains" "rest-narration" "grpc-locations" "ui-super-heroes"
do
  echo "================================="
  echo "Building ${project}..."
  echo "================================="
  echo


  $project/mvnw -f $project/pom.xml \
    clean verify \
    -Dquarkus.platform.group-id=io.quarkus \
    -Dquarkus.platform.version=999-SNAPSHOT \
    -Dquarkus.http.host=0.0.0.0
done
