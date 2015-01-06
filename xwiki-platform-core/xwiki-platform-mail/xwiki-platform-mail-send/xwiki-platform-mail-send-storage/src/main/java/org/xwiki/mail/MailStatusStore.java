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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Save, load and search mail results.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Role
@Unstable
public interface MailStatusStore
{
    /**
     * Saves mail status in the store.
     *
     * @param status the mail status to be saved
     * @throws MailStoreException when an error occurs saving the data
     */
    void save(MailStatus status) throws MailStoreException;

    /**
     * Load a message status from the store.
     *
     * @param messageId the id of the message to load
     * @return the loaded {@link org.xwiki.mail.MailStatus} instance
     * @throws MailStoreException when an error occurs when loading the data
     */
    MailStatus loadFromMessageId(String messageId) throws MailStoreException;

    /**
     * @param batchId the batch id of the message statuses to load
     * @return the number of emails matching the passed batch id from the store.
     * @throws MailStoreException when an error occurs when loading the data
     */
    long count(String batchId) throws MailStoreException;

    /**
     * Loads all message statuses matching the passed state and batch id from the store.
     *
     * @param batchId the batch id of the message statuses to load
     * @param state the state to match (only statuses having that state will be loaded)
     * @return the loaded {@link org.xwiki.mail.MailStatus} instance
     * @throws MailStoreException when an error occurs when loading the data
     */
    List<MailStatus> loadFromBatchId(String batchId, MailState state) throws MailStoreException;

    /**
     * Loads all message statuses matching the passed batch id from the store.
     *
     * @param batchId the batch id of the message statuses to load
     * @return the loaded {@link org.xwiki.mail.MailStatus} instance
     * @throws MailStoreException when an error occurs when loading the data
     */
    List<MailStatus> loadFromBatchId(String batchId) throws MailStoreException;
}
