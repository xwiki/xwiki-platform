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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Optimized map implementation with a key of type Right.
 *
 * @param <V> the value type of this map
 * @version $Id$
 * @since 4.0M2
 */
public class RightMap<V> extends AbstractMap<Right, V> implements Serializable, Cloneable
{
    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;

    /** Need to distinct null and NULL. */
    private static final Object NULL = new Object();

    /** Array list to store values. */
    private transient List<Object> rights;

    /** Cached key set. */
    private transient Set<Right> keySet;

    /** Cached value collection. */
    private transient Collection<V> values;

    /** Cached entry set. */
    private transient Set<Entry<Right, V>> entrySet;

    /** Cached size. */
    private transient int size;

    /** Default constructor. */
    public RightMap()
    {
        rights = new ArrayList<>(Right.size());
    }

    /**
     * Convert null to NULL.
     * @param value the value to convert
     * @return the converted value
     */
    private Object maskNull(Object value)
    {
        return (value == null ? NULL : value);
    }

    /**
     * Convert NULL to null.
     * @param value the value to convert
     * @return the converted value
     */
    @SuppressWarnings("unchecked")
    private V unmaskNull(Object value)
    {
        return (V) (value == NULL ? null : value);
    }

    @Override
    public void clear()
    {
        rights.clear();
        size = 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object clone() throws CloneNotSupportedException
    {
        RightMap<V> clone = (RightMap<V>) super.clone();
        clone.rights.addAll(rights);
        clone.size = size();
        return clone;
    }

    @Override
    public boolean containsKey(Object o)
    {
        return o instanceof Right && getValue(((Right) o).ordinal()) != null;
    }

    @Override
    public boolean containsValue(Object o)
    {
        return rights.contains(maskNull(o));
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof RightMap)) {
            return super.equals(o);
        }

        RightMap rmap = (RightMap) o;

        // Key types match, compare each value
        for (int i = 0; i < Right.size(); i++) {
            Object val = getValue(i);
            Object rval = rmap.getValue(i);
            if (rval != val && (rval == null || !rval.equals(val))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public V get(Object o)
    {
        return (o instanceof Right ? unmaskNull(getValue(((Right) o).ordinal())) : null);
    }

    @Override
    public int hashCode()
    {
        return rights.isEmpty() ? 0 : rights.hashCode();
    }

    @Override
    public boolean isEmpty()
    {
        return (size == 0);
    }

    /**
     * Retrieve a value from the map.
     * @param index index to retrieve
     * @return a value for the given index or null
     */
    @SuppressWarnings("unchecked")
    private V getValue(int index)
    {
        return (index < rights.size()) ? (V) rights.get(index) : null;
    }

    /**
     * Update a value in the map, keeping size in sync.
     * @param index the index to update
     * @param newValue the new value for this index, should be NULL for real null
     * @return the old value at the index
     */
    @SuppressWarnings("unchecked")
    private V updateValue(int index, Object newValue)
    {
        if (index < 0) {
            return null;
        }
        if (rights.size() <= index) {
            ((ArrayList) rights).ensureCapacity(Right.size() + 1);
            while (rights.size() <= Right.size()) {
                rights.add(null);
            }
        }
        Object oldValue = rights.set(index,  newValue);
        if (oldValue == null) {
            if (newValue != null) {
                size++;
            }
            return null;
        }
        if (newValue == null) {
            size--;
        }
        return unmaskNull(oldValue);
    }
    
    @Override
    public V put(Right right, V value)
    {
        return updateValue(right.ordinal(), maskNull(value));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putAll(Map<? extends Right, ? extends V> map)
    {
        if (map instanceof RightMap) {
            RightMap<? extends V> rmap = (RightMap<? extends V>) map;

            for (int i = 0; i < Right.size(); i++) {
                updateValue(i, rmap.getValue(i));
            }
        } else {
            super.putAll(map);
        }
    }

    @Override
    public V remove(Object o)
    {
        if (o instanceof Right) {
            return updateValue(((Right) o).ordinal(), null);
        }
        return null;
    }

    @Override
    public int size()
    {
        return size;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (int i = 0; i < rights.size(); i++) {
            if (getValue(i) != null) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(Right.get(i).getName())
                    .append(" = ")
                    .append(unmaskNull(getValue(i)).toString());
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Set<Right> keySet()
    {
        if (keySet == null) {
            keySet = new RightSet();
        }
        return keySet;
    }

    /**
     * Private right set for representing keys of this map.
     */
    private final class RightSet extends AbstractSet<Right>
    {
        @Override
        public Iterator<Right> iterator()
        {
            return new RightIterator();
        }

        @Override
        public int size()
        {
            return size;
        }

        @Override
        public boolean contains(Object o)
        {
            return containsKey(o);
        }

        @Override
        public boolean remove(Object o)
        {
            int oldSize = size;
            RightMap.this.remove(o);
            return size != oldSize;
        }

        @Override
        public void clear()
        {
            RightMap.this.clear();
        }
    }

    @Override
    public Collection<V> values()
    {
        if (values == null) {
            values = new Values();
        }
        return values;
    }

    /**
     * Private value collection for representing value of this map.
     */
    private final class Values extends AbstractCollection<V>
    {
        @Override
        public Iterator<V> iterator()
        {
            return new ValueIterator();
        }

        @Override
        public int size()
        {
            return size;
        }

        @Override
        public boolean contains(Object o)
        {
            return containsValue(o);
        }

        @Override
        public boolean remove(Object o)
        {
            return updateValue(rights.indexOf(maskNull(o)), null) != null;
        }

        @Override
        public void clear()
        {
            RightMap.this.clear();
        }
    }

    @Override
    public Set<Entry<Right, V>> entrySet()
    {
        if (entrySet == null) {
            entrySet = new EntrySet();
        }
        return entrySet;
    }

    /**
     * Private EntrySet for representing this map.
     */
    private final class EntrySet extends AbstractSet<Entry<Right, V>>
    {
        @Override
        public Iterator<Entry<Right, V>> iterator()
        {
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object o)
        {
            if (!(o instanceof Map.Entry)) {
                return false;
            }

            Map.Entry entry = (Map.Entry) o;
            return entry.getKey() instanceof Right
                && maskNull(entry.getValue()).equals(getValue(((Right) entry.getKey()).ordinal()));
        }

        @Override
        public boolean remove(Object o)
        {
            if (!(o instanceof Map.Entry)) {
                return false;
            }

            Map.Entry entry = (Map.Entry) o;
            if (contains(entry)) {
                updateValue(((Right) entry.getKey()).ordinal(), null);
                return true;
            }
            return false;
        }

        @Override
        public int size()
        {
            return size;
        }

        @Override
        public void clear()
        {
            RightMap.this.clear();
        }

        @Override
        public Object[] toArray()
        {
            return fillArray(new Object[size]);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] ts)
        {
            T[] a = ts;
            if (a.length < size) {
                a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
            }
            if (a.length > size) {
                a[size] = null;
            }
            return (T[]) fillArray(a);
        }

        /**
         * Fill array ts that should have the appropriate size with the {@code Entry} in this set.
         * @param a an array properly sized to receive this set
         * @return an array representing this set
         */
        private Object[] fillArray(Object[] a)
        {
            int j = 0;
            for (int i = 0; i < rights.size(); i++) {
                if (getValue(i) != null) {
                    a[j++] = new AbstractMap.SimpleEntry<Right, V>(
                        Right.get(i), unmaskNull(getValue(i)));
                }
            }
            return a;
        }
    }

    /**
     * Private abstract iterator for this map.
     * @param <T> Value type
     */
    private abstract class AbstractRightMapIterator<T> implements Iterator<T>
    {
        /** Current index. */
        protected int index;

        /** Last returned index. */
        protected int lastIndex = -1;

        @Override
        public boolean hasNext()
        {
            // Skip empty buckets
            while (index < rights.size() && getValue(index) == null) {
                index++;
            }
            return index != rights.size();
        }

        @Override
        public void remove()
        {
            if (lastIndex < 0) {
                throw new IllegalStateException();
            }
            updateValue(lastIndex, null);
            lastIndex = -1;
        }
    }

    /**
     * Private iterator for keys.
     */
    private final class RightIterator extends AbstractRightMapIterator<Right>
    {
        @Override
        public Right next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            lastIndex = index++;
            return Right.get(lastIndex);
        }
    }

    /**
     * Private iterator for values.
     */
    private final class ValueIterator extends AbstractRightMapIterator<V>
    {
        @Override
        public V next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            lastIndex = index++;
            return unmaskNull(getValue(lastIndex));
        }
    }

    /**
     * Private iterator for entries.
     */
    private final class EntryIterator extends AbstractRightMapIterator<Entry<Right, V>> implements Map.Entry<Right, V>
    {
        @Override
        public Map.Entry<Right, V> next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            lastIndex = index++;
            return this;
        }

        @Override
        public Right getKey()
        {
            checkLastIndex();
            return Right.get(lastIndex);
        }

        @Override
        public V getValue()
        {
            checkLastIndex();
            return unmaskNull(RightMap.this.getValue(lastIndex));
        }

        @Override
        public V setValue(V value)
        {
            checkLastIndex();
            return updateValue(lastIndex, value);
        }

        @Override
        public boolean equals(Object o)
        {
            if (lastIndex < 0) {
                return o == this;
            }

            if (!(o instanceof Map.Entry)) {
                return false;
            }

            Map.Entry e = (Map.Entry) o;
            V val = unmaskNull(RightMap.this.getValue(lastIndex));
            Object rval = e.getValue();
            return e.getKey() == Right.get(lastIndex) && (val == rval || (val != null && val.equals(rval)));
        }

        @Override
        public int hashCode()
        {
            if (lastIndex < 0) {
                return super.hashCode();
            }

            Object value = RightMap.this.getValue(lastIndex);
            return Right.get(lastIndex).hashCode() ^ (value == NULL ? 0 : value.hashCode());
        }

        @Override
        public String toString()
        {
            if (lastIndex < 0) {
                return super.toString();
            }

            return Right.get(lastIndex) + "=" + unmaskNull(RightMap.this.getValue(lastIndex));
        }

        /**
         * Throws if lastIndex is invalid.
         */
        private void checkLastIndex()
        {
            if (lastIndex < 0) {
                throw new IllegalStateException("Entry was removed");
            }
        }
    }

    /**
     * Serialization support.
     *
     * @param s the output stream
     * @throws IOException on error
     */
    private void writeObject(ObjectOutputStream s) throws IOException
    {
        // Write out the key type and any hidden stuff
        s.defaultWriteObject();

        // Write out size (number of Mappings)
        s.writeInt(size);

        // Write out keys and values (alternating)
        for (Map.Entry<Right, V> e :  entrySet()) {
            s.writeObject(e.getKey());
            s.writeObject(e.getValue());
        }
    }

    /**
     * Serialization support.
     *
     * @param s the input stream
     * @throws IOException on error
     * @throws ClassNotFoundException on error
     */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException
    {
        // Read in the key type and any hidden stuff
        s.defaultReadObject();

        rights = new ArrayList<Object>(Right.size());

        // Read the keys and values, and put the mappings in the HashMap
        for (int i = 0; i < s.readInt(); i++) {
            Right key = (Right) s.readObject();
            V value = (V) s.readObject();
            put(key, value);
        }
    }
}
