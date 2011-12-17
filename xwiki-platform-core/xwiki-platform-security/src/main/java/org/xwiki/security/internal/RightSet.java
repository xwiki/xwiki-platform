package org.xwiki.security.internal;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.xwiki.security.Right;

/**
 * Optimized set of {@link Right}.
 *
 * @version $Id$
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
        return Long.bitCount(rights);
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
    protected Object clone() throws CloneNotSupportedException
    {
        RightSet clone = (RightSet) super.clone();
        clone.rights = rights;
        return clone;
    }
}
