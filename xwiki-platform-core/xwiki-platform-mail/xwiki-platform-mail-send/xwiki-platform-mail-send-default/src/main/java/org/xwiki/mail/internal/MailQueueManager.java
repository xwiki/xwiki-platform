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

import java.util.UUID;

import org.xwiki.component.annotation.Role;

/**
 * Handles all operations on the Mail Sending Queue.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Role
public interface MailQueueManager
{
    /**
     * Add a mail on the queue for processing.
     *
     * @param mailSenderQueueItem the object representing the mail to add to the queue
     */
    void addToQueue(MailSenderQueueItem mailSenderQueueItem);

    /**
     * @return true if the queue has mails waiting for processing
     */
    boolean hasMessageToSend();

    /**
     * @return the next mail on the queue waiting to be processed
     */
    MailSenderQueueItem peekMessage();

    /**
     * Removes the next mail on the queue.
     *
     * @param mailSenderQueueItem the object representing the mail to remove from the queue
     * @return true if the removal was successful, false otherwise
     */
    boolean removeMessageFromQueue(MailSenderQueueItem mailSenderQueueItem);

    /**
     * Wait till all mails from the batch referenced by the passed batch id have been processed.
     *
     * @param batchId the batch id for the batch we're inspecting
     * @param timeout the maximum number of seconds to wait till we consider there's been an error
     */
    void waitTillSent(UUID batchId, long timeout);

    /**
     * @param batchId the batch id for the batch we're inspecting
     * @return true if all mails from the passed batch id have been processed or false otherwise
     */
    boolean isProcessed(UUID batchId);
}
