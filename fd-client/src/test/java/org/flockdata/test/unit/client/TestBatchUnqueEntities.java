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

import junit.framework.TestCase;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ExtractProfileHandler;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.model.ExtractProfile;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 *
 * @author mholdsworth
 * @since 23/01/2016
 */
public class TestBatchUnqueEntities extends AbstractImport{

    /**
     * Given a source with the same entity and different tags, we should be able to batch one entity + many tags
     * rather than wire over one entity+ one tag.
     * @throws Exception
     */
    @Test
    public void duplicateKeysInSource_UniqueEntity() throws Exception {

        ContentModel contentModel = ContentModelDeserializer.getContentModel( "/model/duplicate-entities.json");
        ExtractProfile extractProfile = new ExtractProfileHandler(contentModel, true);
        contentModel.setDocumentName("Movie"); // ToDo: Deserialize DocumentInputBean
        extractProfile.setContentType(ExtractProfile.ContentType.CSV);
        contentModel.setEntityOnly(true);

        fileProcessor.processFile(extractProfile, "/data/duplicate-entities.csv");
        List<EntityInputBean> entities = fdBatcher.getEntities();
        TestCase.assertEquals(1, entities.size());

        EntityInputBean movie = entities.iterator().next();
        int personCount = 0;
        for (TagInputBean tag : movie.getTags()) {
            if ( tag.getLabel().equals("Person"))
                personCount++;
        }

        assertEquals("Should be 2 directors + 3 actors",5, personCount);

    }
}
