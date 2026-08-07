package edu.eci.arsw.blacklist;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Complete-scan implementation that creates one Java 21 virtual thread per provider.
 */
public final class VirtualThreadBlackListSearch implements BlackListSearch {
    private final List<BlackListProvider> providers;

    public VirtualThreadBlackListSearch(List<BlackListProvider> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
    }

    @Override
    public SearchResult search(String ipAddress, int alarmThreshold) {
        Objects.requireNonNull(ipAddress, "ipAddress");
        if (alarmThreshold <= 0) {
            throw new IllegalArgumentException("alarmThreshold must be greater than zero");
        }

        long startedAt = System.nanoTime();
        List<Callable<Integer>> tasks = providers.stream()
                .<Callable<Integer>>map(provider -> () ->
                        provider.isBlacklisted(ipAddress) ? provider.id() : null)
                .toList();

        List<Integer> matches = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> futures = executor.invokeAll(tasks);
            for (Future<Integer> future : futures) {
                Integer providerId = future.get();
                if (providerId != null) {
                    matches.add(providerId);
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Blacklist search was interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("A blacklist provider consultation failed", ex.getCause());
        }

        matches.sort(Comparator.naturalOrder());
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
        return new SearchResult(ipAddress, matches, providers.size(), elapsed);
    }
}
