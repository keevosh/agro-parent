package com.agroknow.search.domain.repositories;

import com.agroknow.search.domain.entities.User;
import com.agroknow.search.domain.exceptions.PersistenceException;
import com.agroknow.search.domain.exceptions.UserAlreadyExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An elasticsearch based implementation of {@code UserRepository}.
 *
 * @author aggelos
 */
@Component
public class UserRepositoryES implements UserRepository {

    private final Logger log = LoggerFactory.getLogger(UserRepositoryES.class);

    @Autowired
    private TransportClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public User findByUserEmail(String email) {
        SearchRequestBuilder searchReqBuilder = esClient.prepareSearch("users").setTypes("user").setSize(1);
        searchReqBuilder.setQuery(QueryBuilders.queryString(email).defaultField("email.raw"));
        try {
            SearchResponse response = searchReqBuilder.execute().actionGet();
            if(response.getHits()==null || response.getHits().getTotalHits() < 1) {
                return null;
            }

            SearchHit hit = response.getHits().getAt(0);
            String source = hit.getSourceAsString();
            User user = objectMapper.reader(User.class).readValue(source);
            user.setId(hit.getId());
            return user;
        } catch (Exception ex) {
            String msg = "Could not found user with email ["+email+"] in the ES user's index.";
            log.error(msg, log.isDebugEnabled() ? ex : null);
            throw new RuntimeException(msg, ex);
        }
    }

    @Override
    public User findByToken(String token) {
        SearchRequestBuilder searchReqBuilder = esClient.prepareSearch("users").setTypes("user").setSize(1);
        searchReqBuilder.setQuery(QueryBuilders.termQuery("token", token));
        try {
            SearchResponse response = searchReqBuilder.execute().actionGet();
            SearchHit hit = response.getHits().getAt(0);
            String source = hit.getSourceAsString();
            User user = objectMapper.reader(User.class).readValue(source);
            user.setId(hit.getId());
            return user;
        } catch (Exception ex) {
            String msg = "Could not found user by token in the ES user's index.";
            log.error(msg, log.isDebugEnabled() ? ex : null);
            throw new RuntimeException(msg, ex);
        }
    }

    @Override
    public User save(User user) {
        try {
            User checkExisting = this.findByUserEmail(user.getEmail());
            if(checkExisting != null) {
                throw new UserAlreadyExistsException("Account already exists for: " + user.getEmail());
            }

            IndexResponse res = esClient.prepareIndex("users", "user")
                .setSource(objectMapper.writeValueAsString(user))
                .execute()
                .actionGet();

            user.setId(res.getId());
            return user;
        } catch(UserAlreadyExistsException ex) {
            throw ex;
        } catch(Exception ex) {
            throw new PersistenceException("Could not create account for: "+user.getEmail(), ex);
        }
    }
}
