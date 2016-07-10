/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.model;

import java.util.Map;

/**
 * Created by mike on 22/08/15.
 */
public class SubTag extends AbstractEntityTag {
    Long id ;
    Tag tag;
    String relationship;

    public SubTag() {}

    public SubTag(Tag subTag, String label) {
        this.tag = subTag;
        this.relationship = label;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Entity getEntity() {
        return null;
    }

    @Override
    public Tag getTag() {
        return tag;
    }

    @Override
    public String getRelationship() {
        return relationship;
    }

    @Override
    public boolean isGeo() {
        return false;
    }

    @Override
    public Map<String, Object> getTagProperties() {
        return null;
    }

    @Override
    public Boolean isReversed() {
        return false;
    }

    @Override
    public void setRelationship(String name) {
        this.relationship = name;
    }
}
