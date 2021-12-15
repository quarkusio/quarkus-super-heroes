#!/bin/bash -e

# Create the deploy/docker-compose files for each version of each of the Quarkus services

INPUT_DIR=partials/docker-compose
OUTPUT_DIR=deploy/docker-compose

create_output_file() {
  local output_file=$1

  echo "Creating output file: $output_file"

  touch $output_file
  echo 'version: "3"' >> $output_file
  echo 'services:' >> $output_file
}

create_output() {
  local project=$1
  local filename=$2
  local javaVersion=$3
  local kind=$4
  local infra_input_file="$project/$INPUT_DIR/infra.yml"
  local input_file_name="$filename.yml"
  local version_file_name="${kind}java${javaVersion}.yml"
  local all_apps_output_file="$OUTPUT_DIR/$version_file_name"
  local project_input_file="$project/$INPUT_DIR/$input_file_name"
  local project_output_file="$project/$OUTPUT_DIR/$input_file_name"

  echo ""
  echo "-----------------------------------------"
  echo "Creating output for project $project"

  if [[ ! -f "$all_apps_output_file" ]]; then
    create_output_file $all_apps_output_file
  fi

  if [[ -f "$project_output_file" ]]; then
    set -x
    rm -rf $project_output_file
    set +x
  fi

  create_output_file $project_output_file

  if [[ -f "$infra_input_file" ]]; then
    set -x
    cat $infra_input_file >> $project_output_file
    set +x
  fi

  if [[ -f "$project_input_file" ]]; then
    set -x
    cat $project_input_file >> $project_output_file
    set +x

    if [[ "$project" == "event-statistics" || "$project" == "ui-super-heroes" ]]; then
      set -x
      cat $project_input_file >> $all_apps_output_file
      set +x
    fi
  fi

  if [[ "$project" == "rest-fights" ]]; then
    # Need to process/create the downstream version
    # With the rest-villains/rest-heroes apps & their dependencies
    local downstream_infra_file="$project/$INPUT_DIR/infra-downstream.yml"
    local downstream_project_output_file="$project/$OUTPUT_DIR/${kind}java${javaVersion}-all-downstream.yml"

    create_output_file $downstream_project_output_file

    if [[ -d "$project/deploy/db-init" ]]; then
      set -x
      cp -r $project/deploy/db-init deploy
      set +x
    fi

    if [[ -f "$downstream_infra_file" ]]; then
      set -x
      cat $downstream_infra_file >> $downstream_project_output_file
      cat $downstream_infra_file | sed 's/..\/..\/..\//..\/..\//g' >> $all_apps_output_file
      set +x
    fi

    if [[ -f "$project_input_file" ]]; then
      set -x
      cat $project_input_file >> $downstream_project_output_file
      cat $project_input_file >> $all_apps_output_file
      set +x
    fi

    if [[ -f "rest-villains/$INPUT_DIR/$input_file_name" ]]; then
      set -x
      cat rest-villains/$INPUT_DIR/$input_file_name >> $project_output_file
      cat rest-villains/$INPUT_DIR/$input_file_name >> $all_apps_output_file
      set +x
    fi

    if [[ -f "rest-heroes/$INPUT_DIR/$input_file_name" ]]; then
      set -x
      cat rest-heroes/$INPUT_DIR/$input_file_name >> $project_output_file
      cat rest-heroes/$INPUT_DIR/$input_file_name >> $all_apps_output_file
      set +x
    fi
  fi
}

set -x
rm -rf $OUTPUT_DIR/*.yml
rm -rf deploy/db-init
set +x

for javaVersion in 11 17
do
  for kind in "" "native-"
  do
    for project in "rest-villains" "rest-heroes" "rest-fights" "event-statistics"
    do
      create_output $project "${kind}java${javaVersion}" $javaVersion $kind
    done

    create_output "ui-super-heroes" "app" $javaVersion $kind
  done
done
