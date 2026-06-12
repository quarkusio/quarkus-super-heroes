---
title: Deploying to Kubernetes
description: How to deploy the Quarkus Super Heroes system to Kubernetes, OpenShift, Minikube, and Knative.
layout: page
content-toc: true
---

Pre-built container images for all Quarkus Super Heroes services are available at [`quay.io/quarkus-super-heroes`](https://quay.io/organization/quarkus-super-heroes). You can deploy using pre-built images with Kubernetes descriptors, Helm charts, or directly via the Quarkus Kubernetes Extensions.

## Using Pre-built Images

Deployment descriptors for the full system are provided in the [`deploy/k8s`](https://github.com/quarkusio/quarkus-super-heroes/tree/main/deploy/k8s) directory. There are versions for [OpenShift](https://www.openshift.com), [Minikube](https://quarkus.io/guides/deploying-to-kubernetes#deploying-to-minikube), [Kubernetes](https://www.kubernetes.io), and [Knative](https://knative.dev).

The [Knative](https://knative.dev/docs/) variant can be used on any Knative installation that runs on top of Kubernetes or OpenShift. For OpenShift, you need [OpenShift Serverless](https://docs.openshift.com/serverless/latest/about/about-serverless.html) installed from the OpenShift operator catalog. Using Knative has the benefit that services are scaled down to zero replicas when they are not used.

Pick one of the versions from the table below and deploy the appropriate descriptor:

| Description | OpenShift | Minikube | Kubernetes | Knative |
|-------------|-----------|----------|------------|---------|
| JVM Java 21 | `java21-openshift.yml` | `java21-minikube.yml` | `java21-kubernetes.yml` | `java21-knative.yml` |
| Native      | `native-openshift.yml` | `native-minikube.yml` | `native-kubernetes.yml` | `native-knative.yml` |

For example, to deploy the JVM version to Minikube:

```bash
kubectl apply -f deploy/k8s/java21-minikube.yml
```

Each individual service also has its own descriptors in its `deploy/k8s` directory, allowing you to deploy services independently.

**WARNING:** These descriptors deploy the entire application infrastructure (databases, Kafka, Apicurio Schema Registry, etc.) directly as containers into the cluster. This is suitable for demos and development but is **not production-ready**. For production, use managed services or operators for databases, Kafka (e.g. [Strimzi](https://strimzi.io)), and schema registries (e.g. [Apicurio Registry Operator](https://www.apicur.io/registry/docs/apicurio-registry-operator/1.0.0/index.html)).

## Routing

For **Minikube and Kubernetes**, you will need an [Ingress Controller](https://kubernetes.io/docs/concepts/services-networking/ingress-controllers/) to expose services outside the cluster. At minimum, the [Battle UI]({site.url('/ui-super-heroes')}) and [Fight REST API]({site.url('/rest-fights')}) need to be externally accessible. The [Event Statistics]({site.url('/event-statistics')}) service is available on port `8085`.

For **OpenShift**, Route objects are automatically created for exposed services. No additional configuration is required.

## Monitoring

Deployment descriptors for the Grafana LGTM monitoring stack (Grafana, Loki, Tempo, Prometheus) are provided in the [`deploy/k8s`](https://github.com/quarkusio/quarkus-super-heroes/tree/main/deploy/k8s) directory:

| OpenShift | Minikube | Kubernetes |
|-----------|----------|------------|
| `monitoring-openshift.yml` | `monitoring-minikube.yml` | `monitoring-kubernetes.yml` |

On **OpenShift**, a Route is automatically created for the Grafana dashboard. On **Kubernetes and Minikube**, use `kubectl port-forward` to access Grafana:

```bash
kubectl port-forward svc/grafana 3000:3000
```

## Deploying with Helm

Umbrella Helm charts for the full system are provided in the [`deploy/helm`](https://github.com/quarkusio/quarkus-super-heroes/tree/main/deploy/helm) directory, with a separate chart for each target platform: `openshift`, `minikube`, `kubernetes`, and `knative`.

To deploy the full system using Helm (e.g. JVM Java 21 on Kubernetes):

```bash
helm dependency update deploy/helm/kubernetes
helm install super-heroes deploy/helm/kubernetes -f deploy/helm/kubernetes/values-java21.yaml
```

For native:

```bash
helm dependency update deploy/helm/kubernetes
helm install super-heroes deploy/helm/kubernetes -f deploy/helm/kubernetes/values-native.yaml
```

Replace `kubernetes` with `openshift`, `minikube`, or `knative` as appropriate.

Individual service Helm charts are available in each service's `deploy/helm` directory. For example, to deploy just the Hero service:

```bash
helm install rest-heroes rest-heroes/deploy/helm/kubernetes/ -f rest-heroes/deploy/helm/kubernetes/values-java21.yaml
```

Monitoring Helm charts are also available under `deploy/helm/monitoring-openshift`, `deploy/helm/monitoring-minikube`, and `deploy/helm/monitoring-kubernetes`.

## Deploying Directly via Kubernetes Extensions

Each service can be deployed directly using the Quarkus Kubernetes, OpenShift, Minikube, or Knative extensions. Run the appropriate command **from the service directory**:

| Target Platform        | Command |
|------------------------|---------|
| Kubernetes             | `./mvnw clean package -Dquarkus.profile=kubernetes -Dquarkus.kubernetes.deploy=true -DskipTests` |
| OpenShift              | `./mvnw clean package -Dquarkus.profile=openshift -Dquarkus.container-image.registry=image-registry.openshift-image-registry.svc:5000 -Dquarkus.container-image.group=$(oc project -q) -Dquarkus.kubernetes.deploy=true -DskipTests` |
| Minikube               | `./mvnw clean package -Dquarkus.profile=minikube -Dquarkus.kubernetes.deploy=true -DskipTests` |
| Knative                | `./mvnw clean package -Dquarkus.profile=knative -Dquarkus.kubernetes.deploy=true -DskipTests` |
| Knative (on OpenShift) | `./mvnw clean package -Dquarkus.profile=knative-openshift -Dquarkus.container-image.registry=image-registry.openshift-image-registry.svc:5000 -Dquarkus.container-image.group=$(oc project -q) -Dquarkus.kubernetes.deploy=true -DskipTests` |

**Note:** For non-OpenShift Kubernetes variants (other than Minikube), you will most likely need to [push the image to a container registry](https://quarkus.io/guides/container-image#pushing) by adding `-Dquarkus.container-image.push=true`, and setting `quarkus.container-image.registry`, `quarkus.container-image.group`, and/or `quarkus.container-image.name` as needed.

See the [CI/CD Automation]({site.url('/automation')}) page for details on how these extensions are used to generate the deployment descriptors in the `deploy/k8s` directory.

**WARNING:** The deployment descriptors and Helm charts deploy infrastructure (databases, Kafka, schema registry) as containers. This is **not production-ready**. For production deployments, use managed database services, [Strimzi](https://strimzi.io) for Kafka, and the [Apicurio Registry Operator](https://www.apicur.io/registry/docs/apicurio-registry-operator/1.0.0/index.html) for schema registry.
