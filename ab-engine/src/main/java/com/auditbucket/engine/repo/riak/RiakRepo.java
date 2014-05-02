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

import com.auditbucket.engine.repo.KvRepo;
import com.auditbucket.track.model.MetaHeader;
import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakFactory;
import com.basho.riak.client.bucket.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RiakRepo implements KvRepo {

    private static Logger logger = LoggerFactory.getLogger(RiakRepo.class);
    private IRiakClient client = null;
    private final Object synLock = "RiakRepoLock";

    private IRiakClient getClient()  {
        if ( client == null ){
            synchronized (synLock){
                if ( client == null )
                    try {
                        // ToDo: set server and host
                        client = RiakFactory.pbcClient();
                        client.generateAndSetClientId();
                    } catch (RiakException e) {
                        logger.error("Unable to create Riak Client", e);
                    }
            }

        }
        return client;
    }
    public void add(MetaHeader metaHeader, Long key, byte[] value) throws IOException {
        //riak.put(metaHeader.getIndexName(), key);
        try {
            Bucket bucket = getClient().createBucket(metaHeader.getIndexName()).execute();
            bucket.store(String.valueOf(key), value).execute();
        } catch (RiakException e) {
            logger.error("KV Error", e);
            throw new IOException ("KV Error",e);
        }
    }

    public byte[] getValue(MetaHeader metaHeader, Long key) {
        try {
            Bucket bucket = getClient().createBucket(metaHeader.getIndexName()).execute();
            IRiakObject result = bucket.fetch(String.valueOf(key)).execute();
            if (result!=null )
                return result.getValue();
        } catch (RiakException e) {
            logger.error("KV Error", e);
            return null;
        }
        return null;
    }

    public void delete(MetaHeader metaHeader, Long key) {
        try {
            Bucket bucket = getClient().fetchBucket(metaHeader.getIndexName()).execute();
            bucket.delete(String.valueOf(key)).execute();
        } catch (RiakException e) {
            logger.error("KV Error", e);
        }

    }

    @Override
    public String ping() {
        try {
            getClient().ping();
        } catch (RiakException e) {
            return "Error pinging RIAK";
        }
        return "Riak is OK";

    }
}