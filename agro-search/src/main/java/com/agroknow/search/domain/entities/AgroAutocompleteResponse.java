package com.agroknow.search.domain.entities;

import java.util.Collection;
import java.util.HashSet;

/**
 *
 * @author aggelos
 */
public class AgroAutocompleteResponse {

    private long total = 0;
    private long time = -1;
    private Collection<String> results = new HashSet<String>(10);

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public void increaseTotal() {
        this.increaseTotal(1);
    }

    public void increaseTotal(long num) {
        this.total += num;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Collection<String> getResults() {
        return results;
    }

    public boolean addResult(String result) {
        return results.add(result);
    }

    public void setResults(Collection<String> results) {
        this.results = results;
    }
}
