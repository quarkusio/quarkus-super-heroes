# rest-fights

Helm chart for rest-fights
## Configuration

The following table lists the configurable parameters and their default values.

| Parameter | Description | Default |
|  ---  |  ---  |  ---  |
| `app.envs.QUARKUS_OTEL_K8S_RESOURCE_CONTAINER_NAME` |   | rest-fights |
| `app.envs.QUARKUS_OTEL_K8S_RESOURCE_DEPLOYMENT_NAME` |   | rest-fights |
| `app.envs.quarkus.microcks.default.grpc.host` |   |   |
| `app.envs.quarkus.microcks.default.grpc.port` |   |   |
| `app.envs.quarkus.microcks.default.http.host` |   |   |
| `app.envs.quarkus.microcks.default.http.port` |   |   |
| `app.image` | The container image to use. | quay.io/quarkus-super-heroes/rest-fights:java21-latest |
| `app.livenessProbe.failureThreshold` | The failure threshold to use. | 3 |
| `app.livenessProbe.httpGet.path` | The http path to use for the probe. | /q/health/live |
| `app.livenessProbe.httpGet.scheme` | The http schema to use for the probe. | HTTP |
| `app.livenessProbe.initialDelaySeconds` | The amount of time to wait before starting to probe. | 5 |
| `app.livenessProbe.periodSeconds` | The period in which the action should be called. | 10 |
| `app.livenessProbe.successThreshold` | The success threshold to use. | 1 |
| `app.livenessProbe.timeoutSeconds` | The amount of time to wait for each action. | 10 |
| `app.ports.http` | The port number to use for http. | 8082 |
| `app.ports.http1` | The http port to use for the probe. | 8082 |
| `app.readinessProbe.failureThreshold` | The failure threshold to use. | 3 |
| `app.readinessProbe.httpGet.path` | The http path to use for the probe. | /q/health/ready |
| `app.readinessProbe.httpGet.scheme` | The http schema to use for the probe. | HTTP |
| `app.readinessProbe.initialDelaySeconds` | The amount of time to wait before starting to probe. | 5 |
| `app.readinessProbe.periodSeconds` | The period in which the action should be called. | 10 |
| `app.readinessProbe.successThreshold` | The success threshold to use. | 1 |
| `app.readinessProbe.timeoutSeconds` | The amount of time to wait for each action. | 10 |

Specify each parameter using the `--set key=value[,key=value]` argument to `helm install`.
Alternatively, a YAML file that specifies the values for the above parameters can be provided while installing the chart. For example,
```
$ helm install --name chart-name -f values.yaml .
```
> **Tip**: You can use the default [values.yaml](values.yaml)
