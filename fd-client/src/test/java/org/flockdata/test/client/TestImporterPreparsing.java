/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.test.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
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
        FileProcessor fileProcessor = new FileProcessor();
        ClientConfiguration configuration = getClientConfiguration();
        configuration.setLoginUser("test");

        ContentProfileImpl params = ProfileReader.getImportProfile("/profile/pre-parse.json");
        assertEquals(',', params.getDelimiter());
        assertEquals(false, params.hasHeader());
        long rows = fileProcessor.processFile(params, "/data/properties-rlx.txt", getFdWriter(), null, configuration);
        assertEquals(4, rows);

        List<EntityInputBean> entityBatch = getFdWriter().getEntities();

        for (EntityInputBean entityInputBean : entityBatch) {
            assertFalse("Expression not parsed for callerRef",entityInputBean.getCode().contains("|"));
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
