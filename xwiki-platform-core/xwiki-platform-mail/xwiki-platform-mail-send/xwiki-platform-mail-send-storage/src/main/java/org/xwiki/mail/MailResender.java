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
package org.xwiki.mail;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Role;

/**
 * Resends mails.
 *
 * @version $Id$
 * @since 9.3RC1
 */
@Role
public interface MailResender
{
    /**
     * Resends all mails matching the passed filter map, asynchronously.
     *
     * @param filterMap the map of Mail Status parameters to match (e.g. "state", "wiki", "batchId", etc)
     * @param offset the number of rows to skip (0 means don't skip any row)
     * @param count the number of rows to return. If 0 then all rows are returned
     * @return the mail statuses and results for the resent mails
     * @throws MailStoreException if a mail status failed to be loaded. Note that no exception is raised if a mail
     *         message failed to be loaded from the store, in which case no entry will be returned in the returned list
     */
    List<Pair<MailStatus, MailStatusResult>> resendAsynchronously(Map<String, Object> filterMap, int offset, int count)
        throws MailStoreException;

    /**
     * Resends all mails matching the passed filter map, synchronously (one mail after another).
     *
     * @param filterMap the map of Mail Status parameters to match (e.g. "state", "wiki", "batchId", etc)
     * @param offset the number of rows to skip (0 means don't skip any row)
     * @param count the number of rows to return. If 0 then all rows are returned
     * @return the mail statuses and results for the resent mails
     * @throws MailStoreException if a mail status failed to be loaded. Note that no exception is raised if a mail
     *         message failed to be loaded from the store, in which case no entry will be returned in the returned list
     * @since 12.10
     */
    default List<Pair<MailStatus, MailStatusResult>> resend(Map<String, Object> filterMap, int offset, int count)
        throws MailStoreException
    {
        // Not supported by default
        throw new MailStoreException("Synchronous mail resending is not supported");
    }

    /**
     * Resends the mail message matching the passed batch id and message id, asynchronously.
     *
     * @param batchId the id of the batch to which the message to resend belongs to
     * @param uniqueMessageId the unique id of the message to resend
     * @return the status of the mail resend
     * @throws MailStoreException if the corresponding mail message to be resent couldn't be retrieved from the store
     */
    MailStatusResult resendAsynchronously(String batchId, String uniqueMessageId) throws MailStoreException;
}
