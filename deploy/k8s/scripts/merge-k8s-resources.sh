#!/bin/bash

# Merge k8s resources from all the apps (deploy/*.yml)

deploy_dir=deploy

rm -rf $deploy_dir/*.yml

for kind in "jvm" "native"
do
  for version in 11 17
  do
    for proj in "rest-villains" "rest-heroes" "rest-fights" "event-statistics"
    do
      echo "Processing project $proj for ${kind}-java${versopn}"
      echo "---" >> $deploy_dir/all-${kind}-java${version}-kubernetes.yml
      echo "---" >> $deploy_dir/all-${kind}-java${version}-openshift.yml
      cat $proj/deploy/${kind}-java${version}-kubernetes.yml >> $deploy_dir/all-${kind}-java${version}-kubernetes.yml
      cat $proj/deploy/${kind}-java${version}-openshift.yml >> $deploy_dir/all-${kind}-java${version}-openshift.yml
    done

    echo "---" >> $deploy_dir/all-${kind}-java${version}-kubernetes.yml
    echo "---" >> $deploy_dir/all-${kind}-java${version}-openshift.yml

    cat ui-super-heroes/deploy/app-kubernetes.yml >> $deploy_dir/all-${kind}-java${version}-kubernetes.yml
    cat ui-super-heroes/deploy/app-kubernetes.yml >> $deploy_dir/all-${kind}-java${version}-openshift.yml

    echo "---" >> $deploy_dir/all-${kind}-java${version}-openshift.yml
    cat ui-super-heroes/deploy/route.yml >> $deploy_dir/all-${kind}-java${version}-openshift.yml
  done
done

# Start with what the app generated
#cat $app_dir/kubernetes.yml > $kubernetes_result
#cat $app_dir/openshift.yml > $openshift_result

# Now append in all .yml files in the deploy/k8s directory
#for f in deploy/k8s/*.yml
#do
#  echo "---" >> $kubernetes_result
#  echo "---" >> $openshift_result
#  cat "$f" >> $kubernetes_result
#  cat "$f" >> $openshift_result
#done
