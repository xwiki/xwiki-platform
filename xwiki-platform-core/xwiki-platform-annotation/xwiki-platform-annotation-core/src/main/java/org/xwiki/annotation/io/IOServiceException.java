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

package org.xwiki.annotation.io;

/**
 * Thrown when an input/output error occurs.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class IOServiceException extends Exception
{
    /**
     * Serial version number for this type.
     */
    private static final long serialVersionUID = -3005369712366887271L;

    /**
     * Builds an IOService exception with the specified message.
     *
     * @param message the message of this exception
     */
    public IOServiceException(String message)
    {
        super(message);
    }

    /**
     * Builds an IOService exception for the specified cause with the specified message.
     *
     * @param message the message of this exception
     * @param cause the cause of the exception
     */
    public IOServiceException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
