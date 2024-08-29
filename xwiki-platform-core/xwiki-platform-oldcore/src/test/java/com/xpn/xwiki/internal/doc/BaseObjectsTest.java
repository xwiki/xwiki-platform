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
package com.xpn.xwiki.internal.doc;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validate {@link BaseObjects}.
 * 
 * @version $Id$
 */
public class BaseObjectsTest
{
    private static final class TestBaseObject extends BaseObject
    {
        @Override
        public String toString()
        {
            return String.valueOf(getNumber());
        }
    }

    private static final BaseObject XOBJ1 = new TestBaseObject();

    private static final BaseObject XOBJ2 = new TestBaseObject();

    private static final BaseObject XOBJ3 = new TestBaseObject();

    private static final BaseObject XOBJ4 = new TestBaseObject();

    @Test
    void add()
    {
        BaseObjects objects = new BaseObjects();

        assertEquals(0, objects.size());

        objects.add(XOBJ1);

        assertEquals(1, objects.size());

        objects.add(null);

        assertEquals(2, objects.size());

        objects.add(XOBJ2);

        assertEquals(3, objects.size());
        assertSame(XOBJ1, objects.get(0));
        assertEquals(0, objects.get(0).getNumber());
        assertNull(objects.get(1));
        assertSame(XOBJ2, objects.get(2));
        assertEquals(2, objects.get(2).getNumber());

        objects.add(1, XOBJ3);

        assertEquals(4, objects.size());
        assertSame(XOBJ1, objects.get(0));
        assertEquals(0, objects.get(0).getNumber());
        assertSame(XOBJ3, objects.get(1));
        assertEquals(1, objects.get(1).getNumber());
        assertNull(objects.get(2));
        assertSame(XOBJ2, objects.get(3));
        assertEquals(3, objects.get(3).getNumber());

        assertThrows(IndexOutOfBoundsException.class, () -> objects.add(Integer.MAX_VALUE, XOBJ4));
    }

    @Test
    void set()
    {
        BaseObjects objects = new BaseObjects();

        assertEquals(0, objects.size());

        assertThrows(IndexOutOfBoundsException.class, () -> objects.set(0, XOBJ1));

        objects.add(XOBJ1);
        objects.add(XOBJ2);
        objects.add(XOBJ3);

        objects.set(1, XOBJ4);

        assertSame(XOBJ1, objects.get(0));
        assertSame(XOBJ4, objects.get(1));
        assertSame(XOBJ3, objects.get(2));
    }

    @Test
    void remove()
    {
        BaseObjects objects = new BaseObjects(Arrays.asList(XOBJ1, XOBJ2, XOBJ3));

        assertEquals(3, objects.size());

        objects.remove(0);

        assertEquals(2, objects.size());
        assertSame(XOBJ2, objects.get(0));
        assertSame(XOBJ3, objects.get(1));

        objects.remove(1);

        assertEquals(1, objects.size());
        assertSame(XOBJ2, objects.get(0));
    }

    @Test
    void clear()
    {
        BaseObjects objects = new BaseObjects(Arrays.asList(XOBJ1, XOBJ2, XOBJ3));

        assertEquals(3, objects.size());

        objects.clear();

        assertEquals(0, objects.size());
    }
}
