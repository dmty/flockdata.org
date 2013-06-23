package com.auditbucket.test.load;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.service.AuditSearchService;
import com.auditbucket.audit.service.AuditService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import org.apache.commons.lang.time.StopWatch;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * User: mike
 * Date: 26/04/13
 * Time: 8:15 PM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:root-context.xml")
@Transactional

public class TestAuditIntegration {
    @Autowired
    AuditService auditService;

    @Autowired
    AuditSearchService auditSearchService;

    @Autowired
    RegistrationService regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    private Neo4jTemplate template;
    private Logger log = LoggerFactory.getLogger(TestAuditIntegration.class);

    private String monowai = "Monowai";
    private String hummingbird = "Hummingbird";
    private String mike = "mike@monowai.com";
    private String gina = "gina@hummingbird.com";
    String what = "{\"house\": \"house";

    Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "user1");
    Authentication authGina = new UsernamePasswordAuthenticationToken(gina, "user1");

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        SecurityContextHolder.getContext().setAuthentication(authMike);
        //Neo4jHelper.cleanDb(template);
    }

    @Test
    public void testHeader() throws Exception {

        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        regService.registerSystemUser(new RegistrationBean(hummingbird, gina, "bah"));
        //Monowai/Mike
        SecurityContextHolder.getContext().setAuthentication(authMike);
        IFortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "wally", "Company", new Date(), "AHWP");
        String ahWP = auditService.createHeader(inputBean).getAuditKey();
        assertNotNull(ahWP);
        assertNotNull(auditService.getHeader(ahWP));

        //Hummingbird/Gina
        SecurityContextHolder.getContext().setAuthentication(authGina);
        IFortress fortHS = fortressService.registerFortress(new FortressInputBean("honeysuckle", true));
        inputBean = new AuditHeaderInputBean(fortHS.getName(), "harry", "Company", new Date(), "AHHS");
        String ahHS = auditService.createHeader(inputBean).getAuditKey();

        assertNotNull(fortressService.getFortressUser(fortWP, "wally", true));
        assertNotNull(fortressService.getFortressUser(fortHS, "harry", true));
        assertNull(fortressService.getFortressUser(fortWP, "wallyz", false));

        double max = 2000d;
        StopWatch watch = new StopWatch();
        watch.start();

        createLogRecords(authMike, ahWP, what + "\"}", 20);
        createLogRecords(authGina, ahHS, what + "\"}", 40);
        try {
            Thread.sleep(5000l);
        } catch (InterruptedException e) {

            log.error(e.getMessage());
        }
        Long hitCount = auditSearchService.getHitCount("hummingbird.*");

        assertTrue(hitCount != null && hitCount > 1); // Sometimes this fails. It seems to be that the data has not been stored by ES by the time we make this call
        hitCount = auditSearchService.getHitCount("monowai.*");
        assertTrue(hitCount > 1);
        watch.stop();
        log.info("End " + watch.getTime() / 1000d + " avg = " + (watch.getTime() / 1000d) / max);


    }

    @Test
    public void testLastChanged() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        // Create a second log record in order to workout who last change the AuditHeader
        SecurityContextHolder.getContext().setAuthentication(authMike);

        IFortress fortWP = fortressService.registerFortress("wportfolio");
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "wally", "Company", new Date(), "ZZZZ");
        String ahWP = auditService.createHeader(inputBean).getAuditKey();
        IAuditHeader auditKey = auditService.getHeader(ahWP);
        auditService.createLog(new AuditLogInputBean(auditKey.getAuditKey(), "olivia@sunnybell.com", new DateTime(), what + "\"}", "Update"));
        auditKey = auditService.getHeader(ahWP);
        IFortressUser fu = fortressService.getUser(auditKey.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getName());

    }

    /**
     * test that we find the correct number of changes between a range of dates for a given header
     */
    @Test
    public void testInRange() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        // Create a second log record in order to workout who last change the AuditHeader
        SecurityContextHolder.getContext().setAuthentication(authMike);

        int max = 10;
        IFortress fortWP = fortressService.registerFortress("wportfolio");
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(max);
        DateTime workingDate = firstDate.toDateTime();

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "wally", "Company", firstDate.toDate(), "123");
        String ahWP = auditService.createHeader(inputBean).getAuditKey();
        IAuditHeader auditHeader = auditService.getHeader(ahWP);
        int i = 0;
        while (i < max) {
            workingDate = workingDate.plusDays(1);
            assertEquals(AuditService.LogStatus.OK, auditService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", workingDate, what + i + "\"}")).getLogStatus());

            log.info("Created " + i + " new count =" + auditService.getAuditLogCount(auditHeader.getAuditKey()));
            i++;
        }

        Set<IAuditLog> aLogs = auditService.getAuditLogs(auditHeader.getAuditKey());
        assertEquals(max, aLogs.size());

        IAuditLog lastChange = auditService.getLastChange(auditHeader.getAuditKey());
        assertNotNull(lastChange);
        assertEquals(workingDate.toDate(), lastChange.getWhen());
        auditHeader = auditService.getHeader(ahWP);
        assertEquals(max, auditService.getAuditLogCount(auditHeader.getAuditKey()));

        DateTime then = workingDate.minusDays(4);
        log.info("Searching between " + then.toDate() + " and " + workingDate.toDate());
        Set<IAuditLog> logs = auditService.getAuditLogs(auditHeader.getAuditKey(), then.toDate(), workingDate.toDate());
        assertEquals(5, logs.size());

    }

    @Test
    public void testCancelLastChange() throws Exception {
        // For use in compensating transaction cases only
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        IFortress fortWP = fortressService.registerFortress("wportfolio");
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(2);
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "olivia@sunnybell.com", "Company", firstDate.toDate(), "ABC1");
        String ahWP = auditService.createHeader(inputBean).getAuditKey();

        IAuditHeader auditHeader = auditService.getHeader(ahWP);
        auditService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", firstDate, what + 1 + "\"}"));
        auditService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "isabella@sunnybell.com", firstDate.plusDays(1), what + 2 + "\"}"));
        Set<IAuditLog> logs = auditService.getAuditLogs(auditHeader.getAuditKey());
        assertEquals(2, logs.size());
        auditHeader = auditService.getHeader(ahWP);
        compareUser(auditHeader, "isabella@sunnybell.com");
        auditHeader = auditService.cancelLastLog(auditHeader.getAuditKey());
        assertNotNull(auditHeader);
        compareUser(auditHeader, "olivia@sunnybell.com");
        auditHeader = auditService.cancelLastLog(auditHeader.getAuditKey());
        assertNull(auditHeader);
    }

    private void compareUser(IAuditHeader header, String userName) {
        IFortressUser fu = fortressService.getUser(header.getLastUser().getId());
        assertEquals(userName, fu.getName());

    }

    private void createLogRecords(Authentication auth, String auditHeader, String textToUse, double recordsToCreate) throws Exception {
        int i = 0;
        SecurityContextHolder.getContext().setAuthentication(auth);
        String action = "Create";
        while (i < recordsToCreate) {
            if (i == 1)
                action = "Update";
            auditService.createLog(new AuditLogInputBean(auditHeader, "wally", new DateTime(), textToUse + i + "}", action));
            i++;
        }
        assertEquals(recordsToCreate, (double) auditService.getAuditLogCount(auditHeader));
    }

    public void testBigLoad() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        int fortressCount = 20;
        int auditCount = 100;
        int logCount = 5;
        String escJson = "{\"who\":";
        int fortress = 1;
        ArrayList<Long> list = new ArrayList<Long>();
        StopWatch watch = new StopWatch();
        watch.start();
        double splits = 0;
        log.info("FortressCount: " + fortressCount + " AuditCount: " + auditCount + " LogCount: " + logCount);
        while (fortress <= fortressCount) {

            String fortressName = "bulkloada" + fortress;
            IFortress iFortress = fortressService.registerFortress(new FortressInputBean(fortressName, false));
            int audit = 1;
            while (audit <= auditCount) {
                AuditHeaderInputBean aib = new AuditHeaderInputBean(iFortress.getName(), fortress + "olivia@sunnybell.com", "Company", new Date(), "ABC" + audit);
                aib = auditService.createHeader(aib);
                int log = 1;
                while (log <= logCount) {
                    auditService.createLog(new AuditLogInputBean(aib.getAuditKey(), aib.getFortressUser(), new DateTime(), escJson + fortress + "}"));
                    log++;
                }
                audit++;
            }
            watch.split();

            log.info(iFortress.getName() + " took " + (watch.getSplitTime() / 1000d));
            splits = splits + watch.getSplitTime();
            watch.reset();
            watch.start();
            list.add(iFortress.getId());
            fortress++;
        }

        log.info("Created data set");
        double sub = splits / 1000d;
        log.info("Created data set in " + sub + " fortress avg = " + sub / fortressCount + " avg seconds per row " + sub / (fortressCount * auditCount * logCount) + " rows per second " + (fortressCount * auditCount * logCount) / sub);
        watch.reset();
        watch.start();

        int searchLoops = 2;
        int search = 0;
        int totalSearchRequests = 0;
        watch.split();
        do {
            fortress = 0;
            do {
                int x = 1;
                do {
                    int random = (int) (Math.random() * ((auditCount) + 1));
                    if (random == 0)
                        random = 1;
                    assertNotNull("ABC" + random, auditService.findByName(list.get(fortress), "Company", "ABC" + random));
                    totalSearchRequests++;
                    x++;
                } while (x < auditCount);
                fortress++;
            } while (fortress < fortressCount);
            search++;
        } while (search < searchLoops);

        watch.stop();
        double end = watch.getTime() / 1000d;
        log.info("Total Search Requests = " + totalSearchRequests + ". Total time for searches " + end + " avg requests per second = " + totalSearchRequests / end);
    }
}