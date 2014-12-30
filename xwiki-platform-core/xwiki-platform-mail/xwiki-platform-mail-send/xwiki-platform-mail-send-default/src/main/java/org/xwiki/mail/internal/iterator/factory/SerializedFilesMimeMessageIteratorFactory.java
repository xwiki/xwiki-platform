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

package org.xwiki.mail.internal.iterator.factory;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Role;

/**
 *
 * @version $Id$
 * @since 6.4M3
 */
@Role
public interface SerializedFilesMimeMessageIteratorFactory
{
    /**
     * Create Iterator of MimeMessages.
     *
     * @param batchID batchID the name of the directory that contains serialized MimeMessages
     * @param parameters the parameters from which to extract the session, source and the headers
     * @return Iterator of MimeMessage generated from a serialized files MimeMessage
     * @throws MessagingException when an error occurs
     */
    Iterator<MimeMessage> create(UUID batchID, Map<String, Object> parameters) throws MessagingException;
}
