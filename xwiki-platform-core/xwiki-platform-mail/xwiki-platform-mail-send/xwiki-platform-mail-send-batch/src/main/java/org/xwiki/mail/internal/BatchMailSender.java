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

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Send batch mails.
 *
 * @version $Id$
 * @since 6.4M2
 */
@Role
@Unstable
public interface BatchMailSender
{
    /**
     * Send batch mail asynchronously.
     *
     * @param messageIterator the iterator of emails to send
     * @param session the JavaMail session containing all the configuration for the SMTP host, port, etc
     */
    void send(Iterator<MimeMessage> messageIterator, Session session);
}
