#!/bin/bash

curl -XPUT "http://$1/history/" -d '{
    "settings" : {
        "number_of_shards" : 3,
        "number_of_replicas" : 0,
        "routing.allocation.include.zone" : "main",
        "query.default_field" : "_all",

        "analysis":{
            "analyzer": {
                "query_raw": {
                    "type": "custom",
                    "tokenizer": "keyword",
                    "filter": ["lowercase"]
                }
            }
        }
    },

    "mappings" : {
        "query" : {
            "properties" : {
                "query" : {
                    "type" : "multi_field",
                    "fields" : {
                        "query" : {"type" : "string", "analyzer" : "standard"},
                        "raw" : {"type" : "string", "analyzer" : "query_raw"}
                    }
                }
            }
        }
    }
}'
