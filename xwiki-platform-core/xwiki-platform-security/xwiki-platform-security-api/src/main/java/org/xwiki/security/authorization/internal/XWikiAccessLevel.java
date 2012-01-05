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
package org.xwiki.security.authorization.internal;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.SortedSet;
import java.util.TreeSet;

import org.xwiki.security.authorization.AccessLevel;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;

/**
 * Represents access level.
 * 
 * The pattern for creating objects of this class should be as follows:
 * <ol>
 * <li>Clone or construct an instance:<br/>
 * 
 * {@code AccessLevel newInstance = existingInstance.clone();}
 * </li>
 * <li>Modify new instance via allow, deny, and clear.
 * </li>
 * <li>Try to reuse an existing instance if available:<br/>
 *
 * {@code newInstance = newInstance.reuseInstance();}
 * </li></ol>
 * After step 3. the instance is read-only.
 * @version $Id$
 */
public class XWikiAccessLevel implements AccessLevel
{
    /** Read-only flag/mask. */
    private static final int RO_MASK =  1 << 31;

    /** The default access levels. */
    private static XWikiAccessLevel defaultAccessLevel;

    /**
     * The default access levels size. Check to update defaultAccessLevel if a new Right is added.
     */
    private static int defaultAccessLevelSize;

    /** Pool of existing instances. */
    private static final SortedSet<ALWeakRef> POOL = new TreeSet<ALWeakRef>();

    /** Reference queue for removing cleared references.  */
    private static final ReferenceQueue<AccessLevel> REF_QUEUE = new ReferenceQueue<AccessLevel>();

    /**
     * We use two consecutive bits to store an access level.  The
     * high order bit indicate if the state of the level is
     * determined and the low order whether the level is set or
     * not.
     *
     * The enumeration values of the Right enum gives the offset
     * to the bits corresponding to the specific level.
     */
    private int levels;

    /**
     * @return the default access level, using the default value of all rights.
     */
    public static XWikiAccessLevel getDefaultAccessLevel()
    {
        if (defaultAccessLevel == null || Right.size() != defaultAccessLevelSize) {
            defaultAccessLevel = new XWikiAccessLevel();
            for (Right right : Right.values()) {
                defaultAccessLevel.set(right, right.getDefaultState());
            }
            defaultAccessLevel = defaultAccessLevel.getExistingInstance();
        }
        return defaultAccessLevel;
    }

    /**
     * Weak reference for storing instances in a pool.
     */
    private static class ALWeakRef extends WeakReference<XWikiAccessLevel> implements Comparable<ALWeakRef>
    {
        /** Unique order value for these reference objects. */
        private int order;

        /**
         * @param level Access level that shall be weakly referenced.
         */
        public ALWeakRef(XWikiAccessLevel level)
        {
            super(level, REF_QUEUE);
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
        SortedSet<ALWeakRef> tail = POOL.tailSet(member);
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
        while ((ref = (ALWeakRef) REF_QUEUE.poll()) != null) {
            POOL.remove(ref);
        }
    }

    /**
     * Reuse existing instance, if available.
     * @return An access levels instance that equals the given one.
     */
    public XWikiAccessLevel getExistingInstance()
    {
        setReadOnly();
        synchronized (POOL) {
            cleanQueue();
            ALWeakRef ref = new ALWeakRef(this);
            ALWeakRef pooledRef = getFromPool(ref);
            if (pooledRef != null) {
                XWikiAccessLevel pooled = pooledRef.get();
                if (pooled != null) {
                    return pooled;
                }
                POOL.remove(pooledRef);
            }
            POOL.add(ref);
        }
        return this;
    }

    /**
     * Shorthand for the shift operation.
     * @param value The value to shift.
     * @param right The right, which value is the basis for
     * computing an offset.
     * @return Shifted value.
     */
    private int shift(int value, Right right)
    {
        return value << (2 * right.ordinal());
    }
        
    /**
     * Obtain the right state for the right.
     * @param right The right to get.
     * @return The state of the right.
     */
    @Override
    public final RuleState get(Right right)
    {
        int mask   = shift(0x3, right);
        int has    = shift(0x3, right);
        int hasNot = shift(0x2, right);
        if ((levels & mask) == has) {
            return RuleState.ALLOW;
        }
        if ((levels & mask) == hasNot) {
            return RuleState.DENY;
        }
        return RuleState.UNDETERMINED;
    }

    /**
     * Set the right state on the given right.
     * @param right The right to set.
     * @param state The state to set the right to.
     */
    public void set(Right right, RuleState state)
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
        set(right, RuleState.ALLOW);
    }

    /**
     * Set deny on the given right.
     * @param right The right to deny.
     */
    public void deny(Right right)
    {
        set(right, RuleState.DENY);
    }

    /**
     * Clear the given right.  I.e., the access will not be given
     * by this object.
     * @param right The right to clear.
     */
    public void clear(Right right)
    {
        set(right, RuleState.UNDETERMINED);
    }

    @Override
    public XWikiAccessLevel clone() throws CloneNotSupportedException
    {
        try {
            XWikiAccessLevel clone = (XWikiAccessLevel) super.clone();
            clone.levels = getReadOnlyCleared();
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof XWikiAccessLevel
            && ((XWikiAccessLevel) other).getReadOnlyCleared() == getReadOnlyCleared();
    }

    @Override
    public int hashCode()
    {
        return getReadOnlyCleared();
    }

    @Override
    public int compareTo(AccessLevel other)
    {
        return ((XWikiAccessLevel) other).getReadOnlyCleared() - this.getReadOnlyCleared();
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
