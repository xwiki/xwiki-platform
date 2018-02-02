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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.store.locks.LockProvider;
import org.xwiki.store.locks.internal.DefaultReadWriteLock;

/**
 * A provider of fake locks.
 * Used for cases when a lock is demanded by the code but the particular operation does not require one.
 * Also useful for disabling locking.
 *
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named("dummy")
@Singleton
public class DummyLockProvider implements LockProvider
{
    /**
     * Get a fake lock.
     *
     * @param toLockOn unused.
     * @return a lock which doesn't work.
     */
    @Override
    public ReadWriteLock getLock(final Object toLockOn)
    {
        final Lock dl = new DummyLock();
        return new DefaultReadWriteLock(dl, dl);
    }
}
