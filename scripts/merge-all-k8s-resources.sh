#!/bin/bash

# Merge k8s resources from all the apps (deploy/*.yml)

deploy_dir=deploy

rm -rf $deploy_dir/*.yml

for kind in "" "native-"
do
  for version in 11 17
  do
    kubernetes_result="$deploy_dir/all-${kind}java${version}-kubernetes.yml"
    openshift_result="$deploy_dir/all-${kind}java${version}-openshift.yml"

    for proj in "rest-villains" #"rest-heroes" "rest-fights" "event-statistics"
    do
      echo "Processing project $proj for ${kind}java${version}"
      set -x
      cp $proj/deploy/${kind}java${version}-kubernetes.yml $kubernetes_result
      cp $proj/deploy/${kind}java${version}-openshift.yml $openshift_result
      set +x
    done

    echo "Processing project ui-super-heroes for ${kind}java${version}"
    echo "---" >> $kubernetes_result
    echo "---" >> $openshift_result

    cat ui-super-heroes/deploy/app.yml >> $kubernetes_result
    cat ui-super-heroes/deploy/app.yml >> $openshift_result

    echo "---" >> $openshift_result
    cat ui-super-heroes/deploy/route.yml >> $openshift_result
  done
done
