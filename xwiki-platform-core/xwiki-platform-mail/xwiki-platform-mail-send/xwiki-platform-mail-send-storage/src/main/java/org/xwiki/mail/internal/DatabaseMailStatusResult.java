/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.mail.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStoreException;

/**
 * This implementation is not meant for scalability. Don't use it if you're sending a large number of emails. Instead
 * use the Query Manager to perform database queries.
 *
 * @version $Id$
 * @since 6.4M3
 */
public class DatabaseMailStatusResult extends AbstractMailStatusResult
{
    private static final String BATCHID_KEY = "batchId";

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMailStatusResult.class);

    private static final String DATE_FIELD = "date";

    private MailStatusStore mailStatusStore;

    private String batchId;

    /**
     * Constructor initializing the DatabaseMailStatusResult with MailStatusStore.
     * @param mailStatusStore the MailStatusStore
     */
    public DatabaseMailStatusResult(MailStatusStore mailStatusStore)
    {
        this.mailStatusStore = mailStatusStore;
    }

    /**
     * Set the batch id of the message statuses to save or load.
     *
     * @param batchId the batch id of the message statuses
     */
    public void setBatchId(String batchId)
    {
        this.batchId = batchId;
    }

    @Override
    public Iterator<MailStatus> getAll()
    {
        if (this.batchId == null) {
            return Collections.emptyIterator();
        }

        try {
            return this.mailStatusStore.load(Collections.<String, Object>singletonMap(BATCHID_KEY, this.batchId),
                0, 0, DATE_FIELD, true).iterator();
        } catch (MailStoreException e) {
            LOGGER.error("Failed to get all results. Returning an empty result.", e);
            return Collections.emptyIterator();
        }
    }

    @Override
    public Iterator<MailStatus> getAllErrors()
    {
        return getFilteredState("%_error");
    }

    @Override
    public Iterator<MailStatus> getByState(MailState state)
    {
        return getFilteredState(state.toString());
    }

    private Iterator<MailStatus> getFilteredState(String state)
    {
        if (this.batchId == null) {
            return Collections.emptyIterator();
        }

        try {
            Map<String, Object> filterMap = new HashMap<>();
            filterMap.put(BATCHID_KEY, this.batchId);
            filterMap.put("state", state);
            return this.mailStatusStore.load(filterMap, 0, 0, DATE_FIELD, true).iterator();
        } catch (MailStoreException e) {
            LOGGER.error("Failed to get results by state. Returning an empty result.", e);
            return Collections.emptyIterator();
        }
    }
}
