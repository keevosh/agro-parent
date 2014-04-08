package com.agroknow.search.domain.services;

import com.agroknow.domain.InternalFormat;
import com.agroknow.search.domain.entities.AgroAutocompleteRequest;
import com.agroknow.search.domain.entities.AgroAutocompleteResponse;
import com.agroknow.search.domain.entities.AgroSearchRequest;
import com.agroknow.search.domain.entities.AgroSearchResponse;
import com.agroknow.search.domain.entities.UserQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.Facets;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author aggelos
 * @param <T>
 */
public class SearchService<T extends InternalFormat> {

    @Autowired
    private TransportClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    private String fileFormat;
    private Class<T> fileFormatClass;
    private final Map<String, List<String>> knownFieldFacets = new HashMap<String, List<String>>();

    public SearchService() {}

    public final void init(String fileFormat, Class<T> fileFormatClass) {
        this.fileFormat = fileFormat;
        this.fileFormatClass = fileFormatClass;
        this.knownFieldFacets.put("date", Arrays.asList(new String[]{"creationDate", "lastUpdateDate"}));
    }

    public AgroSearchResponse<T> search(AgroSearchRequest req) throws IOException {
        AgroSearchResponse<T> res = new AgroSearchResponse<T>();

        // create the SearchRequestBuilder
        SearchRequestBuilder searchReqBuilder = esClient.prepareSearch(fileFormat).setTypes(fileFormat);

        // setup query
        QueryBuilder queryBuilder;
        Map<String, String> searchFieldsMap = req.getSearchFields();
        String[] searchFields = searchFieldsMap.keySet().toArray(new String[searchFieldsMap.keySet().size()]);
        int searchFieldsLength = searchFields.length;

        // if it is a signle query just create a matchQuery
        if(searchFieldsLength == 1) {
            String searchField = searchFields[0];
            queryBuilder = parsePlainQueryString(searchField, searchFieldsMap.get(searchField));

        // else create a boolQuery for the given fields:
        } else {
            queryBuilder = QueryBuilders.boolQuery();
            BoolQueryBuilder boolQB = (BoolQueryBuilder) queryBuilder;

            for(int i=0; i<searchFieldsLength; i++) {
                String field = searchFields[i];
                String fieldValue = searchFieldsMap.get(field);

                boolQB.must(parsePlainQueryString(field, fieldValue));
            }
        }
        searchReqBuilder.setQuery(queryBuilder);

        // setup sorting
        String sortByField = req.getSortByField();
        if (!StringUtils.isEmpty(sortByField)) {
            searchReqBuilder.addSort(sortByField, SortOrder.valueOf(req.getSortOrder()));
        }

        // setup paging
        searchReqBuilder.setFrom( (req.getPage()-1) * req.getPageSize() );
        searchReqBuilder.setSize(req.getPageSize());

        // setup facets
        List<String> facetFields = req.getFacetFields();
        for (String facetField : facetFields) {
            searchReqBuilder.addFacet(getFacetBuilderForField(facetField, req));
        }

        // execute the request and get the response
        SearchResponse response = searchReqBuilder.execute().actionGet();

        // parse the response into AgroSearchResponse<T>
        Iterator<SearchHit> hitsIter = response.getHits().iterator();
        SearchHit hit;
        while (hitsIter.hasNext()) {
            hit = hitsIter.next();

            String source = hit.getSourceAsString();
            T doc = objectMapper.reader(fileFormatClass).readValue(source);
            doc.setIdentifier(hit.getId());
            res.addResult(doc);
        }

        // add facets to res
        Facets facets = response.getFacets();
        if(facets != null) {
            res.setupFacetsFromES(facets);
        }

        // add query (meta)data to res
        res.setSortByField(req.getSortByField());
        res.setSortOrder(req.getSortOrder());
        res.setPage(req.getPage());
        res.setPageSize(req.getPageSize());
        res.setTotal(response.getHits().getTotalHits());
        res.setTime(response.getTookInMillis());

        // ..and
        return res;
    }

    /**
     * Fetch the akif/agrif objects of the given ids. When no ids argument an empty
     * response is returned with no results.
     *
     * @param set
     * @param ids
     * @return
     * @throws IOException
     */
    public AgroSearchResponse<T> fetch(String set, String[] ids) throws IOException {
        AgroSearchResponse<T> res = new AgroSearchResponse<T>();
        if (ArrayUtils.isEmpty(ids)) { // return empty res when no ids
            return res;
        }

        // create the MultiGetRequest for akif/agrif ids given
        MultiGetRequestBuilder mGetBuilder = esClient.prepareMultiGet();
        MultiGetRequest.Item item;
        for (String id : ids) {
            item = new MultiGetRequest.Item(fileFormat, fileFormat, id).routing(set);
            mGetBuilder.add(item);
        }

        // execute the request and get the response
        MultiGetResponse response = mGetBuilder.execute().actionGet();

        // parse the response into AgroSearchResponse<Akif>
        MultiGetItemResponse itemResponse;
        Iterator<MultiGetItemResponse> responseIter = response.iterator();
        while (responseIter.hasNext()) {
            itemResponse = responseIter.next();
            if (itemResponse.isFailed()) {
                continue;
            }
            GetResponse getRes = itemResponse.getResponse();
            String source = getRes.getSourceAsString();
            T doc = objectMapper.reader(fileFormatClass).readValue(source);
            doc.setIdentifier(getRes.getId());
            res.increaseTotal();
            res.addResult(doc);
        }

        // ..and
        return res;
    }

    private FacetBuilder getFacetBuilderForField(String facetField, AgroSearchRequest req) {
        String timeInterval = "year";

        if(facetField.contains(";")) {
            String[] facetFieldParts = facetField.split(";");
            facetField = facetFieldParts[0];
            timeInterval = facetFieldParts[1];
        }
        if(knownFieldFacets.get("date").contains(facetField)) {
            return FacetBuilders.dateHistogramFacet(facetField).field(facetField).interval(timeInterval);
        }

        return FacetBuilders.termsFacet(facetField).field(facetField).size(req.getFacetSize());
    }

    /**
     *
     * @param field
     * @param value
     * @return
     */
    private QueryBuilder parsePlainQueryString(String field, String value) {
        QueryBuilder q;

        // check for match_all query
        if("_all".equals(field) && "*".equals(value)) {
            return QueryBuilders.matchAllQuery();
        }

        // try to parse a query with AND/OR operatators (if any)
        String[] andParts = value.split("AND");
        if(andParts.length == 1) {
            q = parseORQueryString(field, value);
        } else {
            BoolQueryBuilder andQB = QueryBuilders.boolQuery();
            andQB.minimumNumberShouldMatch(1);
            for(String andPart : andParts) {
                andQB.must(parseORQueryString(field, StringUtils.trim(andPart)));
            }
            q = andQB;
        }
        return q;
    }

    /**
     *
     * @param field
     * @param value
     * @return
     */
    private QueryBuilder parseORQueryString(String field, String value) {
        QueryBuilder q;
        String[] orParts = value.split("OR");
        if(orParts.length == 1) {
            q = createPhraseOrTermQuery(field, value);
        } else {
            BoolQueryBuilder orQB = QueryBuilders.boolQuery();
            orQB.minimumNumberShouldMatch(1);
            for(String orPart : orParts) {
                orQB.should(createPhraseOrTermQuery(field, StringUtils.trim(orPart)));
            }
            q = orQB;
        }
        return q;
    }

    /**
     *
     * @param field
     * @param value
     * @return
     */
    private QueryBuilder createPhraseOrTermQuery(String field, String value) {
        QueryBuilder q;
        if(value.split(" ").length > 1) {
            q = QueryBuilders.matchPhraseQuery(field, value);
        } else {
            q = QueryBuilders.matchQuery(field, value);
        }
        return q;
    }
}
