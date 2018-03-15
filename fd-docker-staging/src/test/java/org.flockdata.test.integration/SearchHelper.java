/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.integration;

import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.IndexManager;
import org.flockdata.search.QueryParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertEquals;

/**
 * Support functions for interacting with ElasticSearch during integration testing
 *
 * @author mholdsworth
 * @since 31/05/2016
 */
@Component
public class SearchHelper {

    @Autowired
    private IndexManager indexManager;
    
    QueryParams getTagQuery(String company, String label, String searchText) throws IOException {
        QueryParams qp= new QueryParams(searchText)
                .searchTags()
                .setIndex(indexManager.getTagIndexRoot(company, label))
                .setTypes(label.toLowerCase());
        return qp;
    }

    QueryParams getTagMatchQuery(String company, String label, String field, String searchText ) throws IOException {
        QueryParams qp= new QueryParams()
                .searchTags()
                .setIndex(indexManager.getTagIndexRoot(company, label))
                .setTypes(label.toLowerCase())
                .addTerm(field, searchText);
        return qp;
    }

    void assertHitCount(String message, int expectedCount, Map<String, Object> esResult) {
        assertNotNull(esResult);
        int count = getHitCount(esResult);
        assertEquals(message + " got "+count, expectedCount, count);
    }

    Integer getHitCount( Map<String, Object> esResult){
        Map hits = (Map) esResult.get("hits");
        assertNotNull(hits);
        return (Integer) hits.get("total");
    }

    /**
     *
     * @param esResult Map of ES results
     * @return the hits as a Json string
     */
    String getHits(Map<String, Object> esResult) {
        assertNotNull(esResult);
        return JsonUtils.toJson(esResult.get("hits"));
    }
}