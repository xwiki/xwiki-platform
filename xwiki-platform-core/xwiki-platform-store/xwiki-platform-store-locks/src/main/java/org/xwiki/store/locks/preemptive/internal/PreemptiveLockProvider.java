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

import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.lang.ref.WeakReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.Map;
import java.util.WeakHashMap;

import javax.inject.Singleton;
import org.xwiki.component.annotation.Component;
import org.xwiki.store.locks.internal.DefaultReadWriteLock;
import org.xwiki.store.locks.LockProvider;

/**
 * A provider of preemptive locks.
 * If all locks are gotten through this provider then they are guaranteed never to deadlock or livelock.
 * The only requirement placed upon users of the locks is that they do not begin doing work until they have
 * all of their locks because they may temporarily lose a lock while waiting for another.
 *
 * @version $Id$
 * @since 3.1M2
 */
@Component
@Singleton
public class PreemptiveLockProvider implements LockProvider
{
    /** A map which holds locks by the object so that the same lock is used for any equivilent object. */
    private final Map<Object, WeakReference<ReadWriteLock>> lockMap =
        new WeakHashMap<Object, WeakReference<ReadWriteLock>>();

    /** Used by PreemptiveLock. */
    private final ThreadLocal<Set<PreemptiveLock>> locksHeldByThread =
        new ThreadLocal<Set<PreemptiveLock>>() {
            protected Set<PreemptiveLock> initialValue()
            {
                return new HashSet<PreemptiveLock>();
            }
        };

    /** Used by PreemptiveLock. */
    private final Map<Thread, PreemptiveLock> lockBlockingThread =
        new HashMap<Thread, PreemptiveLock>();

    /**
     * Get a lock for an object.
     * If the object is equal to another object gotten through this function then
     * the lock will be identical. As long as the lock is referenced IE: not garbage collected,
     * the original object will be referenced as well so holding locks on large objects will
     * cause memory leakage. Because of the guarantee of the same lock being used for equivilent
     * objects, you can get locks as needed and throw them out when you are done with them.
     *
     * @param toLockOn the object to get a lock for.
     * @return a lock for this object and any which are equal.
     */
    public synchronized ReadWriteLock getLock(final Object toLockOn)
    {
        final WeakReference<ReadWriteLock> lock = this.lockMap.get(toLockOn);
        ReadWriteLock strongLock = null;
        if (lock != null) {
            strongLock = lock.get();
        }
        if (strongLock == null) {
            final Lock preemptiveLock = new PreemptiveLock(this.locksHeldByThread, this.lockBlockingThread)
            {
                /**
                 * A strong reference on the object to make sure that the
                 * mere existence of the lock will keep it in the map.
                 */
                private final Object lockMapReference = toLockOn;
            };
            // Currently using the same lock for reading and writing, TODO: fix
            strongLock = new DefaultReadWriteLock(preemptiveLock, preemptiveLock);
            this.lockMap.put(toLockOn, new WeakReference<ReadWriteLock>(strongLock));
        }

        return strongLock;
    }
}
