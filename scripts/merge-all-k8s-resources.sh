#!/bin/bash -e

# Merge k8s resources from all the apps (deploy/*.yml)

deploy_dir=deploy
ui_super_heroes_k8s_dir=ui-super-heroes/deploy/k8s

rm -rf $deploy_dir/*.yml

for kind in "" "native-"
do
  for version in 11 17
  do
    kubernetes_result="$deploy_dir/all-${kind}java${version}-kubernetes.yml"
    minikube_result="$deploy_dir/all-${kind}java${version}-minikube.yml"
    openshift_result="$deploy_dir/all-${kind}java${version}-openshift.yml"

    for proj in "rest-villains" "rest-heroes" #"rest-fights" "event-statistics"
    do
      echo "Processing project $proj for ${kind}java${version}"
      set -x
      cp $proj/deploy/${kind}java${version}-kubernetes.yml $kubernetes_result
      cp $proj/deploy/${kind}java${version}-minikube.yml $minikube_result
      cp $proj/deploy/${kind}java${version}-openshift.yml $openshift_result
      set +x
    done

    echo "Processing project ui-super-heroes for ${kind}java${version}"
    echo "---" >> $kubernetes_result
    echo "---" >> $minikube_result
    echo "---" >> $openshift_result

    cat $ui_super_heroes_k8s_dir/app.yml >> $kubernetes_result
    cat $ui_super_heroes_k8s_dir/app.yml >> $minikube_result
    cat $ui_super_heroes_k8s_dir/app.yml >> $openshift_result

    echo "---" >> $openshift_result
    cat $ui_super_heroes_k8s_dir/route.yml >> $openshift_result
  done
done
