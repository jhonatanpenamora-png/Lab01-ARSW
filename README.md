# ARSW Laboratory 1 — Concurrency with Java 21

## Team members

| Student | GitHub username |
|---|---|
| Juan Sebastian Murcia Yanquen | JuanMurciaY|
| Jhonatan Peña Mora | jhonatanpenamora-png |
| STUDENT 3 NAME | USERNAME |

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
| Operating system | Windows 11 25H2 |
| CPU | Intel Core i5-13450HX |
| Logical processors | 16 |
| RAM | 31.71 GiB |
| Java | Oracle Java 21.0.11 LTS |
| Maven | Apache Maven 3.9.16 |
| Measurement date | 2026-08-05 |
| Warm-up executions | 2 |
| Measured executions | 5 |

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
| No simulated I/O | Sequential | — | 0.019 | 0.009 | 0.036 | 1.00 | 7 | 100 |
| No simulated I/O | Fixed pool | 2 | 1.111 | 0.561 | 2.342 | 0.02 | 7 | 100 |
| No simulated I/O | Fixed pool | 4 | 1.255 | 0.584 | 2.639 | 0.01 | 7 | 100 |
| No simulated I/O | Fixed pool | 8 | 1.924 | 1.101 | 3.255 | 0.01 | 7 | 100 |
| No simulated I/O | Virtual threads | — | 0.880 | 0.675 | 1.219 | 0.02 | 7 | 100 |
| Simulated I/O | Sequential | — | 11886.456 | 11407.918 | 12751.884 | 1.00 | 7 | 100 |
| Simulated I/O | Fixed pool | 2 | 5525.378 | 5511.525 | 5561.022 | 2.15 | 7 | 100 |
| Simulated I/O | Fixed pool | 4 | 2828.355 | 2824.969 | 2831.752 | 4.20 | 7 | 100 |
| Simulated I/O | Fixed pool | 8 | 1476.013 | 1472.657 | 1482.231 | 8.05 | 7 | 100 |
| Simulated I/O | Virtual threads | — | 207.918 | 202.533 | 216.285 | 57.17 | 7 | 100 |

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

With simulated I/O, the average decreased from `5525.378 ms` to `2828.355 ms`. Four threads could consult more providers while other threads were waiting.

Without simulated I/O, the average increased from `1.111 ms` to `1.255 ms` because the local work was too small to benefit from additional threads.

#### 6. What changed from 4 to 8 threads?

With simulated I/O, the average decreased from `2828.355 ms` to `1476.013 ms`.

Without simulated I/O, the average increased to `1.924 ms`. This showed that adding threads does not always make a program faster.

#### 7. Was the improvement proportional?

It was approximately proportional in the blocking scenario, but not exact because providers had different latency values and thread coordination also had a cost.

There was no improvement in the local scenario because the operations finished very quickly.

#### 8. What costs does the fixed pool introduce?

The program must create tasks, schedule threads, wait for results, collect matches, and sort the final list. These costs are useful when operations spend time waiting but can be unnecessary for small local operations.

#### 9. What happens if the pool is too large?

A very large pool can consume more memory and produce more context switches. It can also send too many requests to an external service.

### Virtual threads

#### 10. When did virtual threads provide the clearest benefit?

They provided the clearest benefit with simulated blocking I/O. Their average was `207.918 ms`, compared with `11886.456 ms` for the sequential strategy.

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

Virtual threads achieved a `57.17` speedup with simulated I/O. In contrast, all concurrent strategies were slower than the sequential strategy without simulated I/O.

#### 18. What are the limitations of this experiment?

The experiment used one computer, one IP address, 100 simulated providers, and five measured executions. It did not include real network failures, multiple users, timeouts, or external-service limits.

## Team conclusion

The results showed that the best concurrency strategy depends on the type of work performed by the application. Without simulated I/O, the sequential strategy was the fastest because each provider performed a very small local calculation. In this scenario, creating and coordinating concurrent tasks added more work than it removed.

With simulated blocking I/O, concurrency produced a clear improvement. Fixed pools became faster as their size increased, reaching a speedup of `8.05` with eight threads. Virtual threads produced the best result with an average of `207.918 ms` and a speedup of `57.17`.

Based on this evidence, we recommend virtual threads for systems that perform many independent blocking calls. However, the application should still use timeouts and request limits to protect external services. For small local operations, the sequential strategy is easier to understand and more efficient. A fixed pool remains useful when the architecture needs explicit control over concurrency. These conclusions are limited to this experiment and should not be applied to every production system without additional measurements.

## Individual conclusions

### Student 1

**Name:** Juan Sebastian Murcia Yanquen

While implementing the fixed thread pool strategy, I learned that using more threads does not always improve performance. The blocking experiment showed good results because several providers could be consulted while other threads were waiting. However, the local scenario was faster with sequential execution because the work was very small. I also learned that tasks should return their own results instead of modifying shared data. From an architectural point of view, I believe a fixed pool is useful when the system needs to control the number of operations running at the same time.

### Student 2

**Name:** Jhonatan Peña Mora

While implementing the virtual threads strategy, I understood why they are useful for applications that spend a lot of time waiting for external responses. They allowed every provider to have its own task without creating the same cost as a large number of platform threads. The experiment also showed that virtual threads do not make small local calculations automatically faster. I learned that the concurrency model must be selected according to the workload and not only because a technology is newer. Request limits and timeouts are still necessary in a real system.

### Student 3

**Name:** 

The benchmark helped me understand the importance of measuring software decisions instead of assuming that one strategy is always better. Warm-up executions, repeated measurements, and raw data made the comparison easier to explain. I also learned that speedup must use the sequential result from the same scenario. The results showed a clear difference between local and blocking workloads. From an architectural perspective, performance is only one part of the decision because complexity, resource limits, correctness, and maintainability must also be considered.

## Contribution evidence

| Student | GitHub username | Main contribution | Relevant commits |
|---|---|---|---|
|Juan Sebastian Murcia Yanquen | JuanMurciaY | Fixed pool implementation, tests, and analysis | COMMIT HASHES |
| Jhonatan Peña Mora | jhonatanpenamora-png | Virtual threads implementation, tests, and analysis | 54993dc 51ad8e3 |
| STUDENT 3 NAME | USERNAME | Benchmark runner, results, and documentation | COMMIT HASHES |

## Use of artificial intelligence

| Tool | Purpose | Activities | Validation |
|---|---|---|---|
| OpenAI Codex | Support during implementation and documentation | Code explanation, test suggestions, benchmark guidance, and README drafting | The team reviewed the code, executed automated tests, compared all strategies, and checked the benchmark results |

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
