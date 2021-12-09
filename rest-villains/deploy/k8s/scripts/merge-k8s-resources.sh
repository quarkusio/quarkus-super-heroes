#!/bin/bash

# Merge k8s resources from the app output (target/kubernetes/openshift.yml and target/kubernetes/kubernetes.yml)
# along with things in deploy/k8s

kind=$1
java_version=$2
app_dir=../../../target/kubernetes
kubernetes_result=../../${kind}-java${java_version}-kubernetes.yml
openshift_result=../../${kind}-java${java_version}-openshift.yml

# Start with what the app generated
cat $app_dir/kubernetes.yml > $kubernetes_result
cat $app_dir/openshift.yml > $openshift_result

# Now append in all .yml files in the deploy/k8s directory
for f in ../*.yml
do
  echo "---" >> $kubernetes_result
  echo "---" >> $openshift_result
  cat "$f" >> $kubernetes_result
  cat "$f" >> $openshift_result
done
