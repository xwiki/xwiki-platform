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
package com.xpn.xwiki.internal;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.junit.Test;

/**
 * Unit tests for {@link AbstractNotifyOnUpdateList}.
 * 
 * @version $Id$
 */
public class AbstractNotifyOnUpdateListTest
{
    /**
     * The concrete {@link AbstractNotifyOnUpdateList} implementation used in tests.
     */
    private static class NotifyOnUpdateList extends AbstractNotifyOnUpdateList<Integer>
    {
        private int counter;

        protected NotifyOnUpdateList(List<Integer> list)
        {
            super(list);
        }

        @Override
        protected void onUpdate()
        {
            counter++;
        }

        public int getCounter()
        {
            return counter;
        }
    }

    @Test
    public void testUpdate()
    {
        int expectedCounter = 0;
        NotifyOnUpdateList list = new NotifyOnUpdateList(new ArrayList<Integer>());
        assertEquals(expectedCounter, list.getCounter());

        list.add(13);
        assertEquals(++expectedCounter, list.getCounter());

        list.add(0, 27);
        assertEquals(++expectedCounter, list.getCounter());

        assertTrue(list.contains(13));
        assertFalse(list.isEmpty());
        assertEquals(2, list.size());

        list.clear();
        assertEquals(++expectedCounter, list.getCounter());
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());

        assertFalse(list.contains(27));

        list.addAll(Arrays.asList(1, 2));
        assertEquals(++expectedCounter, list.getCounter());
        assertEquals(2, list.size());

        assertTrue(list.containsAll(Arrays.asList(2, 1)));

        assertEquals(Arrays.asList(1, 2), list);
        assertNotEquals(Arrays.asList(2, 1), list);

        list.remove(Integer.valueOf(3));
        assertEquals(expectedCounter, list.getCounter());

        list.remove(Integer.valueOf(1));
        assertEquals(++expectedCounter, list.getCounter());

        list.removeAll(Arrays.asList(3, 2));
        assertEquals(++expectedCounter, list.getCounter());

        list.addAll(0, Arrays.asList(4, 5));
        assertEquals(++expectedCounter, list.getCounter());

        list.removeAll(Arrays.asList(3, 2));
        assertEquals(expectedCounter, list.getCounter());

        list.retainAll(Arrays.asList(5, 4, 3));
        assertEquals(expectedCounter, list.getCounter());

        list.retainAll(Arrays.asList(5, 6));
        assertEquals(++expectedCounter, list.getCounter());

        list.set(0, 25);
        assertEquals(++expectedCounter, list.getCounter());
    }

    @Test
    public void testToString()
    {
        List<Integer> list = Arrays.asList(3, 2, 1);
        assertEquals(list.toString(), new NotifyOnUpdateList(list).toString());
    }

    @Test
    public void testSubList()
    {
        int expectedCounter = 0;
        NotifyOnUpdateList list = new NotifyOnUpdateList(Arrays.asList(5, 4, 3, 2, 1));
        assertEquals(expectedCounter, list.getCounter());

        List<Integer> subList = list.subList(1, 4);
        subList.set(0, 16);
        assertEquals(++expectedCounter, list.getCounter());
    }

    @Test
    public void testIterator()
    {
        int expectedCounter = 0;
        NotifyOnUpdateList list = new NotifyOnUpdateList(new LinkedList<Integer>(Arrays.asList(1, 2)));
        assertEquals(expectedCounter, list.getCounter());

        Iterator<Integer> iterator = list.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
            assertEquals(++expectedCounter, list.getCounter());
        }
        assertTrue(list.isEmpty());
        assertEquals(2, list.getCounter());
    }

    @Test
    public void testListIterator()
    {
        int expectedCounter = 0;
        NotifyOnUpdateList list = new NotifyOnUpdateList(new LinkedList<Integer>(Arrays.asList(8, 7)));
        assertEquals(expectedCounter, list.getCounter());

        ListIterator<Integer> listIterator = list.listIterator();
        listIterator.next();
        listIterator.set(9);
        assertEquals(++expectedCounter, list.getCounter());

        listIterator.next();
        listIterator.add(10);
        assertEquals(++expectedCounter, list.getCounter());

        assertEquals(Arrays.asList(9, 7, 10), list);
    }
}
