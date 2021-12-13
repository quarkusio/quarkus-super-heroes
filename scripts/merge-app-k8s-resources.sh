#!/bin/bash -e

# Merge k8s resources from the app output (deploy/k8s/gen/app-*.yml)
# along with things in deploy/k8s

process_project() {
  local proj=$1
  local filename=$2
  local version=$3
  local kind=$4
  local source="$proj/deploy/k8s/gen/app-${kind}java${version}-${filename}.yml"
  local result="$proj/deploy/${kind}java${version}-${filename}.yml"

  # Start with what the app generated
  if [[ -f "$source" ]]; then
    echo "Creating $result from $source"
    set -x
    cp $source $result
    set +x
  fi

  # Now append in all .yml files in the deploy/k8s directory from each project
  for f in $proj/deploy/k8s/*.yml
  do
    echo "Appending $f into $result"
    echo "---" >> $result
    cat "$f" >> $result
  done
}

for project in "rest-villains" #"rest-heroes" "rest-fights" "event-statistics"
do
  # Delete everything currently there
  set -x
  rm -rf $proj/deploy/*.yml
  set +x

  for javaVersion in 11 17
  do
    for kind in "" "native-"
    do
      process_project $project "kubernetes" $javaVersion $kind
      process_project $project "minikube" $javaVersion $kind
      process_project $project "openshift" $javaVersion $kind
    done
  done
done
