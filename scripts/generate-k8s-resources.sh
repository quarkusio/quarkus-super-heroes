#!/bin/bash -ex

# Create the deploy/k8s files for each java version of each of the Quarkus services
# Then add on the ui-super-heroes

INPUT_DIR=src/main/kubernetes
OUTPUT_DIR=deploy/k8s

create_output_file() {
  local output_file=$1

  if [[ ! -f "$output_file" ]]; then
    echo "Creating output file: $output_file"
    touch $output_file
  fi
}

do_build() {
  local project=$1
  local deployment_type=$2
  local version_tag=$3
  local javaVersion=$4
  local kind=$5
  local container_tag="${version_tag}-latest-rhbq-2.13"
  local git_server_url="${GITHUB_SERVER_URL:=https://github.com}"
  local git_repo="${GITHUB_REPOSITORY:=quarkusio/quarkus-super-heroes}"
  local github_ref_name="${BRANCH:=${GITHUB_REF_NAME:=rhbq-2.13}}"

  if [[ "$kind" == "native-" ]]; then
    local mem_limit="128Mi"
    local mem_request="32Mi"
  else
    local mem_limit="768Mi"
    local mem_request="256Mi"
  fi

  if [[ "$deployment_type" == "openshift" ]]; then
    local expose=true
  else
    local expose=false
  fi

  echo "Generating app resources for $project/$container_tag/$deployment_type"
  rm -rf $project/target

  $project/mvnw -f $project/pom.xml versions:set clean package \
    -DskipTests \
    -DnewVersion=$container_tag \
    -Dmaven.compiler.release=$javaVersion \
    -Dquarkus.container-image.tag=$container_tag \
    -Dquarkus.kubernetes.deployment-target=$deployment_type \
    -Dquarkus.kubernetes.version=$container_tag \
    -Dquarkus.kubernetes.ingress.expose=$expose \
    -Dquarkus.kubernetes.resources.limits.memory=$mem_limit \
    -Dquarkus.kubernetes.resources.requests.memory=$mem_request \
    -Dquarkus.kubernetes.annotations.\"app.quarkus.io/vcs-url\"=$GITHUB_SERVER_URL/$GITHUB_REPOSITORY \
    -Dquarkus.kubernetes.annotations.\"app.quarkus.io/vcs-ref\"=$github_ref_name \
    -Dquarkus.openshift.version=$container_tag \
    -Dquarkus.openshift.route.expose=$expose \
    -Dquarkus.openshift.resources.limits.memory=$mem_limit \
    -Dquarkus.openshift.resources.requests.memory=$mem_request \
    -Dquarkus.openshift.annotations.\"app.openshift.io/vcs-url\"=$GITHUB_SERVER_URL/$GITHUB_REPOSITORY \
    -Dquarkus.openshift.annotations.\"app.openshift.io/vcs-ref\"=$github_ref_name \
    -Dquarkus.knative.version=$container_tag \
    -Dquarkus.knative.labels.\"app.openshift.io/runtime\"=quarkus \
    -Dquarkus.knative.resources.limits.memory=$mem_limit \
    -Dquarkus.knative.resources.requests.memory=$mem_request \
    -Dquarkus.knative.annotations.\"app.openshift.io/vcs-url\"=$GITHUB_SERVER_URL/$GITHUB_REPOSITORY \
    -Dquarkus.knative.annotations.\"app.openshift.io/vcs-ref\"=$github_ref_name
}

process_quarkus_project() {
  local project=$1
  local deployment_type=$2
  local version_tag=$3
  local javaVersion=$4
  local kind=$5
  local output_filename="${version_tag}-${deployment_type}"
  local app_generated_input_file="$project/target/kubernetes/${deployment_type}.yml"
  local project_output_file="$project/$OUTPUT_DIR/${output_filename}.yml"
  local all_apps_output_file="$OUTPUT_DIR/${output_filename}.yml"

  # 1st do the build
  # The build will generate all the resources for the project
  do_build $project $deployment_type $version_tag $javaVersion $kind

  rm -rf $project_output_file

  create_output_file $project_output_file
  create_output_file $all_apps_output_file

  # Now merge the generated resources to the top level (deploy/k8s)
  if [[ -f "$app_generated_input_file" ]]; then
    echo "Copying app generated input ($app_generated_input_file) to $project_output_file and $all_apps_output_file"
    cat $app_generated_input_file >> $project_output_file
    cat $app_generated_input_file >> $all_apps_output_file
  fi

  if [[ "$project" == "rest-fights" ]]; then
    # Create a descriptor for all of the downstream services (rest-heroes and rest-villains)
    local all_downstream_output_file="$project/$OUTPUT_DIR/${output_filename}-all-downstream.yml"
    local villains_output_file="rest-villains/$OUTPUT_DIR/${output_filename}.yml"
    local heroes_output_file="rest-heroes/$OUTPUT_DIR/${output_filename}.yml"

    rm -rf $all_downstream_output_file

    create_output_file $all_downstream_output_file

    echo "Copying ${app_generated_input_file}, ${villains_output_file}, and $heroes_output_file to $all_downstream_output_file"
    cat $villains_output_file >> $all_downstream_output_file
    cat $heroes_output_file >> $all_downstream_output_file
    cat $app_generated_input_file >> $all_downstream_output_file
  fi
}

process_ui_project() {
  local deployment_type=$1
  local version_tag=$2
  local project="ui-super-heroes"
  local project_input_directory="$project/$INPUT_DIR"
  local input_file="$project_input_directory/${deployment_type}.yml"
  local project_output_file="$project/$OUTPUT_DIR/app-${deployment_type}.yml"
  local all_apps_output_file="$OUTPUT_DIR/${version_tag}-${deployment_type}.yml"

  rm -rf $project_output_file

  if [[ -f "$input_file" ]]; then
    create_output_file $project_output_file
    echo "Copying app input ($input_file) to $project_output_file and $all_apps_output_file"
    cat $input_file >> $project_output_file
    cat $input_file >> $all_apps_output_file
  fi
}

create_monitoring() {
  local monitoring_name="monitoring"

  echo ""
  echo "-----------------------------------------"
  echo "Creating monitoring"

  for deployment_type in "kubernetes" "minikube" "openshift"
  do
    local output_file_name="${monitoring_name}-${deployment_type}.yml"
    local output_file="$OUTPUT_DIR/$output_file_name"
    local input_dir="$monitoring_name/k8s"
    create_output_file $output_file

    if [[ -f "$input_dir/base.yml" ]]; then
      echo "Adding base config from $input_dir/base.yml into $output_file"
      cat "$input_dir/base.yml" >> $output_file
    fi

    if [[ -f "$input_dir/${deployment_type}.yml" ]]; then
      echo "Adding $deployment_type config from $input_dir/${deployment_type}.yml into $output_file"
      cat "$input_dir/${deployment_type}.yml" >> $output_file
    fi
  done
}

rm -rf $OUTPUT_DIR/*.yml

for kind in "" "native-"
do
  if [[ "$kind" == "native-" ]]; then
    javaVersions=(17)
  else
    javaVersions=(11 17)
  fi

  for javaVersion in ${javaVersions[@]}
  do
    for deployment_type in "kubernetes" "minikube" "openshift" "knative"
    do
      if [[ "$kind" == "native-" ]]; then
        version_tag="native"
      else
        version_tag="${kind}java${javaVersion}"
      fi

      for project in "rest-villains" "rest-heroes" "rest-fights" "event-statistics"
      do
        process_quarkus_project $project $deployment_type $version_tag $javaVersion $kind
      done

      process_ui_project $deployment_type $version_tag
    done
  done
done

## Handle the monitoring
create_monitoring
