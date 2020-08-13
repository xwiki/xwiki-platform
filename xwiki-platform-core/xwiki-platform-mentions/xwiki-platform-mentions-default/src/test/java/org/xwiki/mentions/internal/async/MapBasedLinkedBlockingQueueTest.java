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

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.hibernate.internal.util.collections.ConcurrentReferenceHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link MapBasedLinkedBlockingQueue}.
 *
 * @version $Id$
 */
public class MapBasedLinkedBlockingQueueTest
{
    private ConcurrentMap<Long, String> map;

    @BeforeEach
    void setup()
    {
        this.map = new ConcurrentReferenceHashMap();
        map.put(13L, "Foo");
        map.put(28L, "Bar");
        map.put(0L, "Baz");
        map.put(256L, "buz");
    }

    @Test
    void constructor()
    {
        BlockingQueue<String> queue = new MapBasedLinkedBlockingQueue(this.map);
        assertEquals(4, queue.size());
        assertEquals("Baz", queue.poll());
        assertEquals("Foo", queue.poll());
        assertEquals("Bar", queue.poll());
        assertEquals("buz", queue.poll());
        assertTrue(queue.isEmpty());
        assertTrue(this.map.isEmpty());
    }

    @Test
    void contructorAndOffer()
    {
        BlockingQueue<String> queue = new MapBasedLinkedBlockingQueue(this.map);
        assertEquals(4, queue.size());
        assertTrue(queue.offer("Ahaha"));
        assertTrue(queue.offer("Foo"));
        assertEquals(6, queue.size());
        assertEquals(6, this.map.size());
        assertEquals(Integer.MAX_VALUE - 6, queue.remainingCapacity());

        assertEquals("Baz", queue.poll());
        assertEquals("Foo", queue.poll());
        assertEquals("Bar", queue.poll());
        assertEquals("buz", queue.poll());
        assertEquals("Ahaha", queue.poll());
        assertEquals("Foo", queue.poll());
        assertTrue(queue.isEmpty());
        assertTrue(this.map.isEmpty());
    }

    @Test
    void putAndPeek() throws Exception
    {
        ConcurrentReferenceHashMap localMap = new ConcurrentReferenceHashMap();
        BlockingQueue<String> queue = new MapBasedLinkedBlockingQueue(localMap);
        assertTrue(queue.isEmpty());
        queue.put("Something");
        queue.put("Else");
        assertEquals(2, queue.size());
        assertEquals(2, localMap.size());
        assertFalse(queue.isEmpty());

        assertEquals("Something", queue.peek());
        assertTrue(queue.contains("Something"));
        assertEquals(2, queue.size());
        assertEquals("Something", queue.poll());
        assertEquals(1, queue.size());
        assertEquals(1, localMap.size());
    }

    @Test
    void take() throws Exception
    {
        BlockingQueue<String> queue = new MapBasedLinkedBlockingQueue(this.map);
        assertEquals(4, queue.size());
        assertEquals("Baz", queue.take());
        assertEquals("Foo", queue.take());
        assertEquals("Bar", queue.take());
        assertEquals("buz", queue.take());
        assertTrue(queue.isEmpty());
        assertTrue(this.map.isEmpty());
    }

    @Test
    void clear()
    {
        BlockingQueue<String> queue = new MapBasedLinkedBlockingQueue(this.map);
        assertEquals(4, queue.size());
        assertFalse(queue.isEmpty());
        queue.clear();
        assertTrue(queue.isEmpty());
        assertTrue(this.map.isEmpty());
    }

    @Test
    void addElementAndIterate()
    {
        ConcurrentReferenceHashMap localMap = new ConcurrentReferenceHashMap();
        BlockingQueue<String> queue = new MapBasedLinkedBlockingQueue(localMap);
        assertTrue(queue.isEmpty());
        queue.add("Something");
        queue.add("Foo");
        queue.add("Bar");
        assertEquals(3, queue.size());
        assertEquals("Something", queue.element());
        assertEquals(3, queue.size());

        int i = 0;
        for (String s : queue) {
            switch (i) {
                case 0:
                    assertEquals("Something", s);
                    break;
                case 1:
                    assertEquals("Foo", s);
                    break;
                case 2:
                    assertEquals("Bar", s);
                    break;
            }
            i++;
        }
        assertEquals(3, i);
        assertEquals(3, queue.size());
        assertEquals(3, localMap.size());
    }

    @Test
    void retainAll()
    {
        BlockingQueue<String> queue = new MapBasedLinkedBlockingQueue(this.map);
        assertEquals(4, queue.size());
        queue.add("Bar");
        assertEquals(5, queue.size());
        assertEquals(5, this.map.size());
        assertTrue(queue.retainAll(Arrays.asList("Bar", "buz")));
        assertEquals(3, queue.size());
        assertEquals(3, this.map.size());
        assertEquals("Bar", queue.poll());
        assertEquals("buz", queue.poll());
        assertEquals("Bar", queue.poll());
        assertTrue(queue.isEmpty());
        assertTrue(this.map.isEmpty());

        queue.add("Foo");
        queue.add("Bar");
        assertFalse(queue.retainAll(Arrays.asList("Foo", "Bar")));
        assertEquals(2, queue.size());
        assertEquals(2, this.map.size());
    }

    @Test
    void removeAll()
    {
        BlockingQueue<String> queue = new MapBasedLinkedBlockingQueue(this.map);
        assertEquals(4, queue.size());
        queue.add("Bar");
        assertEquals(5, queue.size());
        assertEquals(5, this.map.size());
        assertTrue(queue.removeAll(Arrays.asList("Bar", "buz")));
        assertEquals(2, queue.size());
        assertEquals(2, this.map.size());
        assertEquals("Baz", queue.poll());
        assertEquals("Foo", queue.poll());
        assertTrue(queue.isEmpty());
        assertTrue(this.map.isEmpty());

        queue.add("Foo");
        queue.add("Bar");
        assertFalse(queue.removeAll(Collections.singleton("Toto")));
        assertEquals(2, queue.size());
        assertEquals(2, this.map.size());
    }

    @Test
    void remove()
    {
        BlockingQueue<String> queue = new MapBasedLinkedBlockingQueue(this.map);
        assertEquals(4, queue.size());
        queue.add("Bar");
        assertEquals(5, queue.size());
        assertTrue(queue.remove("Bar"));
        assertEquals(4, queue.size());
        assertEquals("Baz", queue.poll());
        assertEquals("Foo", queue.poll());
        assertEquals("buz", queue.poll());
        assertEquals("Bar", queue.poll());
        assertTrue(queue.isEmpty());
        assertTrue(this.map.isEmpty());

        queue.add("Foo");
        queue.add("Bar");
        assertFalse(queue.remove("Toto"));
        assertEquals(2, queue.size());
        assertEquals(2, this.map.size());
    }

    @Test
    void addAll()
    {
        ConcurrentHashMap<Object, Object> localMap = new ConcurrentHashMap<>();
        BlockingQueue<String> queue = new MapBasedLinkedBlockingQueue(localMap);
        assertTrue(queue.isEmpty());

        assertTrue(queue.addAll(Arrays.asList("Baz", "Foo", "buz", "Bar")));
        assertEquals(4, queue.size());
        assertEquals(4, localMap.size());
        assertEquals("Baz", queue.poll());
        assertEquals("Foo", queue.poll());
        assertEquals("buz", queue.poll());
        assertEquals("Bar", queue.poll());
        assertTrue(queue.isEmpty());
        assertTrue(localMap.isEmpty());
    }
}
