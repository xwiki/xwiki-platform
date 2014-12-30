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

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.mail.MailListener;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Represents a Mail messages placed on the queue for sending.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class MailSenderQueueItem
{
    private Iterable<? extends MimeMessage> messages;

    private Session session;

    private MailListener listener;

    private long threadId;

    private UUID batchID;

    /**
     * @param messages see {@link #getMessages()}
     * @param session see {@link #getSession()}
     * @param listener see {@link #getListener()}
     * @param batchID see {@link #getBatchID()}
     */
    public MailSenderQueueItem(Iterable<? extends MimeMessage> messages, Session session, MailListener listener,
        UUID batchID)
    {
        this.messages = messages;
        this.session = session;
        this.listener = listener;
        this.batchID = batchID;
        this.threadId = Thread.currentThread().getId();
    }

    /**
     * @return the list of mail messages to be sent
     */
    public Iterable<? extends MimeMessage> getMessages()
    {
        return this.messages;
    }

    /**
     * @return the JavaMail Session to be used when sending
     */
    public Session getSession()
    {
        return this.session;
    }

    /**
     * @return an optional listener to call when the mail is sent successfully or when there's an error
     */
    public MailListener getListener()
    {
        return this.listener;
    }

    /**
     * @return the id of the thread that wants to send this email
     */
    public long getThreadId()
    {
        return this.threadId;
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);
        builder.append("batchID", this.batchID);
        builder.append("threadId", getThreadId());
        return builder.toString();
    }

    /**
     * @return the UUID of the batch
     */
    public UUID getBatchID()
    {
        return this.batchID;
    }
}
