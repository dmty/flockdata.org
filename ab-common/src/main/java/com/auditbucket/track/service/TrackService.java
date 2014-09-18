package com.auditbucket.track.service;

import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.search.model.EntitySearchChange;
import com.auditbucket.search.model.SearchResult;
import com.auditbucket.track.bean.*;
import com.auditbucket.track.model.*;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * User: mike
 * Date: 5/09/14
 * Time: 4:22 PM
 */
public interface TrackService {
    EntityContent getWhat(Entity entity, Log change);

    @Deprecated
    Entity getEntity(@NotEmpty String metaKey);

    Entity getEntity(Company company, String metaKey);

    Entity getEntity(Company company, @NotEmpty String metaKey, boolean inflate);

    Entity getEntity(Entity entity);

    Collection<Entity> getEntities(Fortress fortress, Long skipTo);

    Collection<Entity> getEntities(Fortress fortress, String docTypeName, Long skipTo);

    void updateEntity(Entity entity);

    EntityLog getLastEntityLog(Company company, String metaKey) throws FlockException;

    @Deprecated
    EntityLog getLastEntityLog(String metaKey) throws FlockException;

    EntityLog getLastEntityLog(Long entityId);

    Set<EntityLog> getEntityLogs(Long entityId);

    Set<EntityLog> getEntityLogs(Company company, String metaKey) throws FlockException;

    Set<EntityLog> getEntityLogs(String metaKey, Date from, Date to) throws FlockException;

    EntitySearchChange cancelLastLog(Company company, Entity entity) throws IOException, FlockException;

    int getLogCount(Company company, String metaKey) throws FlockException;

    Entity findByCallerRef(String fortress, String documentType, String callerRef);

    Entity findByCallerRefFull(Long fortressId, String documentType, String callerRef);

    Entity findByCallerRefFull(Fortress fortress, String documentType, String callerRef);

    Iterable<Entity> findByCallerRef(Company company, String fortressName, String callerRef);

    Collection<Entity> findByCallerRef(Fortress fortress, String callerRef);

    Entity findByCallerRef(Fortress fortress, String documentType, String callerRef);

    Entity findByCallerRef(Fortress fortress, DocumentType documentType, String callerRef);

    EntitySummaryBean getEntitySummary(Company company, String metaKey) throws FlockException;

    LogDetailBean getFullDetail(String metaKey, Long logId);

    LogDetailBean getFullDetail(Company company, String metaKey, Long logId);

    EntityLog getLogForEntity(Entity entity, Long logId);

    Iterable<TrackResultBean> trackEntities(Fortress fortress, Iterable<EntityInputBean> inputBeans) throws InterruptedException, ExecutionException, FlockException, IOException;

    Collection<String> crossReference(Company company, String metaKey, Collection<String> xRef, String relationshipName) throws FlockException;

    Map<String, Collection<Entity>> getCrossReference(Company company, String metaKey, String xRefName) throws FlockException;

    Map<String, Collection<Entity>> getCrossReference(Company company, String fortressName, String callerRef, String xRefName) throws FlockException;

    List<EntityKey> crossReferenceEntities(Company company, EntityKey sourceKey, Collection<EntityKey> targetKeys, String xRefName) throws FlockException;

    Map<String, Entity> getEntities(Company company, Collection<String> metaKeys);

    void purge(Fortress fortress);

    void recordSearchResult(SearchResult searchResult, Long metaId);

    Collection<TrackTag> getLastLogTags(Company company, String metaKey) throws FlockException;

    EntityLog getEntityLog(Company company, String metaKey, long logId) throws FlockException;

    Collection<TrackTag> getLogTags(Company company, EntityLog tl);


    List<CrossReferenceInputBean> crossReferenceEntities(Company company, List<CrossReferenceInputBean> crossReferenceInputBeans);
}
