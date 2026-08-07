package edu.eci.arsw.blacklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class FixedPoolBlackListSearchTest {
    private static final String IP_ADDRESS = "202.24.34.55";
    private static final int ALARM_THRESHOLD = 5;

    private final List<BlackListProvider> providers =
            ProviderFactory.create(100, false);

    private final SearchResult baseline =
            new SequentialBlackListSearch(providers)
                    .search(IP_ADDRESS, ALARM_THRESHOLD);

    @ParameterizedTest
    @ValueSource(ints = {2, 4, 8})
    void shouldMatchSequentialResultForRequiredPoolSizes(
            int poolSize) {

        BlackListSearch search =
                new FixedPoolBlackListSearch(
                        providers,
                        poolSize);

        SearchResult result =
                search.search(IP_ADDRESS, ALARM_THRESHOLD);

        assertEquals(
                baseline.matchingProviderIds(),
                result.matchingProviderIds());

        assertEquals(
                baseline.consultedProviders(),
                result.consultedProviders());

        assertEquals(
                baseline.isTrustworthy(ALARM_THRESHOLD),
                result.isTrustworthy(ALARM_THRESHOLD));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 4, 8})
    void shouldConsultAllProvidersAndReturnOrderedUniqueMatches(
            int poolSize) {

        SearchResult result =
                new FixedPoolBlackListSearch(
                        providers,
                        poolSize)
                        .search(IP_ADDRESS, ALARM_THRESHOLD);

        List<Integer> matches =
                result.matchingProviderIds();

        List<Integer> sorted =
                new ArrayList<>(matches);

        sorted.sort(Integer::compareTo);

        assertEquals(100, result.consultedProviders());
        assertEquals(sorted, matches);
        assertEquals(
                matches.size(),
                new HashSet<>(matches).size());
    }

    @Test
    void shouldRejectNonPositivePoolSizes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FixedPoolBlackListSearch(
                        providers,
                        0));

        assertThrows(
                IllegalArgumentException.class,
                () -> new FixedPoolBlackListSearch(
                        providers,
                        -1));
    }

    @Test
    void shouldValidateSearchArguments() {
        BlackListSearch search =
                new FixedPoolBlackListSearch(
                        providers,
                        4);

        assertThrows(
                NullPointerException.class,
                () -> search.search(
                        null,
                        ALARM_THRESHOLD));

        assertThrows(
                IllegalArgumentException.class,
                () -> search.search(
                        IP_ADDRESS,
                        0));
    }
}