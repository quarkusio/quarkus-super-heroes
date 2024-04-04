This directory contains a set of [Hyperfoil](https://hyperfoil.io) benchmarks. An instance of the [`rest-villains`](..) application [**MUST** be running somewhere](../README.md#running-the-application) prior to executing this benchmark.

Each benchmark can be customized via parameters. All the parameters are described in comments at the beginning of each benchmark file.

| Benchmark file                                           | Benchmark description                                                                     |
|----------------------------------------------------------|-------------------------------------------------------------------------------------------|
| [`get-all-villains.hf.yml`](get-all-villains.hf.yml)     | Runs a `GET` to the `/api/villains` endpoint                                              |
| [`get-random-villain.hf.yml`](get-random-villain.hf.yml) | Runs a `GET`  to the `/api/villains/random` endpoint                                      |
