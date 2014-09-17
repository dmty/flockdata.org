/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.endpoint;

import com.auditbucket.engine.service.FortressService;
import com.auditbucket.engine.service.TxService;
import com.auditbucket.helper.CompanyResolver;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.NotFoundException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.kv.service.KvService;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.track.bean.*;
import com.auditbucket.track.model.*;
import com.auditbucket.track.service.EntityTagService;
import com.auditbucket.track.service.LogService;
import com.auditbucket.track.service.TrackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@RestController
@RequestMapping("/track")
public class TrackEP {
    @Autowired
    TrackService trackService;

    @Autowired
    com.auditbucket.track.service.MediationFacade mediationFacade;

    @Autowired
    FortressService fortressService;

    @Autowired
    EntityTagService entityTagService;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    CompanyService companyService;

    @Autowired
    KvService kvService;

    @Autowired
    TxService txService;

    @Autowired
    LogService logService;

    private static Logger logger = LoggerFactory.getLogger(TrackEP.class);


    @RequestMapping(value = "/", consumes = "application/json", method = RequestMethod.PUT)
    public Collection<TrackResultBean> trackHeaders(@RequestBody List<EntityInputBean> inputBeans,
                                                    HttpServletRequest request) throws DatagioException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        return mediationFacade.trackHeaders(company, inputBeans);
    }

    /**
     * Tracks an entity
     *
     * @param input meta header input
     * @return TrackResultBean
     * @throws com.auditbucket.helper.DatagioException
     */
    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    public TrackResultBean trackEntity(@RequestBody EntityInputBean input ,
                                                       HttpServletRequest request) throws DatagioException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        TrackResultBean trackResultBean;
        trackResultBean = mediationFacade.trackEntity(company, input);
        trackResultBean.setServiceMessage("OK");
        return trackResultBean;

    }


    @RequestMapping(value = "/log/", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
    public ResponseEntity<LogResultBean> trackLog(@RequestBody ContentInputBean input ,
                                                  HttpServletRequest request) throws DatagioException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);

        LogResultBean resultBean = mediationFacade.trackLog(company, input).getLogResult();
        ContentInputBean.LogStatus ls = resultBean.getStatus();
        if (ls.equals(ContentInputBean.LogStatus.FORBIDDEN))
            return new ResponseEntity<>(resultBean, HttpStatus.FORBIDDEN);
        else if (ls.equals(ContentInputBean.LogStatus.NOT_FOUND)) {
            input.setAbMessage("Illegal meta key");
            return new ResponseEntity<>(resultBean, HttpStatus.NOT_FOUND);
        } else if (ls.equals(ContentInputBean.LogStatus.IGNORE)) {
            input.setAbMessage("Ignoring request to change as the 'what' has not changed");
            return new ResponseEntity<>(resultBean, HttpStatus.NOT_MODIFIED);
        } else if (ls.equals(ContentInputBean.LogStatus.ILLEGAL_ARGUMENT)) {
            return new ResponseEntity<>(resultBean, HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(resultBean, HttpStatus.OK);
    }


    @RequestMapping(value = "/{fortress}/{recordType}/{callerRef}", method = RequestMethod.PUT)
    public ResponseEntity<TrackResultBean> trackByClientRef(@RequestBody EntityInputBean input,
                                                            @PathVariable("fortress") String fortress,
                                                            @PathVariable("recordType") String recordType,
                                                            @PathVariable("callerRef") String callerRef ,
                                                            HttpServletRequest request) throws DatagioException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        TrackResultBean trackResultBean;
        input.setFortress(fortress);
        input.setDocumentType(recordType);
        input.setCallerRef(callerRef);
        input.setMetaKey(null);
        trackResultBean = mediationFacade.trackEntity(company, input);
        trackResultBean.setServiceMessage("OK");
        return new ResponseEntity<>(trackResultBean, HttpStatus.OK);

    }


    @RequestMapping(value = "/{fortress}/all/{callerRef}", method = RequestMethod.GET)
    public Iterable<Entity> findByCallerRef(@PathVariable("fortress") String fortress, @PathVariable("callerRef") String callerRef,
                                            HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        return trackService.findByCallerRef(company, fortress, callerRef);  //To change body of created methods use File | Settings | File Templates.
    }


    @RequestMapping(value = "/{fortress}/{documentType}/{callerRef}", method = RequestMethod.GET)
    public Entity findByCallerRef(@PathVariable("fortress") String fortressName,
                                  @PathVariable("documentType") String recordType,
                                  @PathVariable("callerRef") String callerRef,
                                  HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        Fortress fortress = fortressService.findByName(company, fortressName);
        return trackService.findByCallerRef(fortress, recordType, callerRef);
    }


    @RequestMapping(value = "/{metaKey}", method = RequestMethod.GET)
    public ResponseEntity<Entity> getEntity(@PathVariable("metaKey") String metaKey ,
                                            HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/{metaKey}
        Entity result = trackService.getEntity(company, metaKey, true);
        if (result == null)
            throw new DatagioException("Unable to resolve requested meta key [" + metaKey + "]. Company is " + (company == null ? "Invalid" : "Valid"));

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * locates a collection of Entity based on incoming collection of MetaKeys
     *
     * @param toFind       keys to look for
     * @return Matching entities you are authorised to receive
     * @throws DatagioException
     */

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public Collection<Entity> getEntities(@RequestBody Collection<String> toFind ,
                                          HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        return trackService.getEntities(company, toFind).values();
    }

    @RequestMapping(value = "/{metaKey}/logs", produces = "application/json", method = RequestMethod.GET)
    public Set<EntityLog> getLogs(@PathVariable("metaKey") String metaKey,
                                  HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/{metaKey}/logs
        return trackService.getEntityLogs(company, metaKey);

    }


    @RequestMapping(value = "/{metaKey}/summary", produces = "application/json", method = RequestMethod.GET)
    public EntitySummaryBean getEntitySummary(@PathVariable("metaKey") String metaKey,
                                              HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        return mediationFacade.getEntitySummary(company, metaKey);

    }


    @RequestMapping(value = "/{metaKey}/lastlog", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<EntityLog> getLastLog(@PathVariable("metaKey") String metaKey,
                                                HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/c27ec2e5-2e17-4855-be18-bd8f82249157/lastchange
        EntityLog changed = trackService.getLastEntityLog(company, metaKey);
        if (changed != null)
            return new ResponseEntity<>(changed, HttpStatus.OK);

        return new ResponseEntity<>((EntityLog) null, HttpStatus.NOT_FOUND);

    }


    @RequestMapping(value = "/{metaKey}/lastlog/tags", produces = "application/json", method = RequestMethod.GET)
    public Collection<TrackTag> getLastLogTags(@PathVariable("metaKey") String metaKey,
                                         HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/c27ec2e5-2e17-4855-be18-bd8f82249157/lastchange
//        TrackLog changed = trackService.getLastLog(company, metaKey);
        return trackService.getLastLogTags(company, metaKey);

    }


    @RequestMapping(value = "/{metaKey}/{logId}/tags", produces = "application/json", method = RequestMethod.GET)
    public Collection<TrackTag> getLogTags(@PathVariable("metaKey") String metaKey, @PathVariable("logId") long logId,
                                    HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/c27ec2e5-2e17-4855-be18-bd8f82249157/lastchange
        EntityLog tl = trackService.getEntityLog(company, metaKey, logId);
        return trackService.getLogTags(company, tl);
    }

    @RequestMapping(value = "/{metaKey}/tags", method = RequestMethod.GET)
    public @ResponseBody Collection<TrackTag> getEntityTags(@PathVariable("metaKey") String metaKey,
                                              HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);

        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/{metaKey}
        Entity result = trackService.getEntity(company, metaKey);
        return entityTagService.getEntityTags(company, result);
    }

    @RequestMapping(value = "/{metaKey}/lastlog/attachment",
            produces = "application/pdf",
            method = RequestMethod.GET)
    public @ResponseBody
    byte[] getAttachment(@PathVariable("metaKey") String metaKey,
                         HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        Entity entity = trackService.getEntity(company, metaKey);
        if (entity != null) {
            EntityLog lastLog = logService.getLastLog(entity);
            if (lastLog == null) {
                logger.debug("Unable to find last log for {}", entity);
            } else {
                EntityContent log = kvService.getContent(entity, lastLog.getLog());
                return DatatypeConverter.parseBase64Binary(log.getAttachment());
            }
        }

        throw new NotFoundException("Unable to find the content for the requested metaKey") ;

    }

    @RequestMapping(value = "/{metaKey}/{logId}/delta/{withId}", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<DeltaBean> getDelta(@PathVariable("metaKey") String metaKey, @PathVariable("logId") Long logId, @PathVariable("withId") Long withId,
                                                   HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        Entity entity = trackService.getEntity(company, metaKey);

        if (entity != null) {
            EntityLog left = trackService.getLogForEntity(entity, logId);
            EntityLog right = trackService.getLogForEntity(entity, withId);
            if (left != null && right != null) {
                DeltaBean deltaBean = kvService.getDelta(entity, left.getLog(), right.getLog());

                if (deltaBean != null)
                    return new ResponseEntity<>(deltaBean, HttpStatus.OK);
            }
        }

        return new ResponseEntity<>((DeltaBean) null, HttpStatus.NOT_FOUND);

    }

    @RequestMapping(value = "/{metaKey}/{logId}", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<LogDetailBean> getFullLog(@PathVariable("metaKey") String metaKey, @PathVariable("logId") Long logId,
                                                    HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        LogDetailBean change = trackService.getFullDetail(company, metaKey, logId);

        if (change != null)
            return new ResponseEntity<>(change, HttpStatus.OK);

        return new ResponseEntity<>((LogDetailBean) null, HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = "/{metaKey}/{logId}/what", produces = "application/json", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getLogWhat(@PathVariable("metaKey") String metaKey,
                                          @PathVariable("logId") Long logId,
                                          HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);

        Entity header = trackService.getEntity(company, metaKey);
        if (header != null) {
            EntityLog log = trackService.getLogForEntity(header, logId);
            if (log != null)
                return kvService.getContent(header, log.getLog()).getWhat();
        }

        throw new NotFoundException(String.format("Unable to locate the log for %s / %d", metaKey, logId));

    }

    @RequestMapping(value = "/{metaKey}/lastlog", method = RequestMethod.DELETE)
    public ResponseEntity<String> cancelLastLog(@PathVariable("metaKey") String metaKey,
                                                HttpServletRequest request) throws DatagioException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        Entity result = trackService.getEntity(company, metaKey);
        if (result != null) {
            mediationFacade.cancelLastLog(company, result);
            return new ResponseEntity<>("OK", HttpStatus.OK);
        }

        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);

    }

    @RequestMapping(value = "/tx/{txRef}", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<TxRef> getAuditTx(@PathVariable("txRef") String txRef,
                                            HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        TxRef result;
        result = txService.findTx(txRef);
        return new ResponseEntity<>(result, HttpStatus.OK);

    }


    @RequestMapping(value = "/tx/{txRef}/headers", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getTransactedEntities(@PathVariable("txRef") String txRef,
                                                                     HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        Set<Entity> headers;
        Map<String, Object> result = new HashMap<>(2);
        headers = txService.findTxHeaders(txRef);
        result.put("txRef", txRef);
        result.put("headers", headers);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    @RequestMapping(value = "/tx/{txRef}/logs", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<Map> getEntityTxLogs(@PathVariable("txRef") String txRef,
                                               HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        Map<String, Object> result;
        result = txService.findByTXRef(txRef);
        if (result == null) {
            result = new HashMap<>(1);
            result.put("txRef", "Not a valid transaction identifier");
            return new ResponseEntity<>((Map) result, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<Map>(result, HttpStatus.OK);
    }


    @RequestMapping(value = "/{metaKey}/{xRefName}/xref", produces = "application/json", method = RequestMethod.POST)
    public Collection<String> crossReference(@PathVariable("metaKey") String metaKey, Collection<String> metaKeys, @PathVariable("xRefName") String relationshipName,
                                             HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        return trackService.crossReference(company, metaKey, metaKeys, relationshipName);
    }

    /**
     * Locate cross referenced headers by UID
     *
     * @param metaKey  uid to start from
     * @param xRefName relationship name
     * @return all meta headers of xRefName associated with callerRef
     * @throws DatagioException
     */

    @RequestMapping(value = "/{metaKey}/{xRefName}/xref", produces = "application/json", method = RequestMethod.GET)
    public Map<String, Collection<Entity>> getCrossRefence(@PathVariable("metaKey") String metaKey, @PathVariable("xRefName") String xRefName,
                                                           HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        return trackService.getCrossReference(company, metaKey, xRefName);
    }

    /**
     * Looks across all document types for the caller ref within the fortress. If the callerRef is not unique or does not
     * exist then an exception is thown.
     *
     * @param fortressName application
     * @param callerRef    source
     * @param entities   targets
     * @param xRefName     name of the cross reference
     * @return unresolvable caller references
     * @throws DatagioException if not exactly one Entity for the callerRef in the fortress
     */

    @RequestMapping(value = "/{fortress}/all/{callerRef}/{xRefName}/xref", produces = "application/json", method = RequestMethod.POST)
    public List<EntityKey> crossReferenceEntity(@PathVariable("fortress") String fortressName,
                                                @PathVariable("callerRef") String callerRef,
                                                @RequestBody Collection<EntityKey> entities,
                                                @PathVariable("xRefName") String xRefName,
                                                HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        return trackService.crossReferenceEntities(company, new EntityKey(fortressName, "*", callerRef), entities, xRefName);
    }


    @RequestMapping(value = "/xref", produces = "application/json", method = RequestMethod.POST)
    public List<CrossReferenceInputBean> corssReferenceEntities(@RequestBody List<CrossReferenceInputBean> crossReferenceInputBeans,
                                                                HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);

       return trackService.crossReferenceEntities(company, crossReferenceInputBeans);
        //return crossReferenceInputBeans;
    }


    /**
     * Locate cross referenced headers by Fortress + CallerRef
     *
     * @param fortress     name of the callers application
     * @param callerRef    unique key within the fortress
     * @param xRefName     name of the xReference to lookup
     * @return xRefName and collection of Entities
     * @throws DatagioException if not exactly one CallerRef exists within the fortress
     */
    @RequestMapping(value = "/{fortress}/all/{callerRef}/{xRefName}/xref", produces = "application/json", method = RequestMethod.GET)
    public Map<String, Collection<Entity>> getCrossReference(@PathVariable("fortress") String fortress, @PathVariable("callerRef") String callerRef, @PathVariable("xRefName") String xRefName,
                                                             HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        return trackService.getCrossReference(company, fortress, callerRef, xRefName);
    }


}