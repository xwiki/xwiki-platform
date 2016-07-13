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
import javax.mail.internet.MimeBodyPart;

import org.xwiki.component.annotation.Role;

/**
 * Creates some message body Part to be added to a Multi Part message.
 *
 * @param <T> the type of content to be added to a Multi Part message
 * @version $Id$
 * @since 6.1M2
 */
@Role
public interface MimeBodyPartFactory<T>
{
    /**
     * Create a {@link javax.mail.BodyPart}.
     *
     * @param content the content of the body part (depends on the implementation, can be some String containing plain
     *        text, some String containing HTML, an attachment, etc)
     * @param parameters the list of extra parameters. This is used for example to pass alternate content for the mail
     *        using the {@code alternate} key in the HTML Mime Body Part Factory. Mail headers can also be passed using
     *        the {@code headers} key with a {@code Map<String, String>} value containing header keys and values
     * @return the created Body Part
     * @throws MessagingException when an error occurs
     */
    MimeBodyPart create(T content, Map<String, Object> parameters) throws MessagingException;
}
