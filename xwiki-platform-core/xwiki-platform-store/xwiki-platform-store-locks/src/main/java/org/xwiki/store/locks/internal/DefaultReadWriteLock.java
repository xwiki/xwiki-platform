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
package org.xwiki.store.locks.internal;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * A simple ReadWriteLock implementation.
 *
 * @version $Id$
 * @since 3.1M2
 */
public class DefaultReadWriteLock implements ReadWriteLock
{
    /**
     * The lock for reading.
     */
    private final Lock readLock;

    /**
     * The lock for writing.
     */
    private final Lock writeLock;

    /**
     * The Constructor.
     *
     * @param readLock the lock which wil be returned by calls to {@link #readlock()}.
     * @param writeLock the lock which wil be returned by calls to {@link #writelock()}.
     */
    public DefaultReadWriteLock(final Lock readLock, final Lock writeLock)
    {
        this.readLock = readLock;
        this.writeLock = writeLock;
    }

    @Override
    public Lock readLock()
    {
        return this.readLock;
    }

    @Override
    public Lock writeLock()
    {
        return this.writeLock;
    }
}
