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

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Send mails asynchronously with option to wait for the mail to be sent.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Role
@Unstable
public interface MailSender
{
    /**
     * Send a mail.
     *
     * @param message the message to sent
     * @param session the JavaMail session containing all the configuration for the SMTP host, port, etc
     * @throws MessagingException when an error occurs
     */
    void send(MimeMessage message, Session session) throws MessagingException;

    /**
     * Send a mail.
     *
     * @param message the message to sent
     * @param session the JavaMail session containing all the configuration for the SMTP host, port, etc
     * @param listener a listener called when the mail is sent succesfully or when there is an error
     * @throws MessagingException when an error occurs
     */
    void send(MimeMessage message, Session session, MailResultListener listener) throws MessagingException;

    /**
     * Wait for all messages on the sending queue to be sent before returning.
     *
     * @param timeout the maximum amount of time to wait in milliseconds
     */
    void waitTillSent(long timeout);
}
