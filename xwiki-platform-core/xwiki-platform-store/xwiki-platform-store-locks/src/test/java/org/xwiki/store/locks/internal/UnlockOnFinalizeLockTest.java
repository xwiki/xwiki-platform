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
 * A test of the functionality of the UnlockOnFinalizeLock.
 *
 * @version $Id$
 * @since 3.1M2
 */
public class UnlockOnFinalizeLockTest
{
    private int aliceState;

    /** Make sure the lock is dropped when the garbage collector does a run. */
    @Test
    public void unlockOnFinalizeTest() throws Exception
    {
        final PreemptiveLockProvider provider = new PreemptiveLockProvider();
        final Lock lockX1 = provider.getLock("X").writeLock();

        // Alice
        new Thread(new Runnable() {
            public void run()
            {
                Lock lockX2 = provider.getLock("X").writeLock();
                lockX2.lock();
                aliceState = 1;
                synchronized (this) {
                    this.notify();
                }
                lockX2 = null;
            }
        }).start();

        synchronized (this) {
            while (aliceState != 1) {
                this.wait(1);
            }
        }

        while (!lockX1.tryLock()) {
            Thread.sleep(1);
            System.gc();
        }
    }
}
