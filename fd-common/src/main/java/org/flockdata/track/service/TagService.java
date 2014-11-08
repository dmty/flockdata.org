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

package org.flockdata.track.service;

import org.flockdata.registration.model.Tag;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: mike
 * Date: 5/09/14
 * Time: 4:30 PM
 */
public interface TagService {

    Tag createTag(Company company, TagInputBean tagInput);

    public void createTags(Company company, List<TagInputBean> tagInputs) throws FlockException, IOException, ExecutionException, InterruptedException;

    @Async
    public Future<Collection<Tag>> makeTags(Company company, List<TagInputBean> tagInputs) throws ExecutionException, InterruptedException;

    public Tag findTag(Company company, String tagName);

    @Deprecated // Pass the company
    public Tag findTag(String tagName);

    public Collection<Tag> findDirectedTags(Tag startTag);

    public Collection<Tag> findTags(Company company, String label);

    public Tag findTag(Company company, String tagName, String label);

    public Collection<String> getExistingIndexes();

    public void purgeUnusedConcepts(Company company);

    public void purgeType(Company company, String type);
}
