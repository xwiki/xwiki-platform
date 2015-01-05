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

import java.util.Iterator;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles all operations on the Mail Sending Queue.
 *
 * @version $Id$
 * @since 6.4M3
 */
public class MailQueueManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MailQueueManager.class);

    /**
     * The Mail queue that the mail sender thread will use to send mails. We use a separate thread to allow sending
     * mail asynchronously.
     */
    private Queue<MailSenderQueueItem> mailQueue = new ConcurrentLinkedQueue<>();

    /**
     * @return the mail queue containing all pending mails to be sent
     */
    private Queue<MailSenderQueueItem> getMailQueue()
    {
        return this.mailQueue;
    }

    /**
     * Add a mail on the queue for processing.
     *
     * @param mailSenderQueueItem the object representing the mail to add to the queue
     */
    public void addToQueue(MailSenderQueueItem mailSenderQueueItem)
    {
        this.mailQueue.add(mailSenderQueueItem);
    }

    /**
     * @return true if the queue has mails waiting for processing
     */
    public boolean hasMessageToSend()
    {
        return !this.mailQueue.isEmpty();
    }

    /**
     * @return the next mail on the queue waiting to be processed
     */
    public MailSenderQueueItem peekMessage()
    {
        return this.mailQueue.peek();
    }

    /**
     * Removes the next mail on the queue.
     *
     * @param mailSenderQueueItem the object representing the mail to remove from the queue
     * @return true if the removal was successful, false otherwise
     */
    public boolean removeMessageFromQueue(MailSenderQueueItem mailSenderQueueItem)
    {
        return this.mailQueue.remove(mailSenderQueueItem);
    }

    /**
     * Wait till all mails from the batch referenced by the passed batch id have been processed.
     *
     * @param batchId the batch id for the batch we're inspecting
     * @param timeout the maximum number of seconds to wait till we consider there's been an error
     */
    public void waitTillSent(UUID batchId, long timeout)
    {
        long startTime = System.currentTimeMillis();
        while (!isSent(batchId) && System.currentTimeMillis() - startTime < timeout) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                // Ignore but consider that the mail was sent
                LOGGER.warn("Interrupted while waiting for mails to be sent. Reason [{}]",
                    ExceptionUtils.getRootCauseMessage(e));
                break;
            }
        }
    }

    /**
     * @param batchId the batch id for the batch we're inspecting
     * @return true if all mails from the passed batch id have been processed or false otherwise
     */
    public boolean isSent(UUID batchId)
    {
        Iterator<MailSenderQueueItem> iterator = getMailQueue().iterator();
        while (iterator.hasNext()) {
            MailSenderQueueItem item = iterator.next();
            if (batchId == item.getBatchId()) {
                return false;
            }
        }
        return true;
    }
}
