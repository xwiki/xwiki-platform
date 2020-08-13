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
package org.xwiki.mentions.internal.async;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.tuple.Pair;

/**
 * A dedicated implementation of {@link BlockingQueue} internally based on a {@link LinkedBlockingQueue} and on
 * a {@link ConcurrentMap}indexed by {@code Long}.
 * The idea is to be able to obtain a {@link java.util.concurrent.BlockingQueue} from a {@code Map} persisted on disk.
 * @param <T> The type of object manipulated by this queue.
 *
 * @version $Id$
 * @since 12.7RC1
 * @since 12.6.1
 */
public class MapBasedLinkedBlockingQueue<T> implements BlockingQueue<T>
{
    private final ConcurrentMap<Long, T> internalMap;
    private final AtomicLong highestKey;
    private final LinkedBlockingQueue<Pair<Long, T>> internalQueue;

    /**
     * A specific iterator class that relies on the internal queue.
     */
    private class WrappedIterator implements Iterator<T>
    {
        private Iterator<Pair<Long, T>> internalIterator;
        private Long lastReturnedKey;

        /**
         * Default constructor.
         */
        WrappedIterator()
        {
            this.internalIterator = MapBasedLinkedBlockingQueue.this.internalQueue.iterator();
        }

        @Override
        public boolean hasNext()
        {
            return this.internalIterator.hasNext();
        }

        @Override
        public T next()
        {
            Pair<Long, T> lastElement = this.internalIterator.next();
            this.lastReturnedKey = lastElement.getKey();
            return lastElement.getValue();
        }

        @Override
        public void remove()
        {
            MapBasedLinkedBlockingQueue.this.internalMap.remove(this.lastReturnedKey);
            this.internalIterator.remove();
        }
    }

    /**
     * Default constructor. It uses the provided map to populate the queue information, and then keep in sync the
     * map and the queue.
     *
     * @param internalMap the map in charge of the disk persistency.
     */
    public MapBasedLinkedBlockingQueue(ConcurrentMap<Long, T> internalMap)
    {
        super();
        this.internalMap = internalMap;
        this.highestKey = new AtomicLong(computeHighestKey());
        this.internalQueue = new LinkedBlockingQueue<>();

        // if the map contains some element, we want to populate the actual queue with those info.
        this.populateUnderlyingQueue();
    }

    /**
     * Compute from the internal map what's the highest key number.
     *
     * @return A {@code Long} corresponding to the highest key value of the Map or 0 if none is found.
     */
    private Long computeHighestKey()
    {
        return this.internalMap.keySet()
            .stream()
            // Sort in descending order to get the highest element first
            .sorted((a, b) ->
            {
                if (a.equals(b)) {
                    return 0;
                } else if (a < b) {
                    return 1;
                } else {
                    return -1;
                }
            })
            .limit(1).findFirst().orElse(0L);
    }

    private class DefaultComparator implements Comparator<Map.Entry<Long, T>>
    {
        @Override
        public int compare(Map.Entry<Long, T> a, Map.Entry<Long, T> b)
        {
            Long keyA = a.getKey();
            Long keyB = b.getKey();

            if (keyA.equals(keyB)) {
                return 0;
            } else if (keyA < keyB) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    /**
     * Populate the internal queue representation with the information coming from the Map.
     * The idea is to be able to reuse entirely the original {@link LinkedBlockingQueue} method without really needing
     * the map.
     */
    private void populateUnderlyingQueue()
    {
        this.internalMap.entrySet()
            .stream()
            .sorted(new DefaultComparator())
            .forEach((entry) ->
            {
                try {
                    this.internalQueue.put(Pair.of(entry));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * Put an element in the internal map and with the incremented highest key.
     *
     * @param t an element to add in the internal map.
     * @return the actual key used to put the info in the map.
     */
    private Long internalPut(T t)
    {
        Long key = this.highestKey.incrementAndGet();
        this.internalMap.put(key, t);
        return key;
    }

    /**
     * Remove an element from the internal map and decrement the highest key only if there's no more element
     * associated to this key.
     *
     * @param pair the element to remove, should be of type Pair.
     */
    private void internalRemove(Object pair)
    {
        if (pair != null && pair instanceof Pair) {
            Pair<Long, T> castedPair = (Pair) pair;
            this.internalMap.remove(castedPair.getKey());
        }
    }

    /**
     * Clear the internal map and reset the highest key to 0.
     */
    private void clearInternalMap()
    {
        this.internalMap.clear();
        this.highestKey.set(0);
    }

    @Override
    public void put(T t) throws InterruptedException
    {
        Long key = this.internalPut(t);
        this.internalQueue.put(Pair.of(key, t));
    }

    @Override
    public boolean offer(T t, long l, TimeUnit timeUnit) throws InterruptedException
    {
        Long key = this.internalPut(t);
        return this.internalQueue.offer(Pair.of(key, t), l, timeUnit);
    }

    @Override
    public boolean offer(T t)
    {
        Long key = this.internalPut(t);
        return this.internalQueue.offer(Pair.of(key, t));
    }

    @Override
    public T take() throws InterruptedException
    {
        Pair<Long, T> result = this.internalQueue.take();
        this.internalRemove(result);
        return result.getValue();
    }

    @Override
    public T poll(long l, TimeUnit timeUnit) throws InterruptedException
    {
        Pair<Long, T> result = this.internalQueue.poll(l, timeUnit);
        this.internalRemove(result);
        return result.getValue();
    }

    @Override
    public int remainingCapacity()
    {
        return this.internalQueue.remainingCapacity();
    }

    @Override
    public T poll()
    {
        Pair<Long, T> result = this.internalQueue.poll();
        this.internalRemove(result);
        return result.getValue();
    }

    @Override
    public T element()
    {
        return this.internalQueue.element().getValue();
    }

    @Override
    public T peek()
    {
        Pair<Long, T> result = this.internalQueue.peek();
        return result.getValue();
    }

    @Override
    public boolean remove(Object o)
    {
        boolean result = false;
        if (o != null) {
            Iterator<T> iterator = this.iterator();
            while (iterator.hasNext()) {
                T element = iterator.next();
                if (element.equals(o)) {
                    iterator.remove();
                    return true;
                }
            }
        }
        return result;
    }

    @Override
    public boolean containsAll(Collection<?> collection)
    {
        for (Object o : collection) {
            if (!this.contains(o)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int size()
    {
        return this.internalQueue.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.internalQueue.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        // We check on the map since most likely the given object is not a Pair but a T.
        return this.internalMap.containsValue(o);
    }

    @Override
    public Iterator<T> iterator()
    {
        return new WrappedIterator();
    }

    @Override
    public Object[] toArray()
    {
        return this.internalQueue.stream().map(a -> a.getValue()).toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] t1s)
    {
        return this.internalMap.values().toArray(t1s);
    }

    @Override
    public void clear()
    {
        this.internalQueue.clear();
        this.clearInternalMap();
    }

    @Override
    public int drainTo(Collection<? super T> collection)
    {
        int result = 0;
        while (!this.internalQueue.isEmpty()) {
            collection.add(this.poll());
            result++;
        }
        return result;
    }

    @Override
    public int drainTo(Collection<? super T> collection, int i)
    {
        int result = 0;
        while (!this.internalQueue.isEmpty() && result < i) {
            collection.add(this.poll());
            result++;
        }
        return result;
    }

    @Override
    public boolean add(T t)
    {
        Long key = this.internalPut(t);
        return this.internalQueue.add(Pair.of(key, t));
    }

    @Override
    public T remove()
    {
        Pair<Long, T> result = this.internalQueue.remove();
        this.internalRemove(result);
        return result.getValue();
    }

    @Override
    public boolean addAll(Collection<? extends T> collection)
    {
        boolean result = false;
        for (T t : collection) {
            boolean add = this.add(t);
            result = result || add;
        }

        return result;
    }

    @Override
    public boolean removeAll(Collection<?> collection)
    {
        boolean result = false;
        Iterator<T> iterator = this.iterator();
        while (iterator.hasNext()) {
            T element = iterator.next();
            if (collection.contains(element)) {
                iterator.remove();
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> collection)
    {
        boolean result = false;
        Iterator<T> iterator = this.iterator();
        while (iterator.hasNext()) {
            T element = iterator.next();
            if (!collection.contains(element)) {
                iterator.remove();
                result = true;
            }
        }
        return result;
    }
}
