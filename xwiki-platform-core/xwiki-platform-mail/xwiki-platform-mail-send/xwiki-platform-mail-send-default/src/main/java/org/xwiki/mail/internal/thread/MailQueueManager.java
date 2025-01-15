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

import java.util.concurrent.TimeUnit;

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
     * Add a mail on the queue for processing, waiting a max of timeout.
     *
     * @param mailQueueItem the object representing the mail item to add to the queue
     * @param timeout how long to wait before giving up, in units of {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the {@code timeout} parameter
     * @throws InterruptedException if it times out i.e if the queue doesn't have any empty slot freed in the specified
     *         timeout time
     * @since 11.6RC1
     * @deprecated use {@link #addMessageToQueue(MailQueueItem, long, TimeUnit)} instead
     */
    @Deprecated(since = "15.0RC1")
    default void addMessage(T mailQueueItem, long timeout, TimeUnit unit) throws InterruptedException
    {
        addToQueue(mailQueueItem);
    }

    /**
     * Add a mail on the queue for processing, waiting a max of timeout.
     *
     * @param mailQueueItem the object representing the mail item to add to the queue
     * @param timeout how long to wait before giving up, in units of {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the {@code timeout} parameter
     * @return true if the message was added to the queue successfully or false if it wasn't and the timeout occurred
     * @throws InterruptedException if it times out i.e if the queue doesn't have any empty slot freed in the specified
     *         timeout time
     * @since 15.0RC1
     */
    default boolean addMessageToQueue(T mailQueueItem, long timeout, TimeUnit unit) throws InterruptedException
    {
        addToQueue(mailQueueItem);
        return true;
    }

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
}
