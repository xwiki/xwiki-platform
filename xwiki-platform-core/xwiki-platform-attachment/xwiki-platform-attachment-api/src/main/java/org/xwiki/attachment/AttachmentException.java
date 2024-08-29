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
package org.xwiki.attachment;

/**
 * Generic exception for the Attachment module.
 *
 * @version $Id$
 * @since 14.2RC1
 */

public class AttachmentException extends Exception
{
    /**
     * Constructs a new attachment exception with {@code null} as its detail message. The cause is not initialized, and
     * may subsequently be initialized by a call to {@link #initCause}.
     */
    public AttachmentException()
    {
    }

    /**
     * Constructs a new attachment exception with the specified detail message.  The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link
     *     #getMessage()} method
     */
    public AttachmentException(String message)
    {
        super(message);
    }

    /**
     * Constructs a new attachment exception with the specified detail message and cause. Note that the detail message
     * associated with {@code cause} is <i>not</i> automatically incorporated in this exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A {@code
     *     null} value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public AttachmentException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
