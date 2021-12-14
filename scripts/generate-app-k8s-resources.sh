#!/bin/bash -e

do_build() {
  local proj=$1
  local version=$2
  local kind=$3
  local tag="${kind}java${version}-latest"

  echo "Generating app config for tag $tag"
  set -x
  rm -rf $proj/target

  $proj/mvnw -f $proj/pom.xml versions:set clean package -DskipTests \
    -DnewVersion=$tag \
    -Dmaven.compiler.release=17 \
    -Dquarkus.container-image.tag=$tag \
    -Dquarkus.kubernetes.version=$tag \
    -Dquarkus.kubernetes.annotations.\"app.quarkus.io/vcs-url\"=$GITHUB_SERVER_URL/$GITHUB_REPOSITORY \
    -Dquarkus.kubernetes.annotations.\"app.quarkus.io/vcs-ref\"=$GITHUB_REF_NAME \
    -Dquarkus.openshift.version=$tag \
    -Dquarkus.openshift.annotations.\"app.openshift.io/vcs-url\"=$GITHUB_SERVER_URL/$GITHUB_REPOSITORY \
    -Dquarkus.openshift.annotations.\"app.openshift.io/vcs-ref\"=$GITHUB_REF_NAME

  set +x
}

copy_resource() {
  local proj=$1
  local filename=$2
  local version=$3
  local kind=$4

  mkdir -p $proj/deploy/k8s/gen

  set -x
  cp $proj/target/kubernetes/${filename}.yml $proj/deploy/k8s/gen/app-${kind}java${version}-${filename}.yml
  set +x
}

process_project() {
  local proj=$1
  local version=$2
  local kind=$3

  do_build $proj $version $kind

  for deployment_type in "kubernetes" "minikube" "openshift"
  do
    copy_resource $project $deployment_type $version $kind
  done
}

for project in "rest-villains" "rest-heroes" "rest-fights" "event-statistics"
do
  rm -rf $proj/deploy/k8s/gen

  for javaVersion in 11 17
  do
    for kind in "" "native-"
    do
      process_project $project $javaVersion $kind
    done
  done
done
