
package com.agroknow.search.domain.entities;

/**
 *
 * @author aggelos
 */
public class AgroAutocompleteRequest {

    private String query;

    public AgroAutocompleteRequest() {
    }

    public AgroAutocompleteRequest(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
