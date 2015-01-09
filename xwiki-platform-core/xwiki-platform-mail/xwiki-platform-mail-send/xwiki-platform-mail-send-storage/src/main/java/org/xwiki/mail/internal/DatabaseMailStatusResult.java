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
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStoreException;

/**
 * This implementation is not meant for scalability. Don't use it if you're sending a large number of emails. Instead
 * use the Query Manager to perform database queries.
 *
 * @version $Id$
 * @since 6.4M3
 */
public class DatabaseMailStatusResult implements MailStatusResult
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMailStatusResult.class);

    private MailStatusStore mailStatusStore;

    private String batchId;

    private Map<String, Object> parameters;

    /**
     * Constructor initializing the DatabaseMailStatusResult with MailStatusStore.
     * @param mailStatusStore the MailStatusStore
     */
    public DatabaseMailStatusResult(MailStatusStore mailStatusStore)
    {
        this.mailStatusStore = mailStatusStore;
    }

    /**
     * Set the batch id of the message statuses to save or load along with context parameters (such as the wiki id of
     * the wiki from which the batch is being sent from).
     *
     * @param batchId the batch id of the message statuses
     * @param parameters some parameters specifying addition context data (for example the current wiki is stored under
     *        the {@code wiki} key)
     */
    public void setContextData(String batchId, Map<String, Object> parameters)
    {
        this.batchId = batchId;
        this.parameters = parameters;
    }

    @Override
    public long getSize()
    {
        if (this.batchId == null || this.parameters == null) {
            return 0;
        }

        try {
            return this.mailStatusStore.count(this.batchId, this.parameters);
        } catch (MailStoreException e) {
            LOGGER.error("Failed to get size of results for batch id [{}]. Returning an empty result.",
                this.batchId, e);
            return 0;
        }
    }

    @Override
    public Iterator<MailStatus> getAll()
    {
        if (this.batchId == null || this.parameters == null) {
            return Collections.emptyIterator();
        }

        try {
            return this.mailStatusStore.loadFromBatchId(this.batchId, this.parameters).iterator();
        } catch (MailStoreException e) {
            LOGGER.error("Failed to get all results. Returning an empty result.", e);
            return Collections.emptyIterator();
        }
    }

    @Override
    public Iterator<MailStatus> getByState(MailState state)
    {
        if (this.batchId == null || this.parameters == null) {
            return Collections.emptyIterator();
        }

        try {
            return this.mailStatusStore.loadFromBatchId(this.batchId, state, parameters).iterator();
        } catch (MailStoreException e) {
            LOGGER.error("Failed to get results by state. Returning an empty result.", e);
            return Collections.emptyIterator();
        }
    }
}
