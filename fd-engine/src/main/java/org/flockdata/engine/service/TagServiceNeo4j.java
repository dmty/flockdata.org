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

package org.flockdata.engine.service;

import org.flockdata.engine.repo.neo4j.dao.TagDaoNeo4j;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.service.TagService;
import org.flockdata.helper.Command;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.SecurityHelper;
import org.flockdata.helper.DeadlockRetry;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Handles management of a companies tags.
 * All tags belong to the company across their fortresses
 * <p/>
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 12:53 PM
 */

@Service
@Transactional
public class TagServiceNeo4j implements TagService {
    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    private TagDaoNeo4j tagDao;

    @Autowired
    EngineConfig engineConfig;

    @Autowired
    TagRetryService tagRetryService;


    private Logger logger = LoggerFactory.getLogger(TagServiceNeo4j.class);

    @Override
    public Tag createTag(Company company, TagInputBean tagInput) {
        return tagDao.save(company, tagInput);
    }

    /**
     *
     * @param company   who owns this collection
     * @param tagInputs tags to establish
     * @return tagInputs that failed processing
     */
    @Override
    @Async
    public Future<Collection<Tag>> makeTags(final Company company, final List<TagInputBean> tagInputs) throws ExecutionException, InterruptedException {
        Collection<Tag>failedInput= new ArrayList<>();
        class DLCommand implements Command {
            Collection<Tag> createdTags;
            private final List<TagInputBean> inputs;
            public DLCommand(List<TagInputBean> tagInputBeans) {
                this.inputs = tagInputBeans;
            }

            @Override
            public Command execute() {
                // Creates the relationships
                createdTags = tagDao.save(company, inputs);
                return this;
            }
        }

        List<List<TagInputBean>> splitList = Lists.partition(tagInputs, 5);
        for (List<TagInputBean> tagInputBeans : splitList) {
            DLCommand c = new DLCommand(tagInputBeans);
            try {
                try {
                    DeadlockRetry.execute(c, "creating tags", 15);
                } catch (IOException e) {
                    logger.error("KV Error?", e);
                    throw new FlockException("KV Erro", e);
                }
            } catch (FlockException e) {
                logger.error(" Tag errors detected");
            }
            failedInput.addAll(c.createdTags);
        }
        return new AsyncResult<>(failedInput);
    }

    @Override
    public Tag findTag(Company company, String tagCode) {
        return tagDao.findTag(company, tagCode, Tag.DEFAULT);
    }


    @Override
    public Tag findTag(String tagName) {
        Company company = securityHelper.getCompany();
        if (company == null)
            return null;
        return findTag(company, tagName);
    }

    @Override
    public Collection<Tag> findDirectedTags(Tag startTag) {
        return tagDao.findDirectedTags(startTag, securityHelper.getCompany(), true); // outbound
    }

    @Override
    public Collection<Tag> findTags(Company company, String label) {
        return tagDao.findTags(company, label);
    }

    @Override
    public Tag findTag(Company company, String label, String tagCode) {
        return tagDao.findTag(company, tagCode, label);
    }

    @Override
    public Collection<String> getExistingIndexes() {
        return tagDao.getExistingLabels();
    }

    @Override
    public void createTags(Company company, List<TagInputBean> tagInputs) throws FlockException, IOException, ExecutionException, InterruptedException {
        tagRetryService.track(company, tagInputs);
    }

    @Override
    public void purgeUnusedConcepts(Company company){
        tagDao.purgeUnusedConcepts(company);
    }

    @Override
    public void purgeLabel(Company company, String label) {
        tagDao.purge(company, label);
    }

    @Override
    public void createAlias(Company company, Tag tag, String label, String aliasKeyValue) {
        tagDao.createAlias(company, tag, label, aliasKeyValue);
    }
}
