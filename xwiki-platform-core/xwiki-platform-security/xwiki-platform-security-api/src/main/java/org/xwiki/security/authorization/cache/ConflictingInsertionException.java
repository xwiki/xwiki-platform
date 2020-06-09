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
package org.xwiki.security.authorization.cache;

/**
 * This exception is thrown if two or more parallel threads have attempted to load the cache with the same entry at the
 * same time as a right was updated, so the resulting entries are different.
 *
 * When this happens, there must be a pending event waiting to be delivered, which will remove the offending entry from
 * the cache.  Thus, the appropriate action is to restart the attempt to load the cache when catching this, after the
 * lock that blocks the event delivery has ben released and required.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class ConflictingInsertionException extends Exception
{
    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with {@code null} as its detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause}.
     */
    public ConflictingInsertionException()
    {

    }

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *            method.
     * @since 12.5RC1
     * @since 11.10.6
     */
    public ConflictingInsertionException(String message)
    {
        super(message);
    }
}
