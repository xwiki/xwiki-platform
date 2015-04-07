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

import org.xwiki.mail.MailListener;

/**
 * Represents a Mail Message placed on the queue for sending.
 *
 * @version $Id$
 * @since 6.4
 */
public class SendMailQueueItem extends AbstractMailQueueItem
{
    private String messageId;

    /**
     * @param messageId see {@link #getMessageId()}
     * @param session see {@link #getSession()}
     * @param listener see {@link #getListener()}
     * @param batchId see {@link #getBatchId()}
     * @param wikiId see {@link #getWikiId()}
     */
    public SendMailQueueItem(String messageId, Session session, MailListener listener, String batchId, String wikiId)
    {
        super(session, listener, batchId, wikiId);
        this.messageId = messageId;
    }

    /**
     * @return the unique id of the MimeMessage to send
     */
    public String getMessageId()
    {
        return this.messageId;
    }

    @Override
    public String toString()
    {
        return prepareToString().append("messageId", getMessageId()).toString();
    }
}
