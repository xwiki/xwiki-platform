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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles all operations on the Mail Queues.
 *
 * @version $Id$
 * @since 6.4
 * @param <T> the type of the Mail Queue Item managed by the Queue Manager
 */
public abstract class AbstractMailQueueManager<T extends MailQueueItem> implements MailQueueManager<T>
{
    /**
     * The Mail queue that the mail sender thread will use to send mails. We use a separate thread to allow sending
     * mail asynchronously.
     */
    private Queue<T> mailQueue = new ConcurrentLinkedQueue<>();

    /**
     * @return the mail queue containing all pending mails to be sent
     */
    private Queue<T> getMailQueue()
    {
        return this.mailQueue;
    }

    @Override
    public void addToQueue(T mailQueueItem)
    {
        getMailQueue().add(mailQueueItem);
    }

    @Override
    public boolean hasMessage()
    {
        return !getMailQueue().isEmpty();
    }

    @Override
    public T peekMessage()
    {
        return getMailQueue().peek();
    }

    @Override
    public boolean removeMessageFromQueue(T mailQueueItem)
    {
        return getMailQueue().remove(mailQueueItem);
    }
}
