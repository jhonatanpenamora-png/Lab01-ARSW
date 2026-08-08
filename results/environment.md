# Measurement environment

| Item | Value |
|---|---|
| Operating system | Microsoft Windows 11 Home Single Language, version 10.0.26200, 64-bit |
| CPU | 13th Gen Intel(R) Core(TM) i5-13450HX |
| Logical processors | 16 |
| RAM | 31.71 GiB |
| JDK (runtime used to execute the benchmark) | OpenJDK 24.0.2 (Eclipse Temurin 24.0.2+12) |
| Bytecode target | Java 21 (`maven.compiler.release=21` in `pom.xml`) |
| Maven | Apache Maven 3.9.16 |
| Measurement date | 2026-08-08 |
| IP address queried | 202.24.34.55 |
| Providers | 100 deterministic mock providers |
| Alarm threshold | 5 |
| Warm-up executions per configuration | 2 |
| Measured executions per configuration | 5 |

## Methodology

All ten configurations were executed on this machine, sequentially, one `mvn exec:java` invocation per configuration, using the commands documented in `README.md`. Each invocation runs the requested strategy for `warmups` iterations (discarded) and then for `measuredRuns` iterations, measuring the elapsed time already captured inside `SearchResult.elapsed()` (built from `System.nanoTime()` inside the strategy itself, so it excludes JVM/Maven startup cost).

`BenchmarkRunner` verifies that every measured run for a configuration returns the exact same matching providers before accepting the average; it fails fast otherwise. Speedup is computed by reading the most recent `SEQUENTIAL` row already stored in `results/results.csv` with the same `simulateIo` value — it is never hardcoded, and it is `N/A` until that baseline row exists.

Results for such a small, local workload are sensitive to the OS scheduler, JIT warm-up, CPU frequency scaling, and background processes; they should not be read as a general-purpose CPU-bound benchmark.

Note: the installed JDK on this machine is 24.0.2, not 21. The project still targets Java 21 bytecode (`maven.compiler.release=21`), and `mvn clean test` passes, but `java -version` does not literally print `21` as step 1 of the guide expects — worth flagging to the team/instructor if strict JDK 21 is required.
