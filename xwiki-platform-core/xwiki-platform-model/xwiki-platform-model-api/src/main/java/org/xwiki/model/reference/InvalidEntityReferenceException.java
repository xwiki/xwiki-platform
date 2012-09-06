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
package org.xwiki.model.reference;

/**
 * Thrown when an Entity Reference isn't valid. For example a Document Reference having an Attachment Reference as
 * parent is invalid. 
 *
 * @version $Id$
 * @since 2.2M1 
 */
public class InvalidEntityReferenceException extends RuntimeException
{
    /**
     * Create a new invalid entity reference exception.
     */
    public InvalidEntityReferenceException()
    {
        super();
    }

    /**
     * Create a new invalid entity reference exception with a message.
     *
     * @param message the message
     */
    public InvalidEntityReferenceException(String message)
    {
        super(message);
    }

    /**
     * Create a new invalid entity reference exception with a message and a cause.
     *
     * @param message the message
     * @param throwable the cause
     */
    public InvalidEntityReferenceException(String message, Throwable throwable)
    {
        super(message, throwable);
    }

    /**
     * Create a new invalid entity reference exception with a cause.
     *
     * @param throwable the cause
     */
    public InvalidEntityReferenceException(Throwable throwable)
    {
        super(throwable);
    }
}
