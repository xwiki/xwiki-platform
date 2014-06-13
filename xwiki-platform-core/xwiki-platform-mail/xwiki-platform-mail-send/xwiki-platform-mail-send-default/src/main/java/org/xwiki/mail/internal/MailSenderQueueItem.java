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

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.xwiki.mail.MailResultListener;

/**
 * Represents a Mail message placed on the queue for sending.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class MailSenderQueueItem
{
    private MimeMessage message;

    private Session session;

    private MailResultListener listener;

    /**
     * @param message see {@link #getMessage()}
     * @param session see {@link #getSession()}
     * @param listener see {@link #getListener()}
     */
    public MailSenderQueueItem(MimeMessage message, Session session, MailResultListener listener)
    {
        this.message = message;
        this.session = session;
        this.listener = listener;
    }

    /**
     * @return the mail message to be sent
     */
    public MimeMessage getMessage()
    {
        return this.message;
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
    public MailResultListener getListener()
    {
        return this.listener;
    }
}
