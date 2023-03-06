#!/bin/bash -e

# 0 - event-statistics
# 1 - villains
# 2 - heroes
# 3 - fights
# 4 - ui
# 5 - apicurio
statuses=("0", "0", "0", "0", "0", "0")

max_tries=100
tries=1
interval=2

# Display help
help() {
  echo "This script watches the services started by Docker Compose and reports when all services are up."
  echo
  echo "Syntax: watch-services.sh [options]"
  echo "options:"
  echo "  -h                           Prints this help message"
  echo "  -i <interval_between_tries>  The interval in seconds between tries. Default is 2."
  echo "  -m <max_tries>               The number of times to retry to see if the services are up. Default is 100."
}

exit_abnormal() {
  echo
  help
  exit 1
}

get_status() {
  local port=$1
  local index=$2
  local path=$3

  case "$index" in
    0) local service_name="event-statistics" ;;
    1) local service_name="rest-villains" ;;
    2) local service_name="rest-heroes" ;;
    3) local service_name="rest-fights" ;;
    4) local service_name="ui-super-heroes" ;;
    5) local service_name="apicurio" ;;
  esac

  local url="http://localhost:${port}${path}"
  local command="curl -s -o /dev/null -w \"%{http_code}\" $url"
  local result=$($command)
  echo "Pinging $service_name: $url"
#  echo "result of \"$command\"=\"$result\""
  statuses[$index]=$result
}

get_statuses() {
  if [[ "${statuses[0]}" != "\"200\"" ]]; then
    get_status 8085 0 "/q/health/ready"
  fi

  if [[ "${statuses[1]}" != "\"200\"" ]]; then
    get_status 8084 1 "/q/health/ready"
  fi

  if [[ "${statuses[2]}" != "\"200\"" ]]; then
    get_status 8083 2 "/q/health/ready"
  fi

  if [[ "${statuses[3]}" != "\"200\"" ]]; then
    get_status 8082 3 "/q/health/ready"
  fi

  if [[ "${statuses[4]}" != "\"200\"" ]]; then
    get_status 8080 4 "/"
  fi

  if [[ "${statuses[5]}" != "\"200\"" ]]; then
    get_status 8086 5 "/health/ready"
  fi
}

print_statuses() {
  echo "event_stats_status=${statuses[0]}"
  echo "villains_status=${statuses[1]}"
  echo "heroes_status=${statuses[2]}"
  echo "fights_status=${statuses[3]}"
  echo "ui_status=${statuses[4]}"
  echo "apicurio=${statuses[5]}"
}

# Process the input options
while getopts "hi::m::" option; do
  case $option in
    m) max_tries=$OPTARG
       ;;

    h) help
       exit 1
       ;;

    i) interval=$OPTARG
       ;;

    *) exit_abnormal
       ;;
  esac
done

while [[ "${statuses[0]}" != "\"200\"" ]] || [[ "${statuses[1]}" != "\"200\"" ]] || [[ "${statuses[2]}" != "\"200\"" ]] || [[ "${statuses[3]}" != "\"200\"" ]] || [[ "${statuses[4]}" != "\"200\"" ]] || [[ "${statuses[5]}" != "\"200\"" ]]
do
  if [[ "$tries" -gt $max_tries ]]; then
    break
  fi

  echo ""
  echo "-----------------------------------"
  echo "[$(date +"%m/%d/%Y %T")]: Try #$tries"
  echo "-----------------------------------"
  get_statuses
#  print_statuses
  ((tries++))
  sleep $interval
done

echo ""

if [[ "$tries" -gt $max_tries ]]; then
  echo "Not all services started within $max_tries tries!!!!!!!"
  print_statuses

  exit 1
else
  echo "All services are now up :)"
  echo "Super Heroes UI: http://localhost:8080"
  echo "Event stats: http://localhost:8085"
  echo "Apicurio Schema Registry: http://localhost:8086"
  echo
  echo "---- Monitoring (if started) ----"
  echo "Prometheus: http://localhost:9090"
  echo "Jaeger: http://localhost:16686"
fi

exit 0
