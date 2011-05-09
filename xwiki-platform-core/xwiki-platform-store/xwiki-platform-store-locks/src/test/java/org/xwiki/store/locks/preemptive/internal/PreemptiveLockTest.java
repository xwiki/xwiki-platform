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

import java.io.File;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.junit.Assert;
import org.junit.Test;

/**
 * A test of the components of the preemptive lock.
 *
 * @version $Id$
 * @since 3.1M2
 */
public class PreemptiveLockTest
{
    private int aliceState;

    private int bobState;

    /** Prove that the lock is mutually exclusive. */
    @Test
    public void lockingTest()
    {
        final PreemptiveLockProvider provider = new PreemptiveLockProvider();
        final Lock lockX = provider.getLock("X").writeLock();

        // Alice
        new Thread(new SleepyRunnable() {
            public void run()
            {
                lockX.lock();
                setAliceState(1);
                waitFor(lockX);
                trigger(lockX);
                if (bobState == 0) {
                    // We got the lock first, now lets release it and wait for bobState to go to 1.
                    lockX.unlock();
                    waitFor(lockX);
                    Assert.assertTrue(bobState == 1);
                }
            }
        }).start();

        // Bob
        new Thread(new SleepyRunnable() {
            public void run()
            {
                lockX.lock();
                setBobState(1);
                trigger(lockX);
                if (aliceState == 0) {
                    // We got the lock first, now lets release it and wait for aliceState to go to 1.
                    lockX.unlock();
                    waitFor(lockX);
                    Assert.assertTrue(aliceState == 1);
                }
            }
        }).start();
    }

    @Test(expected=IllegalMonitorStateException.class)
    public void unlockWithoutLockTest()
    {
        final PreemptiveLockProvider provider = new PreemptiveLockProvider();
        final Lock lockX = provider.getLock("X").writeLock();
        lockX.unlock();
    }

    @Test
    public synchronized void reentrenceTest() throws Exception
    {
        final PreemptiveLockProvider provider = new PreemptiveLockProvider();
        final Lock lockX = provider.getLock("X").writeLock();

        new Thread(new SleepyRunnable() {
            public void run()
            {
                lockX.lock();
                lockX.lock();
                setAliceState(1);
                waitFor(lockX);
                lockX.unlock();
                setAliceState(2);
                waitFor(lockX);
                lockX.unlock();
                setAliceState(3);
            }
        }).start();

        while (aliceState != 1) {
            this.wait();
        }

        Assert.assertFalse(lockX.tryLock());
        trigger(lockX);

        while (aliceState != 2) {
            this.wait();
        }

        Assert.assertFalse(lockX.tryLock());
        trigger(lockX);

        while (aliceState != 3) {
            this.wait();
        }

        Assert.assertTrue(lockX.tryLock());
    }

    @Test
    public synchronized void deadlockTest() throws Exception
    {
        final PreemptiveLockProvider provider = new PreemptiveLockProvider();
        final Lock lockX = provider.getLock("X").writeLock();
        final Lock lockY = provider.getLock("Y").writeLock();

        new Thread(new SleepyRunnable() {
            public void run()
            {
                lockX.lock();
                setAliceState(1);
                waitFor(lockX);
                lockY.lock();
                setAliceState(2);
            }
        }).start();

        lockY.lock();

        while (aliceState != 1) {
            this.wait(1);
        }
        trigger(lockX);

        // Try to prevent a race condition from causing test failure...
        int i = 0;
        while (lockX.tryLock() == false) {
            trigger(lockX);
            Thread.sleep(1);
            i++;
            Assert.assertTrue(i < 100);
        }

        lockX.unlock();
        lockY.unlock();

        i = 0;
        while (aliceState != 2) {
            this.wait(1);
            i++;
            Assert.assertTrue(i < 10000);
        }
    }

    private static void trigger(Object o)
    {
        synchronized (o) {
            o.notifyAll();
        }
    }

    private synchronized void setAliceState(int state)
    {
        this.aliceState = state;
        this.notifyAll();
    }

    private synchronized void setBobState(int state)
    {
        this.bobState = state;
        this.notifyAll();
    }

    private static abstract class SleepyRunnable implements Runnable
    {
        protected void waitFor(Object o)
        {
            synchronized (o) {
                try {
                    o.wait();
                } catch (Exception e) { }
            }
        }
    }
}
