package edu.eci.arsw.blacklist;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class BenchmarkRunner {
    private static final Path RESULTS_FILE = Path.of("results", "results.csv");
    private static final String CSV_HEADER =
            "timestamp,strategy,poolSize,simulateIo,warmups,measuredRuns,avgMs,minMs,maxMs,speedup,matches,consultedProviders";
    private static final int PROVIDER_COUNT = 100;
    private static final int ALARM_THRESHOLD = 5;

    private BenchmarkRunner() {
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            throw new IllegalArgumentException(
                    "Uso: <SEQUENTIAL|FIXED|VIRTUAL> <ipAddress> <simulateIo> <warmups> <measuredRuns> [poolSize]");
        }

        String mode = args[0].toUpperCase(Locale.ROOT);
        String ipAddress = args[1];
        boolean simulateIo = Boolean.parseBoolean(args[2]);
        int warmups = Integer.parseInt(args[3]);
        int measuredRuns = Integer.parseInt(args[4]);
        int poolSize = mode.equals("FIXED") ? Integer.parseInt(requireArg(args, 5, mode)) : 0;

        List<BlackListProvider> providers = ProviderFactory.create(PROVIDER_COUNT, simulateIo);
        BlackListSearch search = createSearch(mode, providers, poolSize);

        for (int i = 0; i < warmups; i++) {
            search.search(ipAddress, ALARM_THRESHOLD);
        }

        long[] elapsedNanos = new long[measuredRuns];
        SearchResult lastResult = null;
        for (int i = 0; i < measuredRuns; i++) {
            SearchResult result = search.search(ipAddress, ALARM_THRESHOLD);
            if (lastResult != null && !result.matchingProviderIds().equals(lastResult.matchingProviderIds())) {
                throw new IllegalStateException(
                        "Dos corridas medidas produjeron coincidencias distintas; revisa la implementación antes de reportar estos números.");
            }
            elapsedNanos[i] = result.elapsed().toNanos();
            lastResult = result;
        }

        double avgMs = average(elapsedNanos) / 1_000_000.0;
        double minMs = min(elapsedNanos) / 1_000_000.0;
        double maxMs = max(elapsedNanos) / 1_000_000.0;
        Double speedup = mode.equals("SEQUENTIAL") ? null : findSequentialSpeedup(simulateIo, avgMs);

        printSummary(mode, poolSize, simulateIo, warmups, measuredRuns, avgMs, minMs, maxMs, speedup, lastResult);
        appendCsvRow(mode, poolSize, simulateIo, warmups, measuredRuns, avgMs, minMs, maxMs, speedup, lastResult);
    }

    private static String requireArg(String[] args, int index, String mode) {
        if (args.length <= index) {
            throw new IllegalArgumentException("El modo " + mode + " requiere el tamaño del pool como último argumento.");
        }
        return args[index];
    }

    private static BlackListSearch createSearch(String mode, List<BlackListProvider> providers, int poolSize) {
        return switch (mode) {
            case "SEQUENTIAL" -> new SequentialBlackListSearch(providers);
            case "FIXED" -> new FixedPoolBlackListSearch(providers, poolSize);
            case "VIRTUAL" -> new VirtualThreadBlackListSearch(providers);
            default -> throw new IllegalArgumentException("Modo desconocido: " + mode + ". Usa SEQUENTIAL, FIXED o VIRTUAL.");
        };
    }

    private static double average(long[] values) {
        long sum = 0;
        for (long value : values) {
            sum += value;
        }
        return (double) sum / values.length;
    }

    private static long min(long[] values) {
        long result = values[0];
        for (long value : values) {
            result = Math.min(result, value);
        }
        return result;
    }

    private static long max(long[] values) {
        long result = values[0];
        for (long value : values) {
            result = Math.max(result, value);
        }
        return result;
    }

    private static void printSummary(String mode, int poolSize, boolean simulateIo, int warmups, int measuredRuns,
                                      double avgMs, double minMs, double maxMs, Double speedup, SearchResult result) {
        System.out.printf("Mode: %s%s%n", mode, poolSize > 0 ? " (poolSize=" + poolSize + ")" : "");
        System.out.printf("IP: %s | simulateIo=%s | warmups=%d | measuredRuns=%d%n",
                result.ipAddress(), simulateIo, warmups, measuredRuns);
        System.out.printf("Matches: %s | Consulted: %d%n", result.matchingProviderIds(), result.consultedProviders());
        System.out.printf(Locale.ROOT, "Avg: %.3f ms | Min: %.3f ms | Max: %.3f ms%n", avgMs, minMs, maxMs);
        System.out.printf("Speedup vs sequential: %s%n",
                speedup == null ? "N/A (ejecuta primero SEQUENTIAL con el mismo simulateIo)" : String.format(Locale.ROOT, "%.2fx", speedup));
    }

    /**
     * Speedup se calcula contra la última corrida SEQUENTIAL registrada en results.csv
     * con el mismo valor de simulateIo. No se inventa: si todavía no existe esa fila, se reporta null.
     */
    private static Double findSequentialSpeedup(boolean simulateIo, double currentAvgMs) {
        if (!Files.exists(RESULTS_FILE)) {
            return null;
        }
        try {
            List<String> lines = Files.readAllLines(RESULTS_FILE);
            double sequentialAvgMs = -1;
            for (String line : lines) {
                String[] columns = line.split(",", -1);
                if (columns.length < 12 || !"SEQUENTIAL".equals(columns[1])) {
                    continue;
                }
                if (Boolean.parseBoolean(columns[3]) != simulateIo) {
                    continue;
                }
                sequentialAvgMs = Double.parseDouble(columns[6]);
            }
            return sequentialAvgMs > 0 ? sequentialAvgMs / currentAvgMs : null;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void appendCsvRow(String mode, int poolSize, boolean simulateIo, int warmups, int measuredRuns,
                                      double avgMs, double minMs, double maxMs, Double speedup, SearchResult result) {
        try {
            Files.createDirectories(RESULTS_FILE.getParent());
            boolean isNew = !Files.exists(RESULTS_FILE) || Files.size(RESULTS_FILE) == 0;
            String row = String.join(",",
                    DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                    mode,
                    String.valueOf(poolSize),
                    String.valueOf(simulateIo),
                    String.valueOf(warmups),
                    String.valueOf(measuredRuns),
                    String.format(Locale.ROOT, "%.3f", avgMs),
                    String.format(Locale.ROOT, "%.3f", minMs),
                    String.format(Locale.ROOT, "%.3f", maxMs),
                    speedup == null ? "" : String.format(Locale.ROOT, "%.2f", speedup),
                    String.valueOf(result.matchingProviderIds().size()),
                    String.valueOf(result.consultedProviders()));

            try (var writer = Files.newBufferedWriter(RESULTS_FILE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                if (isNew) {
                    writer.write(CSV_HEADER);
                    writer.newLine();
                }
                writer.write(row);
                writer.newLine();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
