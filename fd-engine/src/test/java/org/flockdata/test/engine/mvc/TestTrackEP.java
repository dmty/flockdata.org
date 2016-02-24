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

package org.flockdata.test.engine.mvc;

import org.flockdata.helper.JsonUtils;
import org.flockdata.model.EntityLog;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackRequestResult;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 29/10/14
 * Time: 11:59 AM
 */
public class TestTrackEP extends MvcBase {

    @Test
    public void track_MinimalArguments () throws Exception {
        FortressResultBean f = makeFortress(mike(),  new FortressInputBean("track_MinimalArguments", true));
        EntityInputBean eib = new EntityInputBean(f.getName(), "DocType");
        eib.setFortressUser("usera");
        ContentInputBean cib = new ContentInputBean("userA", EntityContentHelper.getRandomMap());
        eib.setContent(cib);
        TrackRequestResult trackResult = track(mike(), eib);
        assertNotNull(trackResult);
        EntityBean e = getEntity(mike(), trackResult.getKey());
        //Entity e = entityService.getEntity(su.getCompany(), trackResult.getKey());

        assertEquals("usera", e.getLastUser());
        assertEquals("usera", e.getCreatedUser());

    }

    @Test
    public void track_FortressUserInEntity() throws Exception {
        FortressResultBean f = makeFortress(mike(),  new FortressInputBean("track_MinimalArguments", true));
        EntityInputBean eib = new EntityInputBean(f.getName(), "DocType");
        eib.setFortressUser("userA");
        ContentInputBean cib = new ContentInputBean(EntityContentHelper.getRandomMap());
        eib.setContent(cib);
        TrackRequestResult trackResult = track(mike(), eib);
        assertNotNull("FortressUser in the Header, but not in Content, should work", trackResult);
        EntityBean e = getEntity(mike(), trackResult.getKey());

        assertEquals("usera", e.getLastUser());
        assertEquals("usera", e.getCreatedUser());
    }

    @Test
    public void entity_findLogs() throws Exception {
        FortressResultBean f = makeFortress(mike(),  new FortressInputBean("entity_findLogs", true));
        EntityInputBean eib = new EntityInputBean(f.getName(), "DocType");
        eib.setFortressUser("userA");
        ContentInputBean cib = new ContentInputBean(EntityContentHelper.getRandomMap());
        eib.setContent(cib);
        TrackRequestResult trackResult = track(mike(), eib);
        assertNotNull(trackResult);
        Collection<EntityLog> entityLogs = getEntityLogs(mike(), trackResult.getKey());
        assertEquals(1, entityLogs.size());
    }

    @Test
    public void entity_findLogsWithIllegalEntity() throws Exception {
        FortressResultBean f = makeFortress(mike(),  new FortressInputBean("entity_findLogsWithIllegalEntity", true));
        EntityInputBean eib = new EntityInputBean(f.getName(), "DocType");
        eib.setFortressUser("userA");
        ContentInputBean cib = new ContentInputBean(EntityContentHelper.getRandomMap());
        eib.setContent(cib);
        // Test Serialization
        byte[] bytes =JsonUtils.toJsonBytes(eib);
        eib = JsonUtils.toObject(bytes,EntityInputBean.class);

        TrackRequestResult trackResult = track(mike(), eib);
        assertNotNull(trackResult);
        login("mike", "123");
        getEntityLogsIllegalEntity(mike(), trackResult.getKey() +"123");

    }

    @Test
    public void fortress_DuplicateNameWithProperties() throws Exception{
        String fName= "fortress_ResultProperties";
//        cleanUpGraph();
        FortressInputBean fortressInputBean = new FortressInputBean(fName);
        FortressResultBean result = makeFortress(sally(), fortressInputBean);
        assertEquals(fName, result.getName());
        assertEquals(fName.toLowerCase(), result.getCode());

        // Create the same fortress with a lowercase name.
        fortressInputBean = new FortressInputBean(fName.toLowerCase());
        result = makeFortress(mike(), fortressInputBean);
        assertEquals(fName, result.getName());
        assertEquals(fName.toLowerCase(), result.getCode());
        assertNotNull("Index Name not found", result.getIndexName());

        FortressResultBean fortress = getFortress(mike(), result.getCode());
        assertEquals(fortress.getRootIndex(), result.getIndexName());
        assertEquals("Creation of a fortress should be case insensitive", 1, getFortresses(mike()).size());

    }



}
