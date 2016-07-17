/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.services;

import junit.framework.TestCase;
import org.flockdata.helper.FlockException;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.*;
import org.joda.time.DateTime;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.springframework.data.neo4j.conversion.Result;

import java.util.*;

import static org.junit.Assert.*;

/**
 * User: mike
 * Date: 1/04/14
 * Time: 4:12 PM
 */
public class TestEntityCrossLink extends EngineBase {

    public static final String PARENT = "parent";

    /**
     * Foundation assumption.
     *
     * The parent already exists. We want to link a child entity to it
     *
     * Cypher pattern is Parent-[]->Child
     *
     * @throws Exception
     */
    @Test
    public void link_parentChildDirectionCorrect() throws Exception {
        //
        cleanUpGraph();
        SystemUser su = registerSystemUser("xRef_FromInputBeans", mike_admin);
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));


        EntityInputBean parent = new EntityInputBean(fortressA, "wally", "DocTypeA", new DateTime(), "ABC123");
        TrackResultBean parentResult = mediationFacade.trackEntity(su.getCompany(), parent);

        DocumentType childDoc = new DocumentType(fortressA, "DocTypeZ");
        childDoc.setParent(parentResult.getDocumentType());
        childDoc = conceptService.findOrCreate(fortressA, childDoc);
        assertTrue(childDoc.hasParent());
        assertEquals(childDoc.getParent().getId(), parentResult.getDocumentType().getId());

        EntityInputBean child = new EntityInputBean(fortressA, "wally", "DocTypeZ", new DateTime(), "ABC321");

        TrackResultBean childResult = mediationFacade.trackEntity(su.getCompany(), child);

        EntityKeyBean parentKey = new EntityKeyBean(parent.getDocumentType().getName(), parent.getFortress(), parent.getCode());
        EntityKeyBean childKey = new EntityKeyBean(child.getDocumentType().getName(), child.getFortress(), child.getCode());

        Collection<EntityKeyBean> parents = new ArrayList<>();
        parents.add(parentKey);
        entityService.linkEntities(su.getCompany(), childKey, parents, PARENT);


        String cypher = "match (parent:Entity)-[p:"+PARENT+"]->(child:Entity) where id(parent)={parentId} return child";
        Map<String, Object> params = new HashMap<>();
        params.put("parentId", parentResult.getEntity().getId());
        Result<Map<String, Object>> results = neo4jTemplate.query(cypher, params);
        boolean found = false;
        for (Map<String, Object> result : results) {
            Node node = (Node) result.get("child");
            assertEquals(childResult.getEntity().getId().longValue(), node.getId());
            found = true;

        }
        assertTrue("We couldn't find the child connected to the parent", found);
        Map<String, Collection<Entity>> linked = entityService.getCrossReference(su.getCompany(), parentResult.getKey(), PARENT);
        assertTrue(linked.containsKey(PARENT));
        assertEquals(childResult.getEntity().getId(), linked.get(PARENT).iterator().next().getId());

        SearchChange searchChange = searchService.getEntityChange(childResult);
        assertNotNull(searchChange.getParent());
        assertEquals(parent.getCode(), searchChange.getParent().getCode());


    }

    @Test
    public void link_KeysForSameCompany() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser("xRef_KeysForSameCompany", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "DocTypeA", new DateTime(), "ABC123");
        TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);

        assertNotNull(trackResultBean);
        String sourceKey = trackResultBean.getEntity().getKey();

        Collection<String> xRef = new ArrayList<>();
        inputBean = new EntityInputBean(fortress, "wally", "DocTypeZ", new DateTime(), "ABC321");
        TrackResultBean destBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(destBean);
        String destKey = destBean.getEntity().getKey();
        assertFalse(destKey.equals(sourceKey));

        xRef.add(destKey);
        xRef.add("NonExistent");
        Collection<String> notFound = entityService.crossReference(su.getCompany(), sourceKey, xRef, "cites");
        assertEquals(1, notFound.size());
        assertEquals("NonExistent", notFound.iterator().next());

        Map<String, Collection<Entity>> results = entityService.getCrossReference(su.getCompany(), sourceKey, "cites");
        assertNotNull(results);
        assertEquals(1, results.size());
        Collection<Entity> entities = results.get("cites");
        assertNotNull(entities);
        for (Entity entity : entities) {
            assertEquals(destKey, entity.getKey());
        }


    }

    @Test
    public void link_targetDoesNotExist() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser("xRef_targetDoesNotExist", mike_admin);
        FortressInputBean fib = new FortressInputBean("xRef_targetDoesNotExist", true);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);

        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "DocTypeA", new DateTime(), "ABC123");
        TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String abc123 = trackResultBean.getEntity().getKey();

        assertNotNull(trackResultBean);

        List<EntityKeyBean> callerRefs = new ArrayList<>();
        // DAT-443 - Request to xreference with an entity that does not yet exist.
        // Will only work when both the fortress and doctype are known i.e. not a DocType of "*"
        callerRefs.add(
                new EntityKeyBean(trackResultBean.getDocumentType().getName(), fib, "ABC321")
                        .setMissingAction(EntityKeyBean.ACTION.CREATE));

        EntityKeyBean sourceKey = new EntityKeyBean(new EntityToEntityLinkInput(inputBean));

        Collection<EntityKeyBean> results = entityService.linkEntities(su.getCompany(), sourceKey, callerRefs, "anyrlx");
        TestCase.assertTrue("",results.isEmpty());

        inputBean = new EntityInputBean(fortress, "wally", "DocTypeA", new DateTime(), "ABC321");
        ContentInputBean cib = new ContentInputBean(EntityContentHelper.getRandomMap());
        inputBean.setContent(cib);

        // The Entity, that previously did not exist, can have a log added and be treated like any other entity
        trackResultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        TestCase.assertTrue(trackResultBean.entityExists());
        TestCase.assertEquals(ContentInputBean.LogStatus.OK, trackResultBean.getLogStatus());

        // (ABC123)-[anyrlx]-(ABC321)
        // Retrieving 123 returns 321
        Map<String, Collection<Entity>> xrefResults = entityService.getCrossReference(su.getCompany(), abc123, "anyrlx");
        TestCase.assertFalse(xrefResults.isEmpty());
        Collection<Entity> entities = xrefResults.get("anyrlx");
        assertEquals(1, entities.size());
        assertEquals("ABC321", entities.iterator().next().getCode());

        // Inverse of above - 321 returns 123
        xrefResults = entityService.getCrossReference(su.getCompany(), trackResultBean.getEntity().getKey(), "anyrlx");
        TestCase.assertFalse(xrefResults.isEmpty());
        entities = xrefResults.get("anyrlx");
        assertEquals(1, entities.size());
        assertEquals("ABC123", entities.iterator().next().getCode());

    }

    @Test
    public void link_duplicateCallerRefForFortressFails() throws Exception {
        SystemUser su = registerSystemUser("xRef_duplicateCallerRefForFortressFails", mike_admin);
        FortressInputBean fib = new FortressInputBean("auditTest", true);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);

        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "DocTypeA", new DateTime(), "ABC123");
        String code = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();

        assertNotNull(code);

        // Check that exception is thrown if the code is not unique for the fortress
        Collection<EntityKeyBean> xRef = new ArrayList<>();
        inputBean = new EntityInputBean(fortress, "wally", "DocTypeZ", new DateTime(), "ABC321");
        TrackResultBean destKey = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(destKey);
        assertFalse(code.equals(destKey.getKey()));

        xRef.add(new EntityKeyBean("ABC321", new FortressInputBean("123"), "444"));
        xRef.add(new EntityKeyBean("Doesn't matter", new FortressInputBean("123"), "444"));
        try {
            EntityKeyBean entityKey = new EntityKeyBean("*", fib, code);
            entityKey.setMissingAction(EntityKeyBean.ACTION.ERROR);
            entityService.linkEntities(su.getCompany(), entityKey, xRef, "cites");
            fail("Exactly one check failed");
        } catch (FlockException e) {
            // good stuff!
        }
    }

    @Test
    public void link_ByCallerRefsForFortress() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser("xRef_ByCallerRefsForFortress", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "DocTypeA", new DateTime(), "ABC123");
        TrackResultBean trDocA = mediationFacade.trackEntity(su.getCompany(), inputBean);

        Collection<EntityKeyBean> codeRef = new ArrayList<>();
        // These are the two records that will cite the previously created entity
        inputBean = new EntityInputBean(fortress, "wally", "DocTypeE", new DateTime(), "ABC321");
        TrackResultBean trDocE = mediationFacade.trackEntity(su.getCompany(), inputBean);
        inputBean = new EntityInputBean(fortress, "wally", "DocTypeF", new DateTime(), "ABC333");
        TrackResultBean trDocF = mediationFacade.trackEntity(su.getCompany(), inputBean);

        codeRef.add(new EntityKeyBean(trDocE.getEntityInputBean()));
        codeRef.add(new EntityKeyBean(trDocF.getEntityInputBean()));

        EntityKeyBean entityKey = new EntityKeyBean("*", fortress, "ABC123");
        Collection<EntityKeyBean> notFound = entityService.linkEntities(su.getCompany(), entityKey, codeRef, "cites");
        assertEquals(0, notFound.size());
        Map<String, Collection<Entity>> results = entityService.getCrossReference(su.getCompany(), fortress.getName(), "ABC123", "cites");
        assertNotNull(results);
        assertEquals(1, results.size());
        Collection<Entity> entities = results.get("cites");
        assertNotNull(entities);
        int count = 0;
        for (Entity entity : entities) {
            assertNotNull(entity);
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void link_FromInputBeans() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser("xRef_FromInputBeans", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "DocTypeA", new DateTime(), "ABC123");
        mediationFacade.trackEntity(su.getCompany(), inputBean);

        // These are the two records that will cite the previously created entity
        EntityInputBean inputBeanB = new EntityInputBean(fortress, "wally", "DocTypeZ", new DateTime(), "ABC321");
        mediationFacade.trackEntity(su.getCompany(), inputBeanB);
        EntityInputBean inputBeanC = new EntityInputBean(fortress, "wally", "DocTypeS", new DateTime(), "ABC333");
        mediationFacade.trackEntity(su.getCompany(), inputBeanC);
        Map<String, List<EntityKeyBean>> refs = new HashMap<>();
        List<EntityKeyBean> callerRefs = new ArrayList<>();

        callerRefs.add(new EntityKeyBean(inputBeanB));
        callerRefs.add(new EntityKeyBean(inputBeanC));

        refs.put("cites", callerRefs);
        inputBean.setEntityLinks(refs);
        EntityToEntityLinkInput bean = new EntityToEntityLinkInput(inputBean);
        List<EntityToEntityLinkInput> entities = new ArrayList<>();
        entities.add(bean);

        Collection<EntityToEntityLinkInput> notFound = entityService.linkEntities(su.getCompany(), entities);
        assertEquals(1, notFound.size());
        for (EntityToEntityLinkInput crossReferenceInputBean : notFound) {
            assertTrue(crossReferenceInputBean.getIgnored().get("cites").isEmpty());
        }

        Map<String, Collection<Entity>> results = entityService.getCrossReference(su.getCompany(), fortress.getName(), "ABC123", "cites");
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(2, results.get("cites").size());
    }

    @Test
    public void link_AcrossFortressBoundaries() throws Exception {
        SystemUser su = registerSystemUser("xRef_AcrossFortressBoundaries", mike_admin);
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTestA", true));
        Fortress fortressB = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTestB", true));

        EntityInputBean inputBean = new EntityInputBean(fortressA, "wally", "DocTypeA", new DateTime(), "ABC123");
        mediationFacade.trackEntity(su.getCompany(), inputBean);

        Map<String, List<EntityKeyBean>> refs = new HashMap<>();
        List<EntityKeyBean> entityKeys = new ArrayList<>();

        entityKeys.add(new EntityKeyBean("DocTypeZ", fortressA, "ABC321"));
        entityKeys.add(new EntityKeyBean("DocTypeS", fortressB, "ABC333"));

        refs.put("cites", entityKeys);
        inputBean.setEntityLinks(refs);

        EntityToEntityLinkInput bean = new EntityToEntityLinkInput(inputBean);
        List<EntityToEntityLinkInput> inputs = new ArrayList<>();
        inputs.add(bean);

        Collection<EntityToEntityLinkInput> notFound = entityService.linkEntities(su.getCompany(), inputs);
        assertEquals(2, notFound.iterator().next().getIgnored().get("cites").size());

        // These are the two records that will cite the previously created entity
        EntityInputBean inputBeanB = new EntityInputBean(fortressA, "wally", "DocTypeZ", new DateTime(), "ABC321");
        mediationFacade.trackEntity(su.getCompany(), inputBeanB);
        EntityInputBean inputBeanC = new EntityInputBean(fortressB, "wally", "DocTypeS", new DateTime(), "ABC333");
        mediationFacade.trackEntity(su.getCompany(), inputBeanC);
        notFound = entityService.linkEntities(su.getCompany(), inputs);
        assertEquals(0, notFound.iterator().next().getIgnored().get("cites").size());

        Map<String, Collection<Entity>> results = entityService.getCrossReference(su.getCompany(), fortressA.getName(), "ABC123", "cites");
        assertNotNull(results);
        assertEquals("Unexpected cites count", 2, results.get("cites").size());
        Collection<Entity> entities = results.get("cites");
        assertNotNull(entities);
        int count = 0;
        for (Entity entity : entities) {
            assertNotNull(entity);
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void link_CreatesUniqueRelationships() throws Exception {
        SystemUser su = registerSystemUser("xRef_CreatesUniqueRelationships", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("xRef_CreatesUniqueRelationships", true));

        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "DocTypeA", new DateTime(), "ABC123");
        String sourceKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();

        assertNotNull(sourceKey);

        Collection<String> xRef = new ArrayList<>();
        inputBean = new EntityInputBean(fortress, "wally", "DocTypeZ", new DateTime(), "ABC321");
        String destKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();
        assertNotNull(destKey);
        assertFalse(destKey.equals(sourceKey));

        xRef.add(destKey);
        entityService.crossReference(su.getCompany(), sourceKey, xRef, "cites");
        // Try and force a duplicate relationship - only 1 should be created
        entityService.crossReference(su.getCompany(), sourceKey, xRef, "cites");

        Map<String, Collection<Entity>> results = entityService.getCrossReference(su.getCompany(), sourceKey, "cites");
        assertNotNull(results);
        assertEquals(1, results.size());
        Collection<Entity> entities = results.get("cites");
        assertEquals("Tracking the same relationship name between two entities should not create duplicate relationships", 1, entities.size());
    }


}