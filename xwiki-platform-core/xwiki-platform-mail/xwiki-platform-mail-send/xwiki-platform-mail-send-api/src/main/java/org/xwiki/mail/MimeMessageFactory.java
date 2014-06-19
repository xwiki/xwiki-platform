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

import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Role;

/**
 * Create a {@link MimeMessage}. This allows Component implementors the ability to pre-fill a
 * {@link javax.mail.internet.MimeMessage} according to some algorithm. For example a Template Mime Message Factory
 * could compute the subject from an XObject in a wiki page.
 *
 * @param <T> the type of the source from which to prefill the Mime Message
 * @version $Id$
 * @since 6.1RC1
 */
@Role
public interface MimeMessageFactory<T>
{
    /**
     * Create a {@link javax.mail.internet.MimeMessage}.
     *
     * @param session the JavaMail Session needed to create an instance of {@link MimeMessage} (contains all
     *        configuration such as SMTP server, SMTP port, etc)
     * @param source the source from which to prefill the Mime Message (depends on the implementation)
     * @param parameters an optional generic list of parameters. The supported parameters depend on the implementation
     * @return the pre-filled {@link javax.mail.internet.MimeMessage} that can then be further modified by the user
     * @throws MessagingException in case of an error while creating the {@link javax.mail.internet.MimeMessage}
     */
    MimeMessage createMessage(Session session, T source, Map<String, Object> parameters) throws MessagingException;
}
