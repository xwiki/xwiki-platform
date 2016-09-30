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
package org.xwiki.store.locks;

import java.util.concurrent.locks.ReadWriteLock;

import org.xwiki.component.annotation.Role;

/**
 * A means of getting a lock for a specific object.
 *
 * @version $Id$
 * @since 3.1M2
 */
@Role
public interface LockProvider
{
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
    ReadWriteLock getLock(Object toLockOn);
}
