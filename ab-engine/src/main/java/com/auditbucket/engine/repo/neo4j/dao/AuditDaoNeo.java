/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

package com.auditbucket.engine.repo.neo4j.dao;

import com.auditbucket.audit.model.*;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.bean.AuditTXResult;
import com.auditbucket.dao.IAuditDao;
import com.auditbucket.engine.repo.neo4j.AuditHeaderRepo;
import com.auditbucket.engine.repo.neo4j.AuditLogRepo;
import com.auditbucket.engine.repo.neo4j.model.AuditChangeNode;
import com.auditbucket.engine.repo.neo4j.model.AuditHeaderNode;
import com.auditbucket.engine.repo.neo4j.model.AuditLogRelationship;
import com.auditbucket.engine.repo.neo4j.model.TxRefNode;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.FortressUser;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * User: Mike Holdsworth
 * Date: 21/04/13
 * Time: 8:00 PM
 */
@Repository("auditDAO")
public class AuditDaoNeo implements IAuditDao {
    @Autowired
    AuditHeaderRepo auditRepo;

    @Autowired
    AuditLogRepo auditLogRepo;

    @Autowired
    Neo4jTemplate template;
    private Logger log = LoggerFactory.getLogger(AuditDaoNeo.class);

    @Override
    public AuditHeader save(AuditHeader auditHeader) {
        auditHeader.bumpUpdate();
        return auditRepo.save((AuditHeaderNode) auditHeader);
    }

    public TxRef save(TxRef tagRef) {
        return template.save((TxRefNode) tagRef);
    }

    public AuditHeader findHeader(String key) {
        return findHeader(key, false);
    }

    @Override
    public AuditHeader findHeader(String key, boolean inflate) {
        AuditHeader header = auditRepo.findByUID(key);
        if (inflate) {
            fetch(header);
        }
        return header;
    }

    public AuditHeader findHeaderByCallerRef(Long fortressId, @NotNull String documentType, @NotNull String callerRef) {
        if (log.isDebugEnabled())
            log.debug("findByCallerRef fortress [" + fortressId + "] docType[" + documentType + "], callerRef[" + callerRef.toLowerCase() + "]");
        // This is pretty crappy, but Neo4J will throw an exception the first time you try to search if no index is in place.
        if (template.getGraphDatabaseService().index().existsForNodes("callerRef"))
            return auditRepo.findByCallerRef(fortressId, documentType, callerRef.toLowerCase());

        return null;
    }

    @Override
    public void removeLastChange(AuditHeader header) {
        // Remove the lastChange relationship
        template.deleteRelationshipBetween(header, header.getLastUser(), "lastChanged");
    }

    public AuditHeader fetch(AuditHeader header) {
        template.fetch(header);
        template.fetch(header.getFortress());
        template.fetch(header.getTagValues());

        return header;
    }

    @Override
    public TxRef findTxTag(@NotEmpty String userTag, @NotNull Company company, boolean fetchHeaders) {
        TxRef txRef = auditRepo.findTxTag(userTag, company.getId());
        return txRef;
    }


    @Override
    public TxRef beginTransaction(String id, Company company) {

        TxRef tag = findTxTag(id, company, false);
        if (tag == null) {
            tag = new TxRefNode(id, company);
            template.save(tag);
        }
        return tag;
    }

    @Override
    public int getLogCount(Long id) {
        return auditLogRepo.getLogCount(id);
    }

    public AuditLog getLastChange(Long auditHeaderID) {
        AuditLog when = auditLogRepo.getLastChange(auditHeaderID);
        if (when != null)
            template.fetch(when.getAuditChange());
        return when;
    }

    public AuditLog getChange(Long auditHeaderID, long sysWhen) {
        return auditLogRepo.getChange(auditHeaderID, sysWhen);
    }


    public Set<AuditLog> getAuditLogs(Long auditLogID, Date from, Date to) {
        return auditLogRepo.getAuditLogs(auditLogID, from.getTime(), to.getTime());
    }

    public Set<AuditLog> getAuditLogs(Long auditHeaderID) {
        return auditLogRepo.findAuditLogs(auditHeaderID);
    }

    @Override
    public void delete(AuditChange auditLog) {
        auditLogRepo.delete((AuditChangeNode) auditLog);
    }

    @Override
    public void delete(AuditHeader auditHeader) {
        //ToDo: Remove all the logs
        auditRepo.delete((AuditHeaderNode) auditHeader);
    }

    public Map<String, Object> findByTransaction(TxRef txRef) {
        //Example showing how to use cypher and extract

        String findByTagRef = "start tag =node({txRef}) " +
                "              match tag-[:txIncludes]->auditLog<-[logs:logged]-audit " +
                "             return logs, audit, auditLog " +
                "           order by logs.sysWhen";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("txRef", txRef.getId());

        Iterator<Map<String, Object>> rows;
        Result<Map<String, Object>> exResult = template.query(findByTagRef, params);

        rows = exResult.iterator();

        List<AuditTXResult> simpleResult = new ArrayList<AuditTXResult>();
        int i = 1;
        //Result<Map<String, Object>> results =
        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            AuditLog log = template.convert(row.get("logs"), AuditLogRelationship.class);
            AuditChange change = template.convert(row.get("auditLog"), AuditChangeNode.class);
            AuditHeader audit = template.convert(row.get("audit"), AuditHeaderNode.class);
            simpleResult.add(new AuditTXResult(audit, change, log));
            i++;

        }
        Map<String, Object> result = new HashMap<String, Object>(i);
        result.put("txRef", txRef.getName());
        result.put("logs", simpleResult);

        return result;
    }

    @Override
    public AuditLog addLog(AuditHeader header, AuditChange al, DateTime fortressWhen) {
        AuditLogRelationship aWhen = new AuditLogRelationship(header, al, fortressWhen);
        return template.save(aWhen);

    }

    public void save(AuditLog log) {
        template.save((AuditLogRelationship) log);
    }

    @Override
    public String ping() {
        Map<String, Object> ab = new HashMap<String, Object>();
        ab.put("name", "AuditBucket");
        Node abNode = template.getGraphDatabase().getOrCreateNode("system", "name", "AuditBucket", ab);
        if (abNode == null) {
            return "Neo4J has problems";
        }
        return "Neo4J is OK";
    }

    @Override
    public AuditChange save(FortressUser fUser, AuditLogInputBean input) {
        return save(fUser, input, null);
    }

    @Override
    public AuditChange save(FortressUser fUser, AuditLogInputBean input, TxRef txRef) {
        AuditChange auditChange = new AuditChangeNode(fUser, input, txRef);
        return template.save(auditChange);
    }

    @Override
    public AuditHeader save(FortressUser fu, AuditHeaderInputBean inputBean, DocumentType documentType) {
        AuditHeader ah = new AuditHeaderNode(fu, inputBean, documentType);
        return save(ah);
    }
}
