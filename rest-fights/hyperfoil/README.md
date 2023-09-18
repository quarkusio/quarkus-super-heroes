This directory contains a set of [Hyperfoil](https://hyperfoil.io) benchmarks. An instance of the [`rest-fights`](..) application [**MUST** be running somewhere](../README.md#running-the-application) prior to executing this benchmark.

Each benchmark can be customized via parameters. All the parameters are described in comments at the beginning of each benchmark file.

| Benchmark file                                             | Benchmark description                                                                     |
|------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| [`get-fights.hf.yml`](get-fights.hf.yml)                   | Runs a `GET` to the `/api/fights` endpoint                                                |
| [`get-random-fighters.hf.yml`](get-random-fighters.hf.yml) | Runs a `GET` to the `/api/fights/randomfighters` endpoint                                 |
| [`perform-fight.hf.yml`](perform-fight.hf.yml)             | Performs a "Perform fight" operation, which runs a sequence of requests in each iteration |
