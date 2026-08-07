package edu.eci.arsw.blacklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class VirtualThreadBlackListSearchTest {
    private static final String IP_ADDRESS = "202.24.34.55";
    private static final int ALARM_THRESHOLD = 5;
    private final List<BlackListProvider> providers = ProviderFactory.create(100, false);
    private final SearchResult baseline = new SequentialBlackListSearch(providers)
            .search(IP_ADDRESS, ALARM_THRESHOLD);

    @Test
    void virtualThreadsShouldMatchSequentialBaseline() {
        SearchResult result = new VirtualThreadBlackListSearch(providers)
                .search(IP_ADDRESS, ALARM_THRESHOLD);

        assertEquals(baseline.matchingProviderIds(), result.matchingProviderIds());
        assertEquals(baseline.consultedProviders(), result.consultedProviders());
        assertEquals(baseline.isTrustworthy(ALARM_THRESHOLD), result.isTrustworthy(ALARM_THRESHOLD));
        assertTrue(result.elapsed().toNanos() >= 0);
    }

    @Test
    void virtualThreadsShouldConsultAllProvidersAndReturnOrderedUniqueMatches() {
        SearchResult result = new VirtualThreadBlackListSearch(providers)
                .search(IP_ADDRESS, ALARM_THRESHOLD);

        List<Integer> matches = result.matchingProviderIds();
        List<Integer> sortedMatches = new ArrayList<>(matches);
        sortedMatches.sort(Integer::compareTo);

        assertEquals(100, result.consultedProviders());
        assertEquals(matches.size(), new HashSet<>(matches).size());
        assertEquals(sortedMatches, matches);
    }

    @Test
    void virtualThreadsShouldWorkOnRepeatedSearches() {
        VirtualThreadBlackListSearch search = new VirtualThreadBlackListSearch(providers);
        SearchResult first = search.search(IP_ADDRESS, ALARM_THRESHOLD);
        SearchResult second = search.search(IP_ADDRESS, ALARM_THRESHOLD);

        assertEquals(first.matchingProviderIds(), second.matchingProviderIds());
    }

    @Test
    void virtualThreadsShouldValidateSearchArguments() {
        VirtualThreadBlackListSearch search = new VirtualThreadBlackListSearch(providers);

        assertThrows(NullPointerException.class, () -> search.search(null, ALARM_THRESHOLD));
        assertThrows(IllegalArgumentException.class, () -> search.search(IP_ADDRESS, 0));
    }
}
