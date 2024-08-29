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

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.MailListener;

/**
 * Represents one or several Mail messages placed on the queue for preparation. Preparation means that the MimeMessage
 * objects will be created by the Thread reading from this queue and then placed on the Send Thread queue.
 *
 * @version $Id$
 * @since 6.4
 */
public class PrepareMailQueueItem extends AbstractMailQueueItem
{
    private Iterable<? extends MimeMessage> messages;

    private ExecutionContext executionContext;

    /**
     * @param messages see {@link #getMessages()}
     * @param session see {@link #getSession()}
     * @param listener see {@link #getListener()}
     * @param batchId see {@link #getBatchId()}
     * @param executionContext see {@link #getContext()}
     */
    public PrepareMailQueueItem(Iterable<? extends MimeMessage> messages, Session session, MailListener listener,
        String batchId, ExecutionContext executionContext)
    {
        super(session, listener, batchId);
        this.messages = messages;
        this.executionContext = executionContext;
    }

    /**
     * @return the list of mail messages to be sent
     */
    public Iterable<? extends MimeMessage> getMessages()
    {
        return this.messages;
    }

    /**
     * @return the execution context that will be used when preparing and sending the Mime Message
     * @since 7.1M2
     */
    public ExecutionContext getContext()
    {
        return this.executionContext;
    }

    /**
     * @since 7.1M2
     */
    @Override
    public String toString()
    {
        return prepareToString().append("context",
            this.executionContext == null ? null : this.executionContext.getProperties()).toString();
    }
}
