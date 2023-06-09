#!/bin/bash -u
# -ex

# Create the deploy/k8s files for each java version of each of the Quarkus services
# Then add on the ui-super-heroes

OUTPUT_DIR_K8S=deploy/k8s
OUTPUT_DIR_HELM=deploy/helm

DEPLOYMENT_TYPES=("kubernetes" "minikube" "openshift" "knative")
PROJECTS=("rest-villains" "rest-heroes" "rest-fights" "event-statistics")
KINDS=("java" "native")

JAVA_VERSIONS=(17)
NATIVE_JAVA_VERSION=17
# Static initialization of version tuples
# Each tuple contains, the kind (java or native), the java version and the version tag.
# Native builds do not use all java versions, but a single one defined in NATIVE_JAVA_VERSION
VERSION_TUPLES=( )
for kind in "${KINDS[@]}"
do
  if [[ "$kind" == "native" ]]; then
    VERSION_TUPLES+=( "$kind,$NATIVE_JAVA_VERSION,$kind" )
  else
    for javaVersion in "${JAVA_VERSIONS[@]}"
    do
      VERSION_TUPLES+=( "$kind,$javaVersion,$kind$javaVersion" )
    done
  fi
done

create_output_file() {
  local output_file=$1

  if [[ ! -f "$output_file" ]]; then
    # echo "Creating output file: $output_file"
    touch $output_file
  fi
}

do_build() {
  local project=$1
  local version_tag=$2
  local javaVersion=$3
  local kind=$4

  local container_tag="${version_tag}-latest"
  local git_server_url="${GITHUB_SERVER_URL:=https://github.com}"
  local git_repo="${GITHUB_REPOSITORY:=quarkusio/quarkus-super-heroes}"
  local github_ref_name="${BRANCH:=${GITHUB_REF_NAME:=main}}"

  if [[ "$kind" == "native" ]]; then
    local mem_limit="128Mi"
    local mem_request="32Mi"
  else
    local mem_limit="768Mi"
    local mem_request="256Mi"
  fi

  echo "Building app resources for $project/$version_tag"
  rm -rf $project/target

  printf -v deployment_types_str '%s,' "${DEPLOYMENT_TYPES[@]}"

  $project/mvnw -q -f $project/pom.xml versions:set clean package \
    -DskipTests \
    -DnewVersion=$container_tag \
    -Dmaven.compiler.release=$javaVersion \
    -Dquarkus.liquibase-mongodb.migrate-at-start=false \
    -Dquarkus.container-image.tag=$container_tag \
    -Dquarkus.kubernetes.deployment-target=$deployment_types_str \
    -Dquarkus.kubernetes.version=$container_tag \
    -Dquarkus.kubernetes.ingress.expose=false \
    -Dquarkus.kubernetes.resources.limits.memory=$mem_limit \
    -Dquarkus.kubernetes.resources.requests.memory=$mem_request \
    -Dquarkus.kubernetes.annotations.\"app.quarkus.io/vcs-url\"=$GITHUB_SERVER_URL/$GITHUB_REPOSITORY \
    -Dquarkus.kubernetes.annotations.\"app.quarkus.io/vcs-ref\"=$github_ref_name \
    -Dquarkus.openshift.version=$container_tag \
    -Dquarkus.openshift.route.expose=true \
    -Dquarkus.openshift.resources.limits.memory=$mem_limit \
    -Dquarkus.openshift.resources.requests.memory=$mem_request \
    -Dquarkus.openshift.annotations.\"app.openshift.io/vcs-url\"=$GITHUB_SERVER_URL/$GITHUB_REPOSITORY \
    -Dquarkus.openshift.annotations.\"app.openshift.io/vcs-ref\"=$github_ref_name \
    -Dquarkus.knative.version=$container_tag \
    -Dquarkus.knative.labels.\"app.openshift.io/runtime\"=quarkus \
    -Dquarkus.knative.resources.limits.memory=$mem_limit \
    -Dquarkus.knative.resources.requests.memory=$mem_request \
    -Dquarkus.knative.annotations.\"app.openshift.io/vcs-url\"=$GITHUB_SERVER_URL/$GITHUB_REPOSITORY \
    -Dquarkus.knative.annotations.\"app.openshift.io/vcs-ref\"=$github_ref_name \
    -Dquarkus.helm.version=1.0.0 \
    -Dquarkus.helm.name=$project

}

process_kubernetes_resources(){
  local project=$1
  local version_tag=$2
  local deployment_type=$3

  echo "Processing k8s resources for ${project}:${deployment_type}"

  local output_filename="${version_tag}-${deployment_type}"
  local project_k8s_file="$project/$OUTPUT_DIR_K8S/${output_filename}.yml"
  local all_apps_k8s_file="$OUTPUT_DIR_K8S/${output_filename}.yml"
  local mvn_k8s_file="$project/target/kubernetes/${deployment_type}.yml"

  mkdir -p $OUTPUT_DIR_K8S
  mkdir -p $project/$OUTPUT_DIR_K8S
  rm -rf $project_k8s_file

  create_output_file $project_k8s_file
  create_output_file $all_apps_k8s_file

  # Now merge the generated resources to the top level (deploy/k8s)
  if [[ -f "$mvn_k8s_file" ]]; then
    echo "Adding ${deployment_type} resources from ($mvn_k8s_file) to $project_k8s_file and $all_apps_k8s_file"

    cat $mvn_k8s_file >> $project_k8s_file
    cat $mvn_k8s_file >> $all_apps_k8s_file

  else
    echo "****** The mvn_k8s_file file does not exit! $mvn_k8s_file  ************"
    exit 255
  fi

  if [[ "$project" == "rest-fights" ]]; then
    # Create a descriptor for all of the downstream services (rest-heroes and rest-villains)
    local all_downstream_output_file="$project/$OUTPUT_DIR_K8S/${output_filename}-all-downstream.yml"
    local villains_output_file="rest-villains/$OUTPUT_DIR_K8S/${output_filename}.yml"
    local heroes_output_file="rest-heroes/$OUTPUT_DIR_K8S/${output_filename}.yml"

    rm -rf $all_downstream_output_file

    create_output_file $all_downstream_output_file

    echo "Adding ${deployment_type} rest-fights resources ${mvn_k8s_file}, ${villains_output_file}, and $heroes_output_file to $all_downstream_output_file"
    cat $villains_output_file >> $all_downstream_output_file
    cat $heroes_output_file >> $all_downstream_output_file
    cat $mvn_k8s_file >> $all_downstream_output_file
  fi

  if [ "${DEBUG}" = true ]; then
     # Order the resources for testing purposes
    echo "DEBUG: Sorting kubernetes resources at $project_k8s_file"
    jbang yamlsort@someth2say -yamlpath "kind" -yamlpath "metadata.name" -i "${project_k8s_file}" > "${project_k8s_file}.sort";
    mv -f "${project_k8s_file}.sort" "${project_k8s_file}"
  fi
}

process_helm_resources(){
    local project=$1
    local deployment_type=$2
    local version_tag=$3

    local mvn_helm_dir="$project/target/helm/${deployment_type}/$project"
    local project_helm_dir="$project/${OUTPUT_DIR_HELM}/${deployment_type}"
    local all_apps_helm_dir="${OUTPUT_DIR_HELM}/${deployment_type}"

    # Now copy the helm files into the deploy directory (deploy/helm) out of the transient target.
    if [[ -d "$mvn_helm_dir" ]]; then
      rm -rf $project_helm_dir
      mkdir -p $project_helm_dir
      echo "Copying generated helm chart $mvn_helm_dir to $project_helm_dir"
      cp -R $mvn_helm_dir/* $project_helm_dir
    else
      echo "ERROR: The expected location of the maven generated helm chart is not found: $mvn_helm_dir"
      exit
    fi

#    TODO: For rest-fights We should generate two different helm charts, one with "all-downstream" resources, and another without.
#    if [[ "$project" == "rest-fights" ]]; then
#      echo "Copying rest villain and heroes ${deployment_type} helm charts to the rest fights one "
#      mkdir -p "${project_helm_dir}/charts/rest-villains"
#      cp -R rest-villains/${OUTPUT_DIR_HELM}/${deployment_type}/* "${project_helm_dir}/charts/rest-villains"
#      mkdir -p "${project_helm_dir}/charts/rest-heroes"
#      cp -R rest-heroes/${OUTPUT_DIR_HELM}/${deployment_type}/* "${project_helm_dir}/charts/rest-heroes"
#    fi

#    echo "Copying generated helm chart $project_helm_dir to $all_apps_helm_dir"
#    rm -rf $all_apps_helm_dir
#    mkdir -p $all_apps_helm_dir
#    cp -R $project_helm_dir/* $all_apps_helm_dir

    # Execute templates into a k8s-like resources file.
    # This is optional, and only enabled for testing purposes
    if [ "${DEBUG}" = true ]; then
      local project_helm_generated_dir=$project/deploy/helm/generated
      mkdir -p $project_helm_generated_dir
      local project_helm_generated_file=$project_helm_generated_dir/${version_tag}-$deployment_type.yml
      echo "DEBUG: Applying and sorting helm resources for $project_helm_dir to $project_helm_generated_file"
      helm template $project $project_helm_dir -f scripts/values-${version_tag}.yml > $project_helm_generated_file || exit
      jbang yamlsort@someth2say -yamlpath "kind" -yamlpath "metadata.name" -i "$project_helm_generated_file" > "${project_helm_generated_file}.sort";
      mv -f "${project_helm_generated_file}.sort" "$project_helm_generated_file"
    fi
}

process_ui_project() {
  local deployment_type=$1
  local version_tag=$2
  local project="ui-super-heroes"
  local INPUT_DIR="src/main/kubernetes"
  local project_input_directory="$project/$INPUT_DIR"
  local input_file="$project_input_directory/${deployment_type}.yml"
  local project_k8s_file="$project/$OUTPUT_DIR_K8S/app-${deployment_type}.yml"
  local all_apps_k8s_file="$OUTPUT_DIR_K8S/${version_tag}-${deployment_type}.yml"

  rm -rf $project_k8s_file
  mkdir -p $project/$OUTPUT_DIR_K8S

  if [[ -f "$input_file" ]]; then
    create_output_file $project_k8s_file
    echo "Adding UI resources at $input_file to $project_k8s_file and $all_apps_k8s_file"
    cat $input_file >> $project_k8s_file
    cat $input_file >> $all_apps_k8s_file
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
    local output_file="$OUTPUT_DIR_K8S/$output_file_name"
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

main(){
  rm -rf $OUTPUT_DIR_K8S/*.yml
  rm -rf OUTPUT_DIR_HELM/*.yml

  local tag
  local deployment_type
  local project
  local kind
  local javaVersion
  local version_tag

  echo "${VERSION_TUPLES[@]}"

  for tag in "${VERSION_TUPLES[@]}"
  do
    OLDIFS="$IFS"; IFS=','; set -- $tag;
      kind=$1
      javaVersion=$2
      version_tag=$3
    IFS="$OLDIFS"

    for project in "${PROJECTS[@]}"
    do
      do_build $project $version_tag $javaVersion $kind

    done

    for deployment_type in "${DEPLOYMENT_TYPES[@]}"
    do
      for project in "${PROJECTS[@]}"
      do
        process_kubernetes_resources $project $version_tag $deployment_type

        process_helm_resources $project $deployment_type $version_tag

      done
    done

    for deployment_type in "${DEPLOYMENT_TYPES[@]}"
    do
      process_ui_project $deployment_type $version_tag
    done

  done

  ## Handle the monitoring
  create_monitoring
}

main
