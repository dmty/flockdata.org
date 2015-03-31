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

package org.flockdata.engine.schema.model;

import org.flockdata.company.model.FortressNode;
import org.flockdata.registration.model.Fortress;
import org.flockdata.track.model.DocumentType;
import org.flockdata.track.model.Profile;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 4:30 PM
 */
@NodeEntity(useShortNames = true)
@TypeAlias("Profile")
public class ProfileNode implements Profile {

    @GraphId
    private Long id;

    @Indexed(unique = true)
    private String profileKey;

    @RelatedTo(type = "FORTRESS_PROFILE")
    private FortressNode fortress;

    @RelatedTo( type = "DOCUMENT_PROFILE")
    private DocumentTypeNode document;

    private String content;

    ProfileNode() {}

    public ProfileNode(Fortress fortress, DocumentType documentType) {
        this();
        this.fortress = (FortressNode) fortress;
        this.document = (DocumentTypeNode) documentType;
        this.profileKey = parseKey(fortress, documentType);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getProfileKey() {
        return profileKey;
    }

    public static String parseKey(Fortress fortress, DocumentType documentType) {
        return fortress.getId() +"-"+documentType.getId();
    }

    public Long getId() {
        return id;
    }


}
