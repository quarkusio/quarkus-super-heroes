This directory contains a set of [Hyperfoil](https://hyperfoil.io) benchmarks. An instance of the [`rest-heroes`](..) application [**MUST** be running somewhere](../README.md#running-the-application) prior to executing this benchmark.

Each benchmark can be customized via parameters. All the parameters are described in comments at the beginning of each benchmark file.

| Benchmark file                                     | Benchmark description                              |
|----------------------------------------------------|----------------------------------------------------|
| [`get-all-heroes.hf.yml`](get-all-heroes.hf.yml)   | Runs a `GET` to the `/api/heroes` endpoint         |
| [`get-random-hero.hf.yml`](get-random-hero.hf.yml) | Runs a `GET`  to the `/api/heroes/random` endpoint |
