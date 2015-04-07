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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Create one or several {@link javax.mail.internet.MimeMessage}. This allows Component implementers the ability to
 * create and pre-fill one or several {@link javax.mail.internet.MimeMessage} objects according to some algorithm.
 * For example a Template Mime Message Factory could compute the subject and its content from an XObject in a wiki
 * page. And a Group Mime Message Factory could generate an Iterator of MimeMessage, which itself would generate one
 * MimeMessage per user in the Group.
 *
 * @param <T> the return type of what gets created (usually a {@link javax.mail.internet.MimeMessage} or an
 *        {@link java.util.Iterator} of {@link javax.mail.internet.MimeMessage})
 * @version $Id$
 * @since 6.4.1
 */
@Role
@Unstable
public interface MimeMessageFactory<T>
{
    /**
     * Create a {@link javax.mail.internet.MimeMessage}.
     *
     * @param session the JavaMail Session needed to create the instance(s) of {@link javax.mail.internet.MimeMessage}
     *        (contains all configuration such as SMTP server, SMTP port, etc)
     * @param source the source from which to prefill the Mime Message(s) (depends on the implementation)
     * @param parameters an optional generic list of parameters. The supported parameters depend on the implementation
     * @return the pre-filled {@link javax.mail.internet.MimeMessage}(s) that can then be further modified by the user
     * @throws MessagingException in case of an error while creating the {@link javax.mail.internet.MimeMessage}(s)
     */
    T createMessage(Session session, Object source, Map<String, Object> parameters) throws MessagingException;
}
