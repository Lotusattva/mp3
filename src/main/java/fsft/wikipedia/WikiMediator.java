package fsft.wikipedia;

import java.util.List;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.time.Instant;

import io.github.fastily.jwiki.core.Wiki;
import fsft.fsftbuffer.*;

/**
 * A class that mediates between the Wikipedia API and the user, providing the
 * following services:
 * <ul>
 * <li>search for page titles that match a query string with the method
 * {@link #search(String, int)}</li>
 * <li>retrieve the text of a Wikipedia page with the method
 * {@link #getPage(String)}</li>
 * <li>return the most common search terms or page titles used in the most
 * recent time window with the method {@link #zeitgeist(Duration, int)}</li>
 * <li>return the number of requests made using the public API in the most
 * recent time window with the method {@link #peakLoad(Duration)}</li>
 * </ul>
 */
public class WikiMediator {

    private final Map<Instant, String> searchRecord;
    private final Wiki wiki;
    private final FSFTBuffer<WikiPage> wikiPageCache;

    public WikiMediator() {
        searchRecord = new HashMap<>();
        wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
        wikiPageCache = new FSFTBuffer<>();
    }

    /**
     * Given a {@code searchTerm}, return up to {@code limit} page titles that match
     * the query string (per Wikipedia's search service).
     * 
     * @param searchTerm
     * @param limit
     * @return a list of page titles
     */
    public List<String> search(String searchTerm, int limit) {
        searchRecord.put(Instant.now(), searchTerm);
        List<String> pageTitles = wiki.search(searchTerm, limit);
        for (String pageTitle : pageTitles) {
            wikiPageCache.touch(pageTitle);
        }
        return pageTitles;
    }

    /**
     * Given a {@code pageTitle}, return the text associated with the Wikipedia page
     * that matches {@code pageTitle}.
     * 
     * @param pageTitle
     * @return the text of the page
     */
    public String getPage(String pageTitle) {
        searchRecord.put(Instant.now(), pageTitle);
        try {
            return wikiPageCache.get(pageTitle).getPage();
        } catch (InvalidIdentifierException e) {
            String page = wiki.getPageText(pageTitle);
            wikiPageCache.put(new WikiPage(pageTitle, page));
            return page;
        }
    }

    /**
     * Return the most common Strings ({@code searchTerms} or {@code pageTitles})
     * used in {@link #search(String, int)} and {@link #getPage(String)} requests in
     * the most recent time window specified by {@code duration}, with items being
     * sorted in non-increasing count order. When many requests have been made,
     * return only {@code limit} items.
     * 
     * @param duration
     * @param limit
     * @return a list of the most common search terms or page titles
     */
    public List<String> zeitgeist(Duration duration, int limit) {
        List<String> zeitgeist = searchRecord.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(Instant.now().minus(duration)))
                .map(Map.Entry::getValue).sorted((a, b) -> {
                    int countA = (int) searchRecord.values().stream().filter(s -> s.equals(a)).count();
                    int countB = (int) searchRecord.values().stream().filter(s -> s.equals(b)).count();
                    return countB - countA;
                }).distinct().limit(limit).toList();
        return zeitgeist.subList(0, Math.min(limit, zeitgeist.size()));
    }

    /**
     * Return the number of requests made using the public API of
     * {@link WikiMediator} in the most recent time window specified by
     * {@code duration}.
     * 
     * @param duration
     * @return the number of requests made
     */
    public int peakLoad(Duration duration) {
        return (int) searchRecord.keySet().stream().filter(i -> i.isAfter(Instant.now().minus(duration))).count();
    }
}
