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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * A lock designed never to deadlock.
 *
 * @version $Id$
 * @since 3.1M2
 */
public class PreemptiveLock implements Lock
{
    /** Exception to throw when a function is called which has not been written. */
    private static final String NOT_IMPLEMENTED = "Function not implemented.";

    /**
     * A set of all locks held by each lock-holding thread.
     * Used so that we can make sure a thread owns all of the locks which it acquired, otherwise
     * it may not acquire any more, specifically the one which it is blocked on.
     */
    private final ThreadLocal<Set<PreemptiveLock>> locksHeldByThread;

    /**
     * Find out which lock a given thread is waiting on.
     * This is needed in order to find the loop causing deadlock using an algorithm like:
     * What are you waiting for, who owns that, what is he waiting for...
     */
    private final Map<Thread, PreemptiveLock> lockBlockingThread;

    /**
     * The owner of this lock, if the owner calls multiple times then it is added again.
     * If another thread ceases the lock then it is added.
     */
    private Stack<Thread> owners = new Stack<Thread>();

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
    public PreemptiveLock(final ThreadLocal<Set<PreemptiveLock>> locksHeldByThread,
                          final Map<Thread, PreemptiveLock> lockBlockingThread)
    {
        this.locksHeldByThread = locksHeldByThread;
        this.lockBlockingThread = lockBlockingThread;
    }

    /**
     * Make sure a given thread still has control of all the locks it locked.
     *
     * @return true if all of the locks which this thread has acquired are still in it's possession.
     */
    private boolean currentThreadOwnsAllLocks()
    {
        final Thread currentThread = Thread.currentThread();
        for (PreemptiveLock lock : this.locksHeldByThread.get()) {
            if (lock.owners.peek() != currentThread) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.concurrent.locks.Lock#lock()
     */
    public synchronized void lock()
    {
        final Thread currentThread = Thread.currentThread();

        for (;;)
        {
            if (this.tryLock()) {
                return;
            }

            this.lockBlockingThread.put(currentThread, this);
            try {
                this.wait(100);
            } catch (InterruptedException e) {
                throw new RuntimeException("The thread was interrupted while waiting on the lock.");
            } finally {
                this.lockBlockingThread.remove(currentThread);
            }
        }
    }

    /**
     * Method to detect a deadlock situation in linear time.
     * Looks up what thread is holding the current lock, asks what lock is blocking that thread
     * looks up what thread is holding that lock, etc. If it reaches around to this thread then
     * there is deadlock.
     *
     * @return true if the current lock is known to be deadlocked.
     */
    private boolean isDeadlocked()
    {
        final Thread currentThread = Thread.currentThread();
        PreemptiveLock lock = this;
        Thread thread = null;
        while (lock != null && !lock.owners.empty()) {
            thread = lock.owners.peek();
            if (thread == currentThread) {
                return true;
            }
            lock = this.lockBlockingThread.get(thread);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.concurrent.locks.Lock#unlock()
     */
    public synchronized void unlock()
    {
        final Thread currentThread = Thread.currentThread();
        if (this.owners.empty() || this.owners.peek() != currentThread) {
            if (!this.owners.contains(currentThread)) {
                throw new IllegalMonitorStateException("Cannot unlock this lock as this "
                                                       + "thread does not own it.");
            }
            throw new IllegalMonitorStateException("Attempting to unlock a lock which has since been "
                                                   + "preempted, when a lock is preempted it's original "
                                                   + "thread should wait until it gets it back.\n"
                                                   + "\"This should never happen\" :(");
        }

        this.owners.pop();
        this.locksHeldByThread.get().remove(this);

        this.notify();
    }

    /**
     * {@inheritDoc}
     * Not implemented.
     *
     * @see java.util.concurrent.locks.Lock#lockInterruptibly()
     */
    public void lockInterruptibly()
    {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    /**
     * {@inheritDoc}
     * Not implemented.
     *
     * @see java.util.concurrent.locks.Lock#newCondition()
     */
    public Condition newCondition()
    {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    /**
     * {@inheritDoc}
     * Not implemented.
     *
     * @see java.util.concurrent.locks.Lock#tryLock()
     */
    public synchronized boolean tryLock()
    {
        final Set<PreemptiveLock> locksHeldByThisThread = this.locksHeldByThread.get();
        final Thread currentThread = Thread.currentThread();

        // If the current thread does not own all of it's locks then it may not acquire new locks.
        // We know that this will not become false while the code inside the if statement is
        // running because in order to preempt a lock, isDeadlocked() must be true and in order for
        // isDeadlocked() to be true, the thread must be in the lockBlockingThread map which it is not
        // unless it is in waiting mode.
        if (currentThreadOwnsAllLocks())
        {
            // If 1. the lock is unlocked, 2. reentrance, or 3. it's deadlocked.
            if (this.owners.empty()
                || this.owners.peek() == currentThread
                || this.isDeadlocked())
            {
                this.owners.push(currentThread);
                locksHeldByThisThread.add(this);
                this.lockBlockingThread.remove(currentThread);
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * Not implemented.
     *
     * @see java.util.concurrent.locks.Lock#tryLock(long, TimeUnit)
     */
    public boolean tryLock(final long time, final TimeUnit unit)
    {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }
}
