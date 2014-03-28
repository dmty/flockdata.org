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

package com.auditbucket.engine.repo.riak;

import com.auditbucket.audit.model.MetaHeader;
import com.auditbucket.engine.repo.KvRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.keyvalue.riak.core.RiakTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;

@Component
public class RiakRepo implements KvRepo {

    @Autowired
    private RiakTemplate riak;

    //private Logger logger = LoggerFactory.getLogger(RiakRepo.class);

    public void add(MetaHeader metaHeader, Long key, byte[] value) throws IOException {
        riak.setAsBytes(metaHeader.getIndexName(), key, value);
    }

    public byte[] getValue(MetaHeader metaHeader, Long key) {
        return riak.getAsBytes(metaHeader.getIndexName(), key);
    }

    public void delete(MetaHeader metaHeader, Long key) {
        riak.delete(metaHeader.getIndexName(), key);
    }

    @Override
    public String ping() {
        riak.setIfKeyNonExistent("ab.ping", "ping", new Date());
        riak.delete("ab.ping", "ping");
        return "Riak is OK";

    }
}
