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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.xwiki.component.phase.Initializable;

/**
 * Handles all operations on the Mail Queues.
 *
 * @param <T> the type of the Mail Queue Item managed by the Queue Manager
 * @version $Id$
 * @since 6.4
 */
public abstract class AbstractMailQueueManager<T extends MailQueueItem> implements MailQueueManager<T>, Initializable
{
    /**
     * The Mail queue that the mail prepare & sender threads will use to send mails. We use separate threads to allow
     * preaparing and sending mail asynchronously.
     */
    protected BlockingQueue<T> mailQueue;

    /**
     * @return the mail queue containing all pending mails to be sent
     */
    private BlockingQueue<T> getMailQueue()
    {
        return this.mailQueue;
    }

    @Override
    public void addToQueue(T mailQueueItem)
    {
        getMailQueue().add(mailQueueItem);
    }

    @Override
    public void addMessage(T mailQueueItem, long timeout, TimeUnit unit) throws InterruptedException
    {
        getMailQueue().offer(mailQueueItem, timeout, unit);
    }

    @Override
    public boolean addMessageToQueue(T mailQueueItem, long timeout, TimeUnit unit) throws InterruptedException
    {
        return getMailQueue().offer(mailQueueItem, timeout, unit);
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
