package edu.eci.arsw.blacklist;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Implementación que realiza un escaneo completo usando un grupo fijo de hilos para ejecutar las tareas.
 */
public final class FixedPoolBlackListSearch implements BlackListSearch {
    private final List<BlackListProvider> providers;
    private final int poolSize;

    public FixedPoolBlackListSearch(List<BlackListProvider> providers, int poolSize) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
        if (poolSize <= 0) {
            throw new IllegalArgumentException("El tamaño del grupo de hilos debe ser mayor que cero para que pueda funcionar.");
        }
        this.poolSize = poolSize;
    }

    @Override
    public SearchResult search(String ipAddress, int alarmThreshold) {
        Objects.requireNonNull(ipAddress, "ipAddress");
        if (alarmThreshold <= 0) {
            throw new IllegalArgumentException("El límite definido para activar la alarma debe tener un valor mayor que cero.");
        }

        long startedAt = System.nanoTime();
        List<Callable<Integer>> tasks = providers.stream()
                .<Callable<Integer>>map(provider -> () ->
                        provider.isBlacklisted(ipAddress) ? provider.id() : null)
                .toList();

        List<Integer> matches = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
            List<Future<Integer>> futures = executor.invokeAll(tasks);
            for (Future<Integer> future : futures) {
                Integer providerId = future.get();
                if (providerId != null) {
                    matches.add(providerId);
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La búsqueda en la lista negra fue interrumpida antes de que pudiera terminar.", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Se presentó un error al consultar uno de los proveedores de la lista negra.", ex.getCause());
        }

        matches.sort(Comparator.naturalOrder());
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
        return new SearchResult(ipAddress, matches, providers.size(), elapsed);
    }
}
