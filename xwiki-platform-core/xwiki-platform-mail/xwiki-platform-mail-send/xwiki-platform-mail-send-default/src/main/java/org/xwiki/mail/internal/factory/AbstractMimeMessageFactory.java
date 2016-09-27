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
package org.xwiki.mail.internal.factory;

import javax.mail.MessagingException;

import org.xwiki.mail.MimeMessageFactory;

/**
 * Helper class for writing {@link MimeMessageFactory}.
 *
 * @param <T> the return type of what gets created (usually a {@link javax.mail.internet.MimeMessage} or an
 *        {@link java.util.Iterator} of {@link javax.mail.internet.MimeMessage})
 * @version $Id$
 * @since 6.4.2
 * @since 7.0M2
 */
public abstract class AbstractMimeMessageFactory<T> implements MimeMessageFactory<T>
{
    protected <U> U getTypedSource(Object source, Class<U> expectedSourceClass) throws MessagingException
    {
        if (!expectedSourceClass.isInstance(source)) {
            throw new MessagingException(String.format("Invalid source parameter of type [%s]. Must be of type [%s]",
                source.getClass().getName(), expectedSourceClass.getName()));
        }
        return expectedSourceClass.cast(source);
    }
}
