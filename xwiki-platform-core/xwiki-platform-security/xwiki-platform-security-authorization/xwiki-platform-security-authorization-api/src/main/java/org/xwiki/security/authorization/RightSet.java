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
package org.xwiki.security.authorization;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Optimized set of {@link Right}.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class RightSet extends AbstractSet<Right> implements Cloneable, java.io.Serializable
{
    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;

    /** Bit vector representation to store the set. */
    private long rights;

    /** Default constructor. */
    public RightSet()
    {
        if (Right.size() > 64) {
            throw new IllegalStateException();
        }
    }

    /**
     * Create a new initialized set.
     *
     * @param rights a collection of {@code Right} object to initialize the set
     */
    public RightSet(Collection<? extends Right> rights)
    {
        if (Right.size() > 64) {
            throw new IllegalStateException();
        }
        this.addAll(rights);
    }

    /**
     * Create a new initialized set.
     * @param rights the rights you want in the set
     */
    public RightSet(Right... rights)
    {
        if (Right.size() > 64) {
            throw new IllegalStateException();
        }

        Collections.addAll(this, rights);
    }

    @Override
    public Iterator<Right> iterator()
    {
        return new RightIterator();
    }

    /**
     * Private iterator for this set.
     */
    private class RightIterator implements Iterator<Right>
    {

        /** Current index in the set, using a bit mask of remaining rights. */
        private long index;

        /** Last element returned, using a single bit mask of current element. */
        private long lastIndex;

        /** Default constructor. */
        RightIterator()
        {
            index = rights;
        }

        @Override
        public boolean hasNext()
        {
            return index != 0;
        }

        @Override
        public Right next()
        {
            if (index == 0) {
                throw new NoSuchElementException();
            }
            lastIndex = index & -index;
            index -= lastIndex;
            return Right.get(Long.numberOfTrailingZeros(lastIndex));
        }

        @Override
        public void remove()
        {
            if (lastIndex == 0) {
                throw new IllegalStateException();
            }
            rights -= lastIndex;
            lastIndex = 0;
        }
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof RightSet)) {
            return super.equals(o);
        }

        return ((RightSet) o).rights == rights;
    }

    @Override
    public int hashCode()
    {
        return Long.valueOf(rights).hashCode();
    }

    @Override
    public boolean removeAll(Collection<?> objects)
    {
        if (!(objects instanceof RightSet)) {
            return super.removeAll(objects);
        }
        long old = rights;
        rights &= ~((RightSet) objects).rights;
        return rights != old;
    }

    @Override
    public boolean add(Right right)
    {
        long old = rights;
        rights |= (1L << right.ordinal());
        return rights != old;
    }

    @Override
    public boolean addAll(Collection<? extends Right> rights)
    {
        if (!(rights instanceof RightSet)) {
            return super.addAll(rights);
        }

        long old = this.rights;
        this.rights |= ((RightSet) rights).rights;
        return this.rights != old;
    }

    @Override
    public void clear()
    {
        rights = 0;
    }

    @Override
    public boolean contains(Object o)
    {
        return o != null && o instanceof Right && (rights & (1L << ((Right) o).ordinal())) != 0;
    }

    @Override
    public boolean containsAll(Collection<?> objects)
    {
        if (!(objects instanceof RightSet)) {
            return super.containsAll(objects);
        }
        return (((RightSet) objects).rights & ~rights) == 0;
    }

    @Override
    public boolean remove(Object o)
    {
        if (o == null || !(o instanceof Right)) {
            return false;
        }
        long old = rights;
        rights &= ~(1L << ((Right) o).ordinal());
        return rights != old;
    }

    @Override
    public boolean isEmpty()
    {
        return rights == 0;
    }

    @Override
    public boolean retainAll(Collection<?> objects)
    {
        if (!(objects instanceof RightSet)) {
            return super.retainAll(objects);
        }
        long old = rights;
        rights &= ((RightSet) objects).rights;
        return rights != old;
    }

    @Override
    public int size()
    {
        // return Long.bitCount(rights);
        //
        // Would be easier and probably faster, but some versions of the Oracle/Sun implementation may have an issue
        // with Long.bitCount(), see:
        // [Java 6] Wrong results from basic comparisons after calls to Long.bitCount(long) (pmd : XPathRule_1339015068)
        // See Bug ID : 7063674
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7063674
        //
        // So we have reimplemented it based on public domain code snippets published in Bit Twiddling Hacks
        // by Sean Eron Anderson (see http://www-graphics.stanford.edu/~seander/bithacks.html#CountBitsSetParallelw)
        long v = rights - ((rights >>> 1) & 0x5555555555555555L);
        v = (v & 0x3333333333333333L) + ((v >>> 2) & 0x3333333333333333L);
        return (int) (((v + (v >> 4) & 0x0F0F0F0F0F0F0F0FL) * 0x0101010101010101L) >>> 56);
    }

    @Override
    public Object[] toArray()
    {
        return fillArray(new Object[size()]);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] ts)
    {
        T[] a = ts;
        int size = size();
        if (a.length < size) {
            a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        }
        if (a.length > size) {
            a[size] = null;
        }
        return (T[]) fillArray(a);
    }

    /**
     * Fill array ts that should have the appropriate size with the {@code Right} in this set.
     * @param ts an array properly sized to receive this set
     * @return an array representing this set
     */
    private Object[] fillArray(Object[] ts)
    {
        int j = 0;
        for (int i = 0; i < Right.size(); i++) {
            if ((rights & (1 << i)) > 0) {
                ts[j++] = Right.get(i);
            }
        }
        return ts;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (int i = 0; i < Right.size(); i++) {
            if ((rights & (1 << i)) > 0) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(Right.get(i).getName());
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public RightSet clone() throws CloneNotSupportedException
    {
        RightSet clone = (RightSet) super.clone();
        clone.rights = rights;
        return clone;
    }
}
