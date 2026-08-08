# ARSW Laboratory 1 — Concurrency with Java 21

## Team members

| Student                       | GitHub username |
|-------------------------------|---|
| Juan Sebastian Murcia Yanquen | JuanMurciaY|
| Jhonatan Peña Mora            | jhonatanpenamora-png |
| Jhonatan Madero Riaño         | jhonatanmadero |

## Project description

This laboratory compares different ways of consulting 100 blacklist providers:

- Sequential execution.
- Fixed pool with 2 threads.
- Fixed pool with 4 threads.
- Fixed pool with 8 threads.
- Java 21 virtual threads.

The main purpose was to understand when concurrency improves performance and when it only adds unnecessary work.

All strategies perform a complete scan and must return the same matching providers.

For the IP address `202.24.34.55`, the result was:

```text
[10, 23, 36, 49, 62, 75, 88]
```

All strategies found 7 matches and consulted 100 providers.

## Requirements

- Java 21.
- Maven 3.9 or newer.
- Git.

Verify the installed versions with:

```bash
java -version
mvn -version
```

## Running the tests

Run all automated tests:

```bash
mvn clean test
```

Run only the fixed-pool tests:

```bash
mvn "-Dtest=FixedPoolBlackListSearchTest" test
```

Run only the virtual-threads tests:

```bash
mvn "-Dtest=VirtualThreadBlackListSearchTest" test
```

## Implemented strategies

### Sequential strategy

This strategy consults one provider at a time. It is simple and works well when each operation is small and does not spend time waiting.

### Fixed thread pool

`FixedPoolBlackListSearch` uses `Executors.newFixedThreadPool`.

A separate task is created for every provider, but the pool controls how many tasks can run at the same time. Pools of 2, 4, and 8 threads were tested.

Each task returns its result instead of modifying a shared list. After all tasks finish, the matches are collected and sorted.

### Virtual threads

`VirtualThreadBlackListSearch` uses `Executors.newVirtualThreadPerTaskExecutor`.

A virtual thread is created for every provider. This strategy is useful when many operations spend most of their time waiting for network, database, or external-service responses.

## Automated verification

The tests verify that:

- All strategies return the same matching providers.
- All strategies consult 100 providers.
- Results are returned in ascending order.
- Results do not contain duplicates.
- Pools of 2, 4, and 8 threads work correctly.
- Virtual threads produce the same result as the sequential strategy.
- Invalid pool sizes are rejected.
- The sequential result is deterministic.

The tests do not compare execution time because performance can change depending on the computer and the processes running in the background.

## Benchmark commands

Each configuration uses two warm-up executions and five measured executions.

### Sequential

```bash
mvn exec:java "-Dexec.args=SEQUENTIAL 202.24.34.55 false 2 5"
mvn exec:java "-Dexec.args=SEQUENTIAL 202.24.34.55 true 2 5"
```

### Fixed pool

```bash
mvn exec:java "-Dexec.args=FIXED 202.24.34.55 false 2 5 2"
mvn exec:java "-Dexec.args=FIXED 202.24.34.55 false 2 5 4"
mvn exec:java "-Dexec.args=FIXED 202.24.34.55 false 2 5 8"
```

```bash
mvn exec:java "-Dexec.args=FIXED 202.24.34.55 true 2 5 2"
mvn exec:java "-Dexec.args=FIXED 202.24.34.55 true 2 5 4"
mvn exec:java "-Dexec.args=FIXED 202.24.34.55 true 2 5 8"
```

### Virtual threads

```bash
mvn exec:java "-Dexec.args=VIRTUAL 202.24.34.55 false 2 5"
mvn exec:java "-Dexec.args=VIRTUAL 202.24.34.55 true 2 5"
```

## Measurement environment

| Item | Value |
|---|---|
| Operating system | Microsoft Windows 11 Home Single Language, version 10.0.26200, 64-bit |
| CPU | 13th Gen Intel(R) Core(TM) i5-13450HX |
| Logical processors | 16 |
| RAM | 31.71 GiB |
| JDK (runtime used to execute the benchmark) | OpenJDK 24.0.2 (Eclipse Temurin) |
| Bytecode target | Java 21 (`maven.compiler.release=21`) |
| Maven | Apache Maven 3.9.16 |
| Measurement date | 2026-08-08 |
| Warm-up executions | 2 |
| Measured executions | 5 |

Full details, including the methodology notes, are in [`results/environment.md`](results/environment.md).

The raw measurements are available in:

```text
results/results.csv
```

## Results

Speedup was calculated with:

```text
Speedup = sequential average / strategy average
```

| Scenario | Strategy | Pool | Average ms | Minimum ms | Maximum ms | Speedup | Matches | Consulted |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| No simulated I/O | Sequential | — | 0.027 | 0.007 | 0.104 | 1.00 | 7 | 100 |
| No simulated I/O | Fixed pool | 2 | 0.640 | 0.557 | 0.751 | 0.04 | 7 | 100 |
| No simulated I/O | Fixed pool | 4 | 0.975 | 0.834 | 1.116 | 0.03 | 7 | 100 |
| No simulated I/O | Fixed pool | 8 | 1.210 | 1.052 | 1.439 | 0.02 | 7 | 100 |
| No simulated I/O | Virtual threads | — | 0.692 | 0.526 | 1.104 | 0.04 | 7 | 100 |
| Simulated I/O | Sequential | — | 10943.796 | 10940.827 | 10951.540 | 1.00 | 7 | 100 |
| Simulated I/O | Fixed pool | 2 | 5886.840 | 5872.646 | 5907.056 | 1.86 | 7 | 100 |
| Simulated I/O | Fixed pool | 4 | 2825.884 | 2825.498 | 2826.593 | 3.87 | 7 | 100 |
| Simulated I/O | Fixed pool | 8 | 1559.585 | 1552.394 | 1571.257 | 7.02 | 7 | 100 |
| Simulated I/O | Virtual threads | — | 199.636 | 199.370 | 200.208 | 54.82 | 7 | 100 |

Raw data (including timestamps) is in [`results/results.csv`](results/results.csv), produced automatically by `BenchmarkRunner` on every run — nothing in this table was typed by hand.

## Analysis

### Correctness

#### 1. How did we verify that all strategies are equivalent?

We compared every strategy against the sequential result using automated tests. We verified the matches, classification, order, duplicates, and number of consulted providers.

#### 2. Why can concurrent tasks finish in a different order?

The operating system decides when each thread runs. Some providers also take longer than others, so their tasks may not finish in provider-ID order.

#### 3. How did we prevent lost or duplicated matches?

Each task returned its own result. Only the main thread added matches to the final list, so worker threads did not modify shared data.

#### 4. Why must correctness be verified before performance?

A faster strategy is not useful if it skips providers or returns a different answer. All strategies must solve the same problem before their times can be compared.

### Fixed thread pool

#### 5. What changed from 2 to 4 threads?

With simulated I/O, the average decreased from `5886.840 ms` to `2825.884 ms`. Four threads could consult more providers while other threads were waiting.

Without simulated I/O, the average increased from `0.640 ms` to `0.975 ms` because the local work was too small to benefit from additional threads.

#### 6. What changed from 4 to 8 threads?

With simulated I/O, the average decreased from `2825.884 ms` to `1559.585 ms`.

Without simulated I/O, the average increased to `1.210 ms`. This showed that adding threads does not always make a program faster.

#### 7. Was the improvement proportional?

It was approximately proportional in the blocking scenario, but not exact because providers had different latency values and thread coordination also had a cost.

There was no improvement in the local scenario because the operations finished very quickly.

#### 8. What costs does the fixed pool introduce?

The program must create tasks, schedule threads, wait for results, collect matches, and sort the final list. These costs are useful when operations spend time waiting but can be unnecessary for small local operations.

#### 9. What happens if the pool is too large?

A very large pool can consume more memory and produce more context switches. It can also send too many requests to an external service.

### Virtual threads

#### 10. When did virtual threads provide the clearest benefit?

They provided the clearest benefit with simulated blocking I/O. Their average was `199.636 ms`, compared with `10943.796 ms` for the sequential strategy.

#### 11. Why are virtual threads useful for blocking operations?

When a virtual thread waits, the JVM can use the available platform thread for another task. This allows many waiting operations to be managed efficiently.

#### 12. Why did virtual threads not improve the local workload?

Virtual threads do not add processor cores. The local calculation was very small, so creating and coordinating tasks cost more than executing them sequentially.

#### 13. What limitations do virtual threads still have?

They still create tasks and use memory. A system must also control the number of requests sent to databases or external services to avoid overloading them.

### Architectural decision

#### 14. What do we recommend for blocking external calls?

We recommend virtual threads when the system performs many independent and blocking calls. Timeouts and request limits should still be used.

#### 15. What do we recommend for a small local workload?

We recommend the sequential strategy because it was simpler and faster in this experiment.

#### 16. When is a fixed pool preferable?

A fixed pool is useful when the system needs direct control over the number of simultaneous operations or when an external resource has a limited capacity.

#### 17. What evidence supports this decision?

Virtual threads achieved a `54.82` speedup with simulated I/O. In contrast, all concurrent strategies were slower than the sequential strategy without simulated I/O.

#### 18. What are the limitations of this experiment?

The experiment used one computer, one IP address, 100 simulated providers, and five measured executions. It did not include real network failures, multiple users, timeouts, or external-service limits.

## Team conclusion

The results showed that the best concurrency strategy depends on the type of work performed by the application. Without simulated I/O, the sequential strategy was the fastest because each provider performed a very small local calculation. In this scenario, creating and coordinating concurrent tasks added more work than it removed.

With simulated blocking I/O, concurrency produced a clear improvement. Fixed pools became faster as their size increased, reaching a speedup of `7.02` with eight threads. Virtual threads produced the best result with an average of `199.636 ms` and a speedup of `54.82`.

Based on this evidence, we recommend virtual threads for systems that perform many independent blocking calls. However, the application should still use timeouts and request limits to protect external services. For small local operations, the sequential strategy is easier to understand and more efficient. A fixed pool remains useful when the architecture needs explicit control over concurrency. These conclusions are limited to this experiment and should not be applied to every production system without additional measurements.

## Individual conclusions

### Student 1

**Name:** Juan Sebastian Murcia Yanquen

While implementing the fixed thread pool strategy, I learned that using more threads does not always improve performance. The blocking experiment showed good results because several providers could be consulted while other threads were waiting. However, the local scenario was faster with sequential execution because the work was very small. I also learned that tasks should return their own results instead of modifying shared data. From an architectural point of view, I believe a fixed pool is useful when the system needs to control the number of operations running at the same time.

### Student 2

**Name:** Jhonatan Peña Mora

While implementing the virtual threads strategy, I understood why they are useful for applications that spend a lot of time waiting for external responses. They allowed every provider to have its own task without creating the same cost as a large number of platform threads. The experiment also showed that virtual threads do not make small local calculations automatically faster. I learned that the concurrency model must be selected according to the workload and not only because a technology is newer. Request limits and timeouts are still necessary in a real system.

### Student 3

**Name:** Jhonatan Madero Riaño

The benchmark helped me understand the importance of measuring software decisions instead of assuming that one strategy is always better. Warm-up executions, repeated measurements, and raw data made the comparison easier to explain. I also learned that speedup must use the sequential result from the same scenario. The results showed a clear difference between local and blocking workloads. From an architectural perspective, performance is only one part of the decision because complexity, resource limits, correctness, and maintainability must also be considered.

While completing this part I found that the results table already in this README had been written before `BenchmarkRunner` actually supported the `FIXED` and `VIRTUAL` strategies, so those numbers could not have come from a real run. I extended `BenchmarkRunner` to select a strategy from the command line, run the warm-ups and measured executions, and append every run to `results/results.csv` automatically instead of typing numbers by hand. I then re-ran all ten configurations on my own machine and replaced the results with the real ones. This showed me that a results table is only trustworthy if you can point to the exact command and file that produced each number.

## Contribution evidence

| Student | GitHub username | Main contribution | Relevant commits |
|---|---|---|---|
|Juan Sebastian Murcia Yanquen | JuanMurciaY | Fixed pool implementation, tests, and analysis | COMMIT HASHES |
| Jhonatan Peña Mora | jhonatanpenamora-png | Virtual threads implementation, tests, and analysis | 54993dc 51ad8e3 |
| Jhonatan Madero Riaño | jhonatanmadero | Benchmark runner, results, and documentation | 089e46f de572ec |

## Use of artificial intelligence

| Tool | Purpose | Activities | Validation |
|---|---|---|---|
| OpenAI Codex | Support during implementation and documentation | Code explanation, test suggestions, benchmark guidance, and README drafting | The team reviewed the code, executed automated tests, compared all strategies, and checked the benchmark results |
| Claude Code | Support during Part 3 implementation and documentation | Extended `BenchmarkRunner` with strategy selection, warm-up/measured runs and automatic CSV export; ran all ten benchmark configurations on the author's machine; corrected results, analysis figures, and environment data that did not match an actual run | Jhonatan Madero Riaño reviewed the implementation, re-ran `mvn clean test` and the benchmark commands himself, and can explain how each number in `results/results.csv` was produced |

AI was used as a support tool. Every team member reviewed their contribution and is responsible for understanding and explaining the submitted code.

## Final verification

Before submission, run:

```bash
git status
mvn clean test
```

Create and publish the final tag:

```bash
git tag -a lab-1-final -m "Laboratory 1 final submission"
git push origin main
git push origin lab-1-final
```
