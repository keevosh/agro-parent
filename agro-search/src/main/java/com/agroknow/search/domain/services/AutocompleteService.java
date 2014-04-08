package com.agroknow.search.domain.services;

import com.agroknow.search.domain.entities.AgroAutocompleteRequest;
import com.agroknow.search.domain.entities.AgroAutocompleteResponse;
import com.agroknow.search.domain.entities.UserQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Iterator;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author aggelos
 */
@Service
public class AutocompleteService {

    @Autowired
    private TransportClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    public AutocompleteService() {}

    /**
     *
     * @param req
     * @return
     * @throws IOException
     */
    public AgroAutocompleteResponse autocomplete(AgroAutocompleteRequest req) throws IOException {
        AgroAutocompleteResponse res = new AgroAutocompleteResponse();

        // create the SearchRequestBuilder
        SearchRequestBuilder searchReqBuilder = esClient.prepareSearch("history").setTypes("query");

        // setup query
        QueryBuilder queryBuilder;
        queryBuilder = QueryBuilders.prefixQuery("query.raw", req.getQuery());
        searchReqBuilder.setQuery(queryBuilder);

        // setup paging
        searchReqBuilder.setFrom(0);
        searchReqBuilder.setSize(200);

        // execute the request and get the response
        SearchResponse response = searchReqBuilder.execute().actionGet();

        // parse the response into AgroAutocompleteResponse
        Iterator<SearchHit> hitsIter = response.getHits().iterator();
        SearchHit hit;
        int i=0;

        while (hitsIter.hasNext() &&  i<10) {
            hit = hitsIter.next();

            String source = hit.getSourceAsString();
            UserQuery uq = objectMapper.reader(UserQuery.class).readValue(source);
            if(res.addResult(uq.getQuery())) {
                i++;
            }
        }

        // add query (meta)data to res
        res.setTotal(response.getHits().getTotalHits());
        res.setTime(response.getTookInMillis());

        // ..and
        return res;
    }

}
