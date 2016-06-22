/*
 *  Copyright 2012-2016 the original author or authors.
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

package org.flockdata.test.unit.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ImportContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by mike on 28/01/15.
 */
public class TestImporterPreparsing extends AbstractImport {
    @Test
    public void string_PreParseRow() throws Exception {

        ImportContentModel params = ProfileReader.getContentModel("/model/pre-parse.json");
        assertEquals(',', params.getDelimiter());
        assertEquals(false, params.hasHeader());
        long rows = fileProcessor.processFile(params, "/data/properties-rlx.txt");
        assertEquals(4, rows);

        List<EntityInputBean> entityBatch = fdBatcher.getEntities();

        for (EntityInputBean entityInputBean : entityBatch) {
            assertFalse("Expression not parsed for code",entityInputBean.getCode().contains("|"));
            assertTrue("Tag not set", entityInputBean.getTags().size() == 3);
            TagInputBean politician= null;
            for (TagInputBean tagInputBean : entityInputBean.getTags()) {
                assertFalse("Expression not parsed for code", tagInputBean.getCode().contains("|"));
                if ( tagInputBean.getLabel().equals("Politician"))
                    politician= tagInputBean;
                if ( tagInputBean.getLabel().equals("InterestGroup")){
                    assertEquals("direct", tagInputBean.getEntityLinks().keySet().iterator().next());
                    TestCase.assertEquals(2, tagInputBean.getProperties().size());
                    TestCase.assertNotNull(tagInputBean.getProperties().get("amount"));
                    TestCase.assertEquals("ABC123", tagInputBean.getProperties().get("calculatedColumn"));
                }
            }
            assertNotNull(politician);
            HashMap link = (HashMap) politician.getEntityLinks().get("receives");
            assertNotNull(link);
            assertNotNull(link.get("amount"));
            assertTrue("Amount not calculated as a value", Integer.parseInt(link.get("amount").toString()) >0);

        }
        ObjectMapper om = new ObjectMapper();
        try {
            om.writeValueAsString(entityBatch);
        } catch (Exception e) {
            throw new FlockException("Failed to serialize");
        }

    }

}
