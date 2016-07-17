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

import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ExtractProfileHandler;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.transform.Transformer;
import org.flockdata.transform.tags.TagMapper;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * SPeL tests and custom properties for tags
 * <p>
 * Created by mike on 17/01/15.
 */
public class TestExpressions extends AbstractImport {
    @Test
    public void string_Concatenation() throws Exception {
        ContentModel params = ContentModelDeserializer.getContentModel("/model/tag-expressions.json");
        TagMapper mapper = new TagMapper();
        String[] headers = new String[]{"last_name", "first_name", "birthday", "gender", "type", "state", "district", "party", "url", "address", "phone", "contact_form", "rss_url", "twitter", "facebook", "facebook_id", "youtube", "youtube_id", "bioguide_id", "thomas_id", "opensecrets_id", "lis_id", "cspan_id", "govtrack_id", "votesmart_id", "ballotpedia_id", "washington_post_id", "icpsr_id", "wikipedia_id"};
        String[] data = new String[]{"Whitehouse", "Sheldon", "1955-10-20", "M", "sen", "RI", "", "Democrat", "http://www.whitehouse.senate.gov", "530 Hart Senate Office Building Washington DC 20510", "202-224-2921", "http://www.whitehouse.senate.gov/contact", "http://www.whitehouse.senate.gov/rss/feeds/?type=all&amp;cachebuster=1", "SenWhitehouse", "SenatorWhitehouse", "194172833926853", "SenatorWhitehouse", "UCnG0N70SNBkNqvIMLodPTIA", "W000802", "01823", "N00027533", "S316", "92235", "412247", "2572", "Sheldon Whitehouse", "gIQA7KHw9O", "40704", "Sheldon Whitehouse"};
        Map<String, Object> json = mapper.setData(Transformer.convertToMap(headers, data, new ExtractProfileHandler(params)), params);
        assertNotNull(json);
        assertNotNull(mapper);
        assertEquals("Politician", mapper.getLabel());
        assertEquals("Whitehouse, Sheldon", mapper.getName());
        assertEquals("01823", mapper.getCode());
        assertEquals(1, mapper.getTargets().get("HAS_ALIAS").size());
    }

    @Test
    public void string_Properties() throws Exception {
        ContentModel params = ContentModelDeserializer.getContentModel("/model/tag-expressions.json");
        TagMapper mapper = new TagMapper();
        String[] headers = new String[]{"last_name", "first_name", "birthday", "gender", "type", "state", "district", "party", "url", "address", "phone", "contact_form", "rss_url", "twitter", "facebook", "facebook_id", "youtube", "youtube_id", "bioguide_id", "thomas_id", "opensecrets_id", "lis_id", "cspan_id", "govtrack_id", "votesmart_id", "ballotpedia_id", "washington_post_id", "icpsr_id", "wikipedia_id"};
        String[] data = new String[]{"Whitehouse", "Sheldon", "1955-10-20", "M", "sen", "RI", "", "Democrat", "http://www.whitehouse.senate.gov", "530 Hart Senate Office Building Washington DC 20510", "202-224-2921", "http://www.whitehouse.senate.gov/contact", "http://www.whitehouse.senate.gov/rss/feeds/?type=all&amp;cachebuster=1", "SenWhitehouse", "SenatorWhitehouse", "194172833926853", "SenatorWhitehouse", "UCnG0N70SNBkNqvIMLodPTIA", "W000802", "01823", "N00027533", "S316", "92235", "412247", "2572", "Sheldon Whitehouse", "gIQA7KHw9O", "40704", "Sheldon Whitehouse"};
        Map<String, Object> json = mapper.setData(Transformer.convertToMap(headers, data, new ExtractProfileHandler(params)), params);
        assertNotNull(json);
        assertNotNull(mapper);

        assertEquals("Custom properties not being set", 3, mapper.getProperties().size());
        boolean birthdaySet = false, urlSet = false, genderSet = false;
        for (String key : mapper.getProperties().keySet()) {
            switch (key) {
                case "dob":
                    assertEquals("1955-10-20", mapper.getProperties().get("dob"));
                    birthdaySet = true;
                    break;
                case "url":
                    urlSet = true;
                    assertEquals("http://www.whitehouse.senate.gov", mapper.getProperties().get("url"));
                    break;
                case "gender":
                    genderSet = true;
                    break;
            }
        }

        assertEquals("Unable to find remapped target property name", true, birthdaySet);
        assertEquals(true, urlSet);
        assertEquals(true, genderSet);
    }


}
