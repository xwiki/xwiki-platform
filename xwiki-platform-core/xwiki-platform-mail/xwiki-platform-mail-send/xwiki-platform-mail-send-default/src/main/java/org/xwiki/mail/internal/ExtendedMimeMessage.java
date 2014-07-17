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

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * Since there's no easy way to verify if the content of a {@link javax.mail.internet.MimeMessage} has been set we
 * need to extend it to allow for this.
 *
 * @version $Id$
 * @since 6.2M1
 */
public class ExtendedMimeMessage extends MimeMessage
{
    private boolean isEmpty;

    /**
     * @param session see javadoc for {@link MimeMessage#MimeMessage(javax.mail.Session)}
     */
    public ExtendedMimeMessage(Session session)
    {
        super(session);
    }

    /**
     * @param source see javadoc for {@link MimeMessage#MimeMessage(javax.mail.internet.MimeMessage)}
     * @throws MessagingException see javadoc for {@link MimeMessage#MimeMessage(javax.mail.internet.MimeMessage)}
     */
    public ExtendedMimeMessage(MimeMessage source) throws MessagingException
    {
        super(source);
    }

    /**
     * @return true if no body content has been defined yet for this message or false otherwise
     */
    public boolean isEmpty()
    {
        return this.dh == null;
    }
}
