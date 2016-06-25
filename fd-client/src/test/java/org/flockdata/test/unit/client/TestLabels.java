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
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ExtractProfileDeserializer;
import org.flockdata.profile.ExtractProfileHandler;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.model.ExtractProfile;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Various import profiles
 * Created by mike on 1/03/15.
 */
public class TestLabels extends AbstractImport {
    @Test
    public void conflict_LabelDefinition() throws Exception {

        ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/tag-labels.json");
        ExtractProfile extractProfile = new ExtractProfileHandler(contentModel);
        fileProcessor.processFile(extractProfile,
                "/data/tag-labels.csv");

        List<TagInputBean> tagInputBeans = getFdBatcher().getTags();
        assertEquals(2, tagInputBeans.size());
        boolean loanType=false, occupancy= false;
        for (TagInputBean tagInputBean : tagInputBeans) {
            if ( tagInputBean.getLabel().equals("Occupancy")) {
                occupancy = true;
                assertEquals("1", tagInputBean.getCode());
                assertEquals(null, tagInputBean.getName());
            }else if ( tagInputBean.getLabel().equals("Loan Type")) {
                loanType = true;
                assertEquals("blah", tagInputBean.getCode());
                assertEquals(null, tagInputBean.getName());
            } else
                throw new FlockException("Unexpected tag - " + tagInputBean.toString());

        }
        assertTrue("Occupancy Not Found", occupancy);
        assertTrue("loanType Not Found", loanType);
    }

    @Test
    public void label_expressionsAndConstants() throws Exception {
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/tag-label-expressions.json");
        ExtractProfile extractProfile = new ExtractProfileHandler(contentModel);

        fileProcessor.processFile(extractProfile,
                "/data/tag-label-expressions.csv");

        List<TagInputBean> tagInputBeans = getFdBatcher().getTags();
        // 1 Politician
        //
        assertEquals(4, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            if ( tagInputBean.getLabel().equals("Agency"))
                assertEquals("1", tagInputBean.getCode());
            else if ( tagInputBean.getLabel().equals("Edit Status"))
                assertEquals("7", tagInputBean.getCode());
            else if ( tagInputBean.getLabel().equals("MSA/MD"))
                assertEquals("10180", tagInputBean.getCode());
            else if ( tagInputBean.getLabel().equals("County"))
                assertEquals("011", tagInputBean.getCode());
            else
                throw new FlockException("Unexpected tag - " + tagInputBean.toString());

        }
    }

    @Test
    public void alias_DescriptionEvaluates() throws Exception {

        ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/labels.json");
        ExtractProfile extractProfile = ExtractProfileDeserializer.getImportProfile("/import/empty-ignored.json", contentModel);

        fileProcessor.processFile(
                extractProfile,
                "/data/assets.txt");
        List<EntityInputBean>entities = fdBatcher.getEntities();
        List<TagInputBean> tagInputBeans = entities.iterator().next().getTags();
        assertEquals(1, tagInputBeans.size());
        TestCase.assertEquals(3, entities.iterator().next().getTags().iterator().next().getAliases().size());
        List<TagInputBean> tags = entities.iterator().next().getTags();
        for (TagInputBean tag : tags) {
            Collection<AliasInputBean> aliase = tag.getAliases();
            for (AliasInputBean alias : aliase) {
                switch (alias.getDescription()){
                    case "ISIN":
                    case "Asset PK":
                    case "assetCode":
                        break;
                    default:
                        throw new Exception("Unexpected alias description " + alias.toString());
                }
            }
        }
    }

}
