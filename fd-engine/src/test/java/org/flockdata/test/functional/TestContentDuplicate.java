/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.functional;

import org.flockdata.helper.JsonUtils;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.test.utils.Helper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.Entity;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * User: mike
 * Date: 12/11/14
 * Time: 1:44 PM
 */
public class TestContentDuplicate  extends  EngineBase{
    private Logger logger = LoggerFactory.getLogger(TestTrack.class);

    @org.junit.Before
    public void setup(){
        engineConfig.setDuplicateRegistration(true);
    }

    @Test
    public void reprocess_HistoricContentsNotCreated() throws Exception {
        logger.debug("### reprocess_HistoricContentsNotCreated");
        SystemUser su = registerSystemUser("reprocess_HistoricContentsNotCreated");

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("reprocess_HistoricContentsNotCreated", true));
        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "poppy", "TestDoc", DateTime.now(), "123");
        inputBean.setApiKey(su.getApiKey());

        int max = 5;
        List<ContentInputBean> contentBeans = new ArrayList<>();
        for (int i=0; i<max; i++){
            ContentInputBean contentBean = new ContentInputBean("poppy", DateTime.now(), Helper.getSimpleMap("name", "a" +i));
            contentBeans.add(contentBean);
            inputBean.setContent(contentBean);
            mediationFacade.trackEntity(JsonUtils.getObjectAsJsonBytes(inputBean));
        }
        Entity entity = trackService.findByCallerRef(su.getCompany(), fortress.getName(), "TestDoc", "123");
        assertEquals(max, trackService.getLogCount(su.getCompany(), entity.getMetaKey()));

        // Reprocess forward
        for (ContentInputBean contentBean : contentBeans) {
            inputBean.setContent(contentBean);
            mediationFacade.trackEntity(JsonUtils.getObjectAsJsonBytes(inputBean));
        }

        assertEquals(max, trackService.getLogCount(su.getCompany(), entity.getMetaKey()));

        // Try reversing out of order
        Collections.reverse(contentBeans);
        for (ContentInputBean contentBean : contentBeans) {
            inputBean.setContent(contentBean);
            mediationFacade.trackEntity(JsonUtils.getObjectAsJsonBytes(inputBean));
        }

        assertEquals(max, trackService.getLogCount(su.getCompany(), entity.getMetaKey()));


    }
}
