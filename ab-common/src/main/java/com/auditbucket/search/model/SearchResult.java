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

package com.auditbucket.search.model;

import com.auditbucket.track.model.SearchChange;

/**
 * Object to tie the keys between ab-engine and ab-search so that ab-engine can keep the document up-to-date
 * <p/>
 * User: Mike Holdsworth
 * Since: 13/07/13
 */
public class SearchResult {
    private String metaKey, fortress, searchKey, documentType;
    private Long logId;
    private Long metaId;

    protected SearchResult() {
    }

    public SearchResult(SearchChange thisChange) {
        this();
        this.metaId = thisChange.getMetaId();
        this.fortress = thisChange.getFortressName();
        this.searchKey = thisChange.getSearchKey();
        this.documentType = thisChange.getDocumentType();
        this.metaKey = thisChange.getMetaKey();

    }

    /**
     * GUID for the metaKey
     *
     * @return string
     */
    public String getMetaKey() {
        return metaKey;
    }

    /**
     * name of the fortress that owns the metaKey
     *
     * @return string
     */
    public String getFortress() {
        return fortress;
    }

    /**
     * GUID for the search document
     *
     * @return string
     */
    public String getSearchKey() {
        return searchKey;
    }

    /**
     * useful for external caller to know what type of document was indexed
     *
     * @return
     */
    public String getDocumentType() {
        return documentType;
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "metaId='" + metaId + '\'' +
                ", metaKey='" + metaKey + '\'' +
                ", logId='" + logId + '\'' +
                ", fortress='" + fortress + '\'' +
                ", documentType='" + documentType + '\'' +
                '}';
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getMetaId() {
        return metaId;
    }

    public void setMetaId(Long metaId) {
        this.metaId = metaId;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }
}
