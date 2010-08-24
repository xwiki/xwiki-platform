/*
 * Copyright 2010 Andreas Jonsson
 * 
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
 *
 */
package org.xwiki.security;

import java.util.TreeSet;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;

import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;

/**
 * Represents access level.
 * 
 * The pattern for creating objects of this class should be as follows:
 *
 * 1. Clone or construct an instance:
 * 
 *    AccessLevel newInstance = existingInstance.clone();
 *
 * 2. Modify new instance via allow, deny, and clear.
 *
 * 3. Try to reuse an existing instance if available:
 *
 *    newInstance = newInstance.reuseInstance();
 *
 * After step 3. the instance is read-only.
 * @version $Id$
 */
public class AccessLevel implements RightCacheEntry, Cloneable, Comparable<AccessLevel>
{
    /**
     * The default access levels.
     */
    public static final AccessLevel DEFAULT_ACCESS_LEVEL;

    /** Read-only flag/mask. */
    private static final int RO_MASK =  1 << 31;

    /** Pool of existing instances. */
    private static SortedSet<ALWeakRef> pool = new TreeSet();
    /** Reference queue for removing cleared references.  */
    private static ReferenceQueue<AccessLevel> refQueue = new ReferenceQueue();

    static {
        DEFAULT_ACCESS_LEVEL = new AccessLevel() 
            {
                {
                    allow(Right.VIEW);
                    allow(Right.EDIT);
                    allow(Right.COMMENT);
                    allow(Right.LOGIN);
                    allow(Right.REGISTER);
                    deny(Right.DELETE);
                    deny(Right.ADMIN);
                    deny(Right.PROGRAM);
                    deny(Right.ILLEGAL);
                }
            } .getExistingInstance();
    }

    /**
     * We use two consequtive bits to store an access level.  The
     * high order bit indicate if the state of the level is
     * determined and the low order wether the level is set or
     * not.
     *
     * The enumeration values of the Right enum gives the offset
     * to the bits corresponding to the specific level.
     */
    private int levels;

    /**
     * Weak reference for storing instances in a pool.
     */
    private static class ALWeakRef extends WeakReference<AccessLevel> implements Comparable<ALWeakRef>
    {
        /** Unique order value for these reference objects. */
        private int order;

        /**
         * @param level Access level that shall be weakly referenced.
         */
        public ALWeakRef(AccessLevel level)
        {
            super(level, refQueue);
            order = level.levels;
        }

        @Override
        public int compareTo(ALWeakRef other)
        {
            return order - other.order;
        }

        @Override
        public boolean equals(Object other)
        {
            return other instanceof ALWeakRef && order == ((ALWeakRef) other).order;
        }

        @Override
        public int hashCode()
        {
            return order;
        }
    }

    /**
     * @param member Return from the pool an existing instance that is
     * equal to member, if exists.
     * @return An instance that is equal to the given one, or null if no such instance exist.
     */
    private ALWeakRef getFromPool(ALWeakRef member)
    {
        SortedSet<ALWeakRef> tail = pool.tailSet(member);
        if (tail.size() > 0 && member.equals(tail.first())) {
            return tail.first();
        }
        return null;
    }

    /**
     * Cleanup the reference queue.
     */
    private void cleanQueue()
    {
        ALWeakRef ref;
        while ((ref = (ALWeakRef) refQueue.poll()) != null) {
            pool.remove(ref);
        }
    }

    /**
     * Reuse existing instance, if available.
     * @param levels
     * @return An access levels instance that equals the given one.
     */
    public AccessLevel getExistingInstance()
    {
        setReadOnly();
        synchronized (pool) {
            cleanQueue();
            ALWeakRef ref = new ALWeakRef(this);
            ALWeakRef pooledRef = getFromPool(ref);
            if (pooledRef != null) {
                AccessLevel pooled = pooledRef.get();
                if (pooled != null) {
                    return pooled;
                }
                pool.remove(pooledRef);
            }
            pool.add(ref);
        }
        return this;
    }

    /**
     * Shorthand for the shift operation.
     * @param value The value to shift.
     * @param right The right, which's value is the basis for
     * computing an offset.
     * @return Shifted value.
     */
    private int shift(int value, Right right)
    {
        return value << (2 * right.getValue());
    }
        
    /**
     * Obtain the right state for the right.
     * @param right The right to get.
     * @return The state of the right.
     */
    public final RightState get(Right right)
    {
        int mask   = shift(0x3, right);
        int has    = shift(0x3, right);
        int hasNot = shift(0x2, right);
        if ((levels & mask) == has) {
            return RightState.ALLOW;
        }
        if ((levels & mask) == hasNot) {
            return RightState.DENY;
        }
        return RightState.UNDETERMINED;
    }

    /**
     * Set the right state on the given right.
     * @param right The right to set.
     * @param state The state to set the right to.
     */
    public void set(Right right, RightState state)
    {
        assert !isReadOnly();
        int mask = ~shift(0x3, right);
        levels = (levels & mask) | shift(state.getValue(), right);
    }

    /** Flag this entry as read only. */
    private void setReadOnly()
    {
        levels |= RO_MASK;
    }

    /** @return true if and only if this entry is read-only. */
    private boolean isReadOnly()
    {
        return (levels & RO_MASK) != 0;
    }

    /** @return the access levels with the read only flag cleared. */
    private int getReadOnlyCleared()
    {
        return levels & ~RO_MASK;
    }

    /**
     * Set allow on the given right.
     * @param right The right to allow.
     */
    public void allow(Right right)
    {
        set(right, RightState.ALLOW);
    }

    /**
     * Set deny on the given right.
     * @param right The right to deny.
     */
    public void deny(Right right)
    {
        set(right, RightState.DENY);
    }

    /**
     * Clear the given right.  I.e., the access will not be given
     * by this object.
     * @param right The right to clear.
     */
    public void clear(Right right)
    {
        set(right, RightState.UNDETERMINED);
    }

    @Override
    public AccessLevel clone()
    {
        try {
            AccessLevel clone = (AccessLevel) super.clone();
            clone.levels = getReadOnlyCleared();
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof AccessLevel && ((AccessLevel) other).getReadOnlyCleared() == getReadOnlyCleared();
    }

    @Override
    public int hashCode()
    {
        return getReadOnlyCleared();
    }

    @Override
    public int compareTo(AccessLevel other) 
    {
        return other.getReadOnlyCleared() - this.getReadOnlyCleared();
    }

    @Override
    public RightCacheEntry.Type getType()
    {
        return RightCacheEntry.Type.ACCESS_LEVEL;
    }

    @Override
    public <T> Collection<T> getObjects(Class<T> type)
    {
        return Collections.EMPTY_SET;
    }

    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (Right r : Right.values()) {
            if (first) {
                first = false;
            } else {
                b.append(", ");
            }
            b.append(r).append(": ").append(get(r));
        }
        return b.toString();
    }
}
