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
package org.xwiki.store.locks.dummy.internal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
//import java.util.concurrent.locks.ReentrantLock;

/**
 * A fake lock.
 * Used for cases when a lock is demanded by the code but the perticular operation does not require one.
 * Also useful for disabling locking.
 *
 * @version $Id$
 * @since 4.3M2
 */
public class DummyLock implements Lock
{
    /** a default instance, can be always used, as this {@link Lock} is stateless. */
    public static final DummyLock INSTANCE = new DummyLock();

    @Override
    public void lock()
    {
        // This is a really complicated operation.
    }

    @Override
    public void lockInterruptibly()
    {
        // This one even more so.
    }

    @Override
    public boolean tryLock()
    {
        return true;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit)
    {
        return true;
    }

    @Override
    public void unlock()
    {
        // Reverse of the above process.
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }
}
