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

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * A wrapper for which all lock events will be tracked and any which
 * are left over when it is garbage collected will be unlocked.
 * A new UnlockOnFinalizeLock is expected to be created for each thread
 * and wrap the same underlying lock.
 *
 * @version $Id$
 * @since 3.1M2
 */
public class UnlockOnFinalizeLock implements Lock
{
    /** A list of each thread holding the lock. */
    private final List<Thread> threadsHoldingLock =
        (List<Thread>) Collections.synchronizedList(new ArrayList());

    /** The lock which this wraps. */
    private final RemovableLock wrappedLock;

    /**
     * The Constructor.
     *
     * @param lock the lock which this will wrap.
     */
    public UnlockOnFinalizeLock(final RemovableLock lock)
    {
        this.wrappedLock = lock;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.concurrent.locks.Lock#lock()
     */
    public void lock()
    {
        this.wrappedLock.lock();
        this.threadsHoldingLock.add(Thread.currentThread());
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.concurrent.locks.Lock#unlock()
     */
    public void unlock()
    {
        this.wrappedLock.unlock();
        this.threadsHoldingLock.remove(Thread.currentThread());
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.concurrent.locks.Lock#lockInterruptibly()
     */
    public void lockInterruptibly() throws InterruptedException
    {
        this.wrappedLock.lockInterruptibly();
        this.threadsHoldingLock.add(Thread.currentThread());
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.concurrent.locks.Lock#newCondition()
     */
    public Condition newCondition()
    {
        return this.wrappedLock.newCondition();
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.concurrent.locks.Lock#tryLock()
     */
    public boolean tryLock()
    {
        if (this.wrappedLock.tryLock()) {
            this.threadsHoldingLock.add(Thread.currentThread());
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.concurrent.locks.Lock#tryLock(long, TimeUnit)
     */
    public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException
    {
        if (this.wrappedLock.tryLock(time, unit)) {
            this.threadsHoldingLock.add(Thread.currentThread());
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#finalize()
     */
    protected void finalize()
    {
        for (final Thread thread : this.threadsHoldingLock) {
            this.wrappedLock.removeThread(thread);
        }
    }
}
