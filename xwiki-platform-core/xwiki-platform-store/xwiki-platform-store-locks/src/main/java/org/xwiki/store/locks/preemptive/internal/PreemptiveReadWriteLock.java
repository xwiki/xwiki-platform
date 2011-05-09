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
package org.xwiki.store.locks.preemptive.internal;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * This ReadWriteLock implementation based on preemptive locks.
 * Currently this implementation only returns one lock for the read and write locks.
 * TODO: Modify PreemptiveLock to handle groups of threads instead of single threads
 * real read/write locking a reality.
 *
 * @version $Id$
 * @since 3.1M2
 */
public class PreemptiveReadWriteLock implements ReadWriteLock
{
    /** The only lock in this readWriteLock. */
    private final Lock onlyLock;

    /**
     * The Constructor.
     *
     * @param locksHeldByThread a ThreadLocal which is used internally to detect and break deadlock
     *        conditions, the same ThreadLocal must be passed to all new locks if there is a possibility
     *        of them deadlocking and the it must not be interfered with externally. This ThreadLocal must
     *        return an empty Set of PreemptiveLocks when {@link ThreadLocal#initialValue()} is called.
     * @param lockBlockingThread another map used internally to detect and break deadlock.
     *        the same map must be passed to all new locks if there is a possibility of
     *        them deadlocking and the map must not be interfered with externally.
     */
    public PreemptiveReadWriteLock(final ThreadLocal<Set<PreemptiveLock>> locksHeldByThread,
                                   final Map<Thread, PreemptiveLock> lockBlockingThread)
    {
        this.onlyLock = new PreemptiveLock(locksHeldByThread, lockBlockingThread);
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.concurrent.locks.ReadWriteLock#readLock()
     */
    public Lock readLock()
    {
        return this.onlyLock;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.concurrent.locks.ReadWriteLock#writeLock()
     */
    public Lock writeLock()
    {
        return this.onlyLock;
    }
}
