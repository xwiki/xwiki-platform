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

import java.nio.charset.StandardCharsets;

import org.xwiki.mail.MimeMessageFactory;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;

/**
 * @version $Id$
 * @since 6.1M2
 */
public class DefaultMimeMessageFactory implements MimeMessageFactory
{
    @Override
    public MimeMessage create(String to, String subject, Session session)
    {
        MimeMessage message = new MimeMessage(session);
        try {
            message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject, StandardCharsets.UTF_8.name());
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return message;
    }
}