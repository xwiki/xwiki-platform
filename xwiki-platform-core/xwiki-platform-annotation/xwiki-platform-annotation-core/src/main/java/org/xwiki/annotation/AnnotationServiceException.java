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

package org.xwiki.annotation;

/**
 * General wrapping exception.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class AnnotationServiceException extends Exception
{
    /**
     * Serial version number for this type.
     */
    private static final long serialVersionUID = 3856169545666798382L;

    /**
     * Builds an annotation exception with the specified message.
     *
     * @param message the message of this exception
     */
    public AnnotationServiceException(String message)
    {
        super(message);
    }

    /**
     * Builds an annotation exception for the specified cause.
     *
     * @param cause the cause of this exception
     */
    public AnnotationServiceException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Builds an annotation exception for the specified cause with the specified message.
     *
     * @param message the message of this exception
     * @param cause the cause of the exception
     */
    public AnnotationServiceException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
