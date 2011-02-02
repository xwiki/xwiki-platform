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
package org.xwiki.store.filesystem.internal;

import java.io.File;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.junit.Assert;
import org.junit.Test;

/**
 * Some tests for DefaultFilesystemStoreTools, specifically the locking functionality.
 *
 * @version $Id$
 * @since 3.0M1
 */
public class DefaultFilesystemStoreToolsTest
{
    private int aliceState;

    private int bobState;

    private int charleyState;

    /**
     * Simulating this situation:
     * <code>
     * Alice ----X--Y--z                Z----(Y)-(X)----(Z)---
     *
     * Bob ------Y--x                             X-----(X)---
     *
     * Charley --Z-----Y---X---(Z)-(Y)-(X)--------------------
     *
     * Event:    1  2  3   4    5   6   7     8   9      10
     * </code>
     */
    @Test
    public void deadlockTest()
    {
        final DefaultFilesystemStoreTools dfst = new DefaultFilesystemStoreTools(null, null);

        final Object event1 = "event1";
        final Object event2 = "event2";
        final Object event2b = "event2b";
        final Object event3 = "event3";
        final Object event3b = "event3b";
        final Object event456 = "event456";
        final Object event7 = "event7";
        final Object event8 = "event8";
        final Object event9 = "event9";
        final Object event10 = "event10";

        final Lock lockX = dfst.getLockForFile(new File("X")).writeLock();
        final Lock lockY = dfst.getLockForFile(new File("Y")).writeLock();
        final Lock lockZ = dfst.getLockForFile(new File("Z")).writeLock();

        // Alice
        new Thread(new SleepyRunnable() {
            public void run()
            {
                waitFor(event1);
                lockX.lock();
                setAliceState(1);

                // In order for Bob to lose the lock, he must get it first.
                waitFor(event2b);
                lockY.lock();
                setAliceState(2);

                waitFor(event3);
                lockZ.lock();
                setAliceState(7);

                waitFor(event8);
                lockY.unlock();
                setAliceState(8);

                waitFor(event9);
                lockX.unlock();
                setAliceState(9);
                // at this point Bob should be awake.

                waitFor(event10);
                lockZ.unlock();
                setAliceState(10);
            }
        }).start();

        // Bob
        new Thread(new SleepyRunnable() {
            public void run()
            {
                waitFor(event1);
                lockY.lock();
                setBobState(1);

                waitFor(event2);
                lockX.lock();
                setBobState(9);
                // make sure Bob is blocked until event9

                waitFor(event10);
                lockX.unlock();
                setBobState(10);
            }
        }).start();

        // Charley
        new Thread(new SleepyRunnable() {
            public void run()
            {
                waitFor(event1);
                lockZ.lock();
                setCharleyState(1);

                waitFor(event3b);
                lockY.lock();
                setCharleyState(3);

                waitFor(event456);
                lockX.lock();
                setCharleyState(4);
                lockZ.unlock();
                setCharleyState(5);
                lockY.unlock();
                setCharleyState(6);
                // At this point we should assert that neither alice nor Bob have moved.

                waitFor(event7);
                lockX.unlock();
                setCharleyState(10);
                // Assert that alice has moved on lockZ
            }
        }).start();

        triggerAndExpect(event1, 1, 1, 1);
        //printState();

        // This just sends Bob into a block state.
        triggerAndExpect(event2, 1, 1, 1);

        // Alice steals lock from Bob.
        triggerAndExpect(event2b, 2, 1, 1);
        //printState();

        // Alice reaches for lockZ and fails.
        triggerAndExpect(event3, 2, 1, 1);

        // Charley steals lockZ from Alice.
        triggerAndExpect(event3b, 2, 1, 3);
        //printState();

        // Charley releases some locks.
        triggerAndExpect(event456, 2, 1, 6);
        //printState();

        // Charley releases lockX, unblocking Alice.
        triggerAndExpect(event7, 7, 1, 10);
        //printState();

        // Alice releases a lock which should not matter to Bob.
        triggerAndExpect(event8, 8, 1, 10);
        //printState();

        // Alice releases lockX, unblocking Bob.
        triggerAndExpect(event9, 9, 9, 10);
        //printState();

        // The last locks are released.
        triggerAndExpect(event10, 10, 10, 10);
        //printState();
    }

    private void triggerAndExpect(final Object mutex,
                                  final int aState,
                                  final int bState,
                                  final int cState)
    {
        final long time = System.currentTimeMillis();
        int a = 0;
        int b = 0;
        int c = 0;
        do {
            synchronized (mutex) {
                mutex.notifyAll();
            }
            synchronized (this) {
                if (System.currentTimeMillis() - time > 10000) {
                    Assert.fail("Calling " + mutex + " and expecting:\n" +
                                getState(aState, bState, cState) + "\nBut got:\n" +
                                getState(a, b, c));
                }
                try {
                    this.wait(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while waiting.");
                }
                a = this.aliceState;
                b = this.bobState;
                c = this.charleyState;
            }
        } while(a != aState || b != bState || c != cState);
    }

    private static void trigger(Object o)
    {
        synchronized (o) {
            o.notifyAll();
        }
    }

    private void printState()
    {
        System.out.println(getState(this.aliceState, this.bobState, this.charleyState));
    }

    private String getState(int aliceState, int bobState, int charleyState)
    {
        return "Alice is at event #" + aliceState + " Bob is at event #"
               + bobState + " and Charley is at event #" + charleyState;
    }

    private boolean isState(int aliceState, int bobState, int charleyState)
    {
        return (this.aliceState == aliceState
                && this.bobState == bobState
                && this.charleyState == charleyState);
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

    private synchronized void setCharleyState(int state)
    {
        this.charleyState = state;
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
