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
 * All cache entries except wiki cache entries must have their parent cached, so the {@link SecurityCacheLoader} must
 * insert the entries, if missing in turn.
 *
 * There is a chance, though, that the cache will evict a parent entry of the entry that the {@link SecurityCacheLoader}
 * is about to insert.  When this happens, this exception is thrown and the attempt to load the cache must be restarted.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class ParentEntryEvictedException extends Exception
{
    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with {@code null} as its detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause}.
     */
    public ParentEntryEvictedException()
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
    public ParentEntryEvictedException(String message)
    {
        super(message);
    }
}
