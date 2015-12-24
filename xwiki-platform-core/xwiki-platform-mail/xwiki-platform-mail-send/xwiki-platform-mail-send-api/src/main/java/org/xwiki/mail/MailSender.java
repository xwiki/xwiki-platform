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
package org.xwiki.mail;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Role;

/**
 * Send mails.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Role
public interface MailSender
{
    /**
     * Send a list of mails asynchronously (the call returns immediately).
     *
     * @param messages the list of messages to sent
     * @param session the JavaMail session containing all the configuration for the SMTP host, port, etc
     * @param listener a listener called when the mail is sent successfully or when there is an error
     * @return the result of the mail sending
     * @since 6.2M1
     */
    MailResult sendAsynchronously(Iterable<? extends MimeMessage> messages, Session session, MailListener listener);
}
