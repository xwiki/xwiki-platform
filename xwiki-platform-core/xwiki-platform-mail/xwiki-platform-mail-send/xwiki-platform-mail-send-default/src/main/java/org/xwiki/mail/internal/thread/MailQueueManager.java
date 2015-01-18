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
package org.xwiki.mail.internal.thread;

import org.xwiki.component.annotation.Role;

/**
 * Handles all operations on the Mail Queues.
 *
 * @version $Id$
 * @since 6.4
 * @param <T> the type of the Mail Queue Item managed by the Queue Manager
 */
@Role
public interface MailQueueManager<T extends MailQueueItem>
{
    /**
     * Add a mail on the queue for processing.
     *
     * @param mailQueueItem the object representing the mail item to add to the queue
     */
    void addToQueue(T mailQueueItem);

    /**
     * @return true if the queue has messages waiting for processing
     */
    boolean hasMessage();

    /**
     * @return the next mail on the queue waiting to be processed
     */
    T peekMessage();

    /**
     * Removes the next mail on the queue.
     *
     * @param mailQueueItem the object representing the mail to remove from the queue
     * @return true if the removal was successful, false otherwise
     */
    boolean removeMessageFromQueue(T mailQueueItem);

    /**
     * Wait till all mails from the batch referenced by the passed batch id have been processed.
     *
     * @param batchId the batch id for the batch we're inspecting
     * @param timeout the maximum number of seconds to wait till we consider there's been an error
     */
    void waitTillProcessed(String batchId, long timeout);

    /**
     * @param batchId the batch id for the batch we're inspecting
     * @return true if all mails from the passed batch id have been processed or false otherwise
     */
    boolean isProcessed(String batchId);
}
