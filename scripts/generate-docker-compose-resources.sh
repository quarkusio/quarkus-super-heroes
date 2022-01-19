#!/bin/bash -ex

# Create the deploy/docker-compose files for each version of each of the Quarkus services
# Then add on the ui-super-heroes

INPUT_DIR=src/main/docker-compose
OUTPUT_DIR=deploy/docker-compose

create_output_file() {
  local output_file=$1

  echo "Creating output file: $output_file"

  touch $output_file
  echo 'version: "3"' >> $output_file
  echo 'services:' >> $output_file
}

create_project_output() {
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
    rm -rf $project_output_file
  fi

  create_output_file $project_output_file

  if [[ -f "$infra_input_file" ]]; then
    cat $infra_input_file >> $project_output_file
  fi

  if [[ -f "$project_input_file" ]]; then
    cat $project_input_file >> $project_output_file

    if [[ "$project" == "event-statistics" || "$project" == "ui-super-heroes" ]]; then
      cat $project_input_file >> $all_apps_output_file
    fi
  fi

  if [[ "$project" == "rest-fights" ]]; then
    # Need to process/create the downstream version
    # With the rest-villains/rest-heroes apps & their dependencies
    local downstream_infra_file="$project/$INPUT_DIR/infra-downstream.yml"
    local downstream_project_output_file="$project/$OUTPUT_DIR/${kind}java${javaVersion}-all-downstream.yml"

    if [[ -f "$downstream_project_output_file" ]]; then
      rm -rf $downstream_project_output_file
      create_output_file $downstream_project_output_file
    fi

    if [[ -d "$project/deploy/db-init" ]]; then
      cp -r $project/deploy/db-init deploy
    fi

    if [[ -f "$downstream_infra_file" ]]; then
      cat $downstream_infra_file >> $downstream_project_output_file
      cat $downstream_infra_file | sed 's/..\/..\/..\//..\/..\//g' >> $all_apps_output_file
    fi

    if [[ -f "$project_input_file" ]]; then
      cat $project_input_file >> $downstream_project_output_file
      cat $project_input_file >> $all_apps_output_file
    fi

    if [[ -f "rest-villains/$INPUT_DIR/$input_file_name" ]]; then
      cat rest-villains/$INPUT_DIR/$input_file_name >> $downstream_project_output_file
      cat rest-villains/$INPUT_DIR/$input_file_name >> $all_apps_output_file
    fi

    if [[ -f "rest-heroes/$INPUT_DIR/$input_file_name" ]]; then
      cat rest-heroes/$INPUT_DIR/$input_file_name >> $downstream_project_output_file
      cat rest-heroes/$INPUT_DIR/$input_file_name >> $all_apps_output_file
    fi
  fi
}

create_monitoring() {
  local monitoring_name="monitoring"

  echo ""
  echo "-----------------------------------------"
  echo "Creating monitoring"

  mkdir -p $OUTPUT_DIR/$monitoring_name
  cp $monitoring_name/config/*.yml $OUTPUT_DIR/$monitoring_name
  cp $monitoring_name/docker-compose/*.yml $OUTPUT_DIR
}

rm -rf $OUTPUT_DIR/*.yml
rm -rf $OUTPUT_DIR/monitoring
rm -rf deploy/db-init

for project in "rest-villains" "rest-heroes" "rest-fights" "event-statistics" "ui-super-heroes"
do
  for javaVersion in 11 17
  do
    for kind in "" "native-"
    do
      if [[ "$project" == "ui-super-heroes" ]]; then
        filename="app"
      else
        filename="${kind}java${javaVersion}"
      fi

      create_project_output $project $filename $javaVersion $kind
    done
  done
done

## Now handle the monitoring
create_monitoring
