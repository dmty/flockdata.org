package com.auditbucket.test.functional;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.CrossReferenceInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.MetaKey;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.Assert.*;

/**
 * User: mike
 * Date: 1/04/14
 * Time: 4:12 PM
 */
@Transactional
public class TestMetaCrossReference extends TestEngineBase {

    @Test
    public void crossReferenceMetaKeysForSameCompany() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String sourceKey = trackEP.trackHeader(inputBean, null, null).getBody().getMetaKey();

        assertNotNull(sourceKey);

        Collection<String> xRef = new ArrayList<>();
        inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        String destKey = trackEP.trackHeader(inputBean, null, null).getBody().getMetaKey();
        assertNotNull(destKey);
        assertFalse(destKey.equals(sourceKey));

        xRef.add(destKey);
        xRef.add("NonExistent");
        Collection<String> notFound = trackEP.putCrossReference(sourceKey, xRef, "cites", null, null);
        assertEquals(1, notFound.size());
        assertEquals("NonExistent", notFound.iterator().next());

        Map<String, Collection<MetaHeader>> results = trackEP.getCrossRefenceByMetaKey(sourceKey, "cites", null, null);
        assertNotNull ( results);
        assertEquals(1, results.size());
        Collection<MetaHeader> headers = results.get("cites");
        assertNotNull ( headers);
        for (MetaHeader header : headers) {
            assertEquals(destKey, header.getMetaKey());
        }


    }

    @Test
    public void duplicateCallerRefForFortressFails() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String sourceKey = trackEP.trackHeader(inputBean, null, null).getBody().getMetaKey();

        assertNotNull(sourceKey);

        // Check that exception is thrown if the callerRef is not unique for the fortress
        Collection<MetaKey> xRef = new ArrayList<>();
        inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        String destKey = trackEP.trackHeader(inputBean, null, null).getBody().getMetaKey();
        assertNotNull(destKey);
        assertFalse(destKey.equals(sourceKey));

        xRef.add(new MetaKey("ABC321"));
        xRef.add(new MetaKey("Doesn't matter"));
        try {
            trackEP.postCrossReferenceByCallerRef(fortress.getName(), sourceKey, xRef, "cites", null, null);
            fail("Exactly one check failed");
        } catch ( DatagioException e ){
            // good stuff!
        }
    }

    @Test
    public void crossReferenceByCallerRefsForFortress() throws Exception {
        registerSystemUser(monowai, mike_admin);
        Fortress fortress = fortressService.registerFortress(new FortressInputBean("auditTest", true));

        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        trackEP.trackHeader(inputBean, null, null).getBody();

        Collection<MetaKey> callerRefs = new ArrayList<>();
        // These are the two records that will cite the previously created header
        inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        trackEP.trackHeader(inputBean, null, null).getBody();
        inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeS", new DateTime(), "ABC333");
        trackEP.trackHeader(inputBean, null, null).getBody();

        callerRefs.add(new MetaKey("ABC321"));
        callerRefs.add(new MetaKey("ABC333"));

        Collection<MetaKey> notFound = trackEP.postCrossReferenceByCallerRef(fortress.getName(), "ABC123", callerRefs, "cites", null, null);
        assertEquals(0, notFound.size());
        Map<String, Collection<MetaHeader>> results = trackEP.getCrossReferenceByCallerRef(fortress.getName(), "ABC123", "cites", null, null);
        assertNotNull ( results);
        assertEquals(1, results.size());
        Collection<MetaHeader> headers = results.get("cites");
        assertNotNull ( headers);
        int count = 0;
        for (MetaHeader header : headers) {
            count ++;
        }
        assertEquals(2, count);
    }
    @Test
    public void crossReferenceWithInputBean() throws Exception {
        registerSystemUser(monowai, mike_admin);
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("auditTest", true));

        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        trackEP.trackHeader(inputBean, null, null).getBody();

        // These are the two records that will cite the previously created header
        MetaInputBean inputBeanB = new MetaInputBean(fortressA.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        trackEP.trackHeader(inputBeanB, null, null).getBody();
        MetaInputBean inputBeanC = new MetaInputBean(fortressA.getName(), "wally", "DocTypeS", new DateTime(), "ABC333");
        trackEP.trackHeader(inputBeanC, null, null).getBody();
        Map<String, List<MetaKey>> refs = new HashMap<>();
        List<MetaKey> callerRefs = new ArrayList<>();

        callerRefs.add(new MetaKey("ABC321"));
        callerRefs.add(new MetaKey("ABC333"));

        refs.put("cites",callerRefs);
        CrossReferenceInputBean bean = new CrossReferenceInputBean(fortressA.getName(), "ABC123",refs);
        List<CrossReferenceInputBean > inputs = new ArrayList<>();
        inputs.add(bean);

        List<CrossReferenceInputBean> notFound = trackEP.postCrossReferenceByCallerRef(inputs, null, null);
        assertEquals(1, notFound.size());
        for (CrossReferenceInputBean crossReferenceInputBean : notFound) {
            assertTrue(crossReferenceInputBean.getIgnored().get("cites").isEmpty());
        }

        Map<String, Collection<MetaHeader>> results = trackEP.getCrossReferenceByCallerRef(fortressA.getName(), "ABC123", "cites", null, null);
        assertNotNull ( results);
        assertEquals(1, results.size());
        Collection<MetaHeader> headers = results.get("cites");
        assertNotNull ( headers);
        int count = 0;
        for (MetaHeader header : headers) {
            count ++;
        }
        assertEquals(2, count);
    }
    @Test
    public void crossXRefDifferentFortresses() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTestA", true));
        Fortress fortressB = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTestB", true));

        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        trackEP.trackHeader(inputBean, null, null).getBody();

        Map<String, List<MetaKey>> refs = new HashMap<>();
        List<MetaKey> callerRefs = new ArrayList<>();

        callerRefs.add(new MetaKey(fortressA.getName(), "DocTypeZ", "ABC321"));
        callerRefs.add(new MetaKey(fortressB.getName(), "DocTypeS", "ABC333"));

        refs.put("cites",callerRefs);
        CrossReferenceInputBean bean = new CrossReferenceInputBean(fortressA.getName(), "ABC123",refs);
        List<CrossReferenceInputBean > inputs = new ArrayList<>();
        inputs.add(bean);

        List<CrossReferenceInputBean> notFound = trackEP.postCrossReferenceByCallerRef(inputs, null, null);
        assertEquals(2, notFound.iterator().next().getIgnored().get("cites").size());

        // These are the two records that will cite the previously created header
        MetaInputBean inputBeanB = new MetaInputBean(fortressA.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        trackEP.trackHeader(inputBeanB, null, null).getBody();
        MetaInputBean inputBeanC = new MetaInputBean(fortressB.getName(), "wally", "DocTypeS", new DateTime(), "ABC333");
        trackEP.trackHeader(inputBeanC, null, null).getBody();
        notFound = trackEP.postCrossReferenceByCallerRef(inputs, null, null);
        assertEquals(0, notFound.iterator().next().getIgnored().get("cites").size());

        Map<String, Collection<MetaHeader>> results = trackEP.getCrossReferenceByCallerRef(fortressA.getName(), "ABC123", "cites", null, null);
        assertNotNull ( results);
        assertEquals("Unexpected cites count", 2, results.get("cites").size());
        Collection<MetaHeader> headers = results.get("cites");
        assertNotNull ( headers);
        int count = 0;
        for (MetaHeader header : headers) {
            count ++;
        }
        assertEquals(2, count);
    }
}
