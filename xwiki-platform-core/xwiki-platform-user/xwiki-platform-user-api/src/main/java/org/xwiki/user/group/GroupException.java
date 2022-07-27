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
package org.xwiki.user.group;

/**
 * Any exception raised in the group manager.
 *
 * @version $Id$
 * @since 10.8RC1
 */
public class GroupException extends Exception
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause(Throwable)}.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public GroupException(String message)
    {
        super(message);
    }

    /**
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). A {@code null}
     *            value is permitted, and indicates that the cause is nonexistent or unknown
     */
    public GroupException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). A {@code null}
     *            value is permitted, and indicates that the cause is nonexistent or unknown
     */
    public GroupException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
