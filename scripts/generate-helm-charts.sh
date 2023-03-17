#!/bin/bash
#-ex

# Create the deploy/helm files for each java version of each of the Quarkus services
# Then add on the ui-super-heroes

INPUT_DIR=src/main/kubernetes
OUTPUT_DIR=deploy/helm

OUTPUT_HELM_DIR=deploy/helm

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
  local container_tag="${version_tag}-latest"
  local git_server_url="${GITHUB_SERVER_URL:=https://github.com}"
  local git_repo="${GITHUB_REPOSITORY:=quarkusio/quarkus-super-heroes}"
  local github_ref_name="${BRANCH:=${GITHUB_REF_NAME:=main}}"

  if [[ "$deployment_type" == "openshift" ]]; then
    local expose=true
  else
    local expose=false
  fi

  echo "Generating helm resources for $project/$container_tag/$deployment_type"
  rm -rf $project/target

  $project/mvnw -q -f $project/pom.xml versions:set clean package \
    -DskipTests \
    -DnewVersion=$container_tag \
    -Dmaven.compiler.release=$javaVersion \
    -Dquarkus.container-image.tag=$container_tag \
    -Dquarkus.kubernetes.deployment-target=$deployment_type \
    -Dquarkus.kubernetes.version=$container_tag \
    -Dquarkus.kubernetes.ingress.expose=$expose \
    -Dquarkus.kubernetes.annotations.\"app.quarkus.io/vcs-url\"=$GITHUB_SERVER_URL/$GITHUB_REPOSITORY \
    -Dquarkus.kubernetes.annotations.\"app.quarkus.io/vcs-ref\"=$github_ref_name \
    -Dquarkus.openshift.version=$container_tag \
    -Dquarkus.openshift.route.expose=$expose \
    -Dquarkus.openshift.annotations.\"app.openshift.io/vcs-url\"=$GITHUB_SERVER_URL/$GITHUB_REPOSITORY \
    -Dquarkus.openshift.annotations.\"app.openshift.io/vcs-ref\"=$github_ref_name \
    -Dquarkus.knative.version=$container_tag \
    -Dquarkus.knative.labels.\"app.openshift.io/runtime\"=quarkus \
    -Dquarkus.knative.annotations.\"app.openshift.io/vcs-url\"=$GITHUB_SERVER_URL/$GITHUB_REPOSITORY \
    -Dquarkus.knative.annotations.\"app.openshift.io/vcs-ref\"=$github_ref_name \
    -Dquarkus.helm.version=1.0.0 \
    -Dquarkus.helm.name=$project-$container_tag-$deployment_type
}

process_quarkus_project() {
  local project=$1
  local deployment_type=$2
  local version_tag=$3
  local javaVersion=$4
  local kind=$5
  local container_tag="${version_tag}-latest"
  local output_filename="${version_tag}-${deployment_type}"
  local app_generated_input_file="$project/target/kubernetes/${deployment_type}.yml"
  local project_output_file="$project/$OUTPUT_DIR/${output_filename}.yml"
  local all_apps_output_file="$OUTPUT_DIR/${output_filename}.yml"

  local app_generated_helm_chart="$project/target/helm/${deployment_type}/${project}-${container_tag}-${deployment_type}"
  local generated_helm_dir="$project/${OUTPUT_HELM_DIR}/${deployment_type}"

  # 1st do the build
  # The build will generate all the resources for the project
  do_build $project $deployment_type $version_tag $javaVersion $kind

  rm -rf $generated_helm_dir
  mkdir -p $generated_helm_dir

  # Now copy the helm files into the deploy directory (deploy/helm) out of the transient target.
  if [[ -f "$app_generated_input_file" ]]; then
    echo "Copying generated helm chart ($app_generated_helm_chart) to $generated_helm_dir"

    cp -R $app_generated_helm_chart/* $generated_helm_dir

  fi
  if [[ "$project" == "rest-fights" ]]; then
    echo "Copying rest villain and heroes helm charts to the rest fights one "
    mkdir -p "${generated_helm_dir}/charts/rest-villains"
    cp -R rest-villains/${OUTPUT_HELM_DIR}/${deployment_type}/* "${generated_helm_dir}/charts/rest-villains"
    mkdir -p "${generated_helm_dir}/charts/rest-heroes"
    cp -R rest-heroes/${OUTPUT_HELM_DIR}/${deployment_type}/* "${generated_helm_dir}/charts/rest-heroes"
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

#create_monitoring() {
#  local monitoring_name="monitoring"
#
#  echo ""
#  echo "-----------------------------------------"
#  echo "Creating monitoring"
#
#  for deployment_type in "kubernetes" "minikube" "openshift"
#  do
#    local output_file_name="${monitoring_name}-${deployment_type}.yml"
#    local output_file="$OUTPUT_DIR/$output_file_name"
#    local input_dir="$monitoring_name/k8s"
#    create_output_file $output_file
#
#    if [[ -f "$input_dir/base.yml" ]]; then
#      echo "Adding base config from $input_dir/base.yml into $output_file"
#      cat "$input_dir/base.yml" >> $output_file
#    fi
#
#    if [[ -f "$input_dir/${deployment_type}.yml" ]]; then
#      echo "Adding $deployment_type config from $input_dir/${deployment_type}.yml into $output_file"
#      cat "$input_dir/${deployment_type}.yml" >> $output_file
#    fi
#  done
#}

#rm -rf $OUTPUT_DIR/*.yml
KINDS_ALL=("" "native-")
PROJECTS_ALL=("rest-villains" "rest-heroes" "rest-fights" "event-statistics")
DEPLOYMENTS_ALL=("kubernetes" "minikube" "openshift" "knative")

#KINDS_ALL=("")
#PROJECTS_ALL=("rest-villains")
#DEPLOYMENTS_ALL=("kubernetes")

for kind in "${KINDS_ALL[@]}"
do
  if [[ "$kind" == "native-" ]]; then
    javaVersions=(17)
  else
    javaVersions=(11 17)
  fi

  for javaVersion in "${javaVersions[@]}"
  do
    for deployment_type in "${DEPLOYMENTS_ALL[@]}"
    do
      if [[ "$kind" == "native-" ]]; then
        version_tag="native"
      else
        version_tag="${kind}java${javaVersion}"
      fi

      for project in "${PROJECTS_ALL[@]}"
      do
        process_quarkus_project $project $deployment_type $version_tag $javaVersion $kind
      done

#      process_ui_project $deployment_type $version_tag
    done
  done
done

## Handle the monitoring
#create_monitoring
