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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test Map interface of RightMap.
 *
 * @version $Id$
 * @since 4.0M2
 */
class RightMapTest
{
    private static final Right[] POPULATED_KEYS = {
        Right.VIEW, Right.EDIT, Right.COMMENT, Right.DELETE, Right.ADMIN
    };

    private RightMap<Object> emptyMap;

    private RightMap<Object> populatedMap;

    @BeforeEach
    void setUp()
    {
        emptyMap = new RightMap<>();
        populatedMap = new RightMap<>();
        for (Right key : POPULATED_KEYS) {
            populatedMap.put(key, newValue());
        }
    }

    private Object newValue()
    {
        return new Object()
        {
            @Override
            public int hashCode()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString()
            {
                return "[Object@" + Integer.toHexString(super.hashCode()) + "]";
            }
        };
    }

    @Test
    void sizeWhenEmpty()
    {
        assertEquals(0, emptyMap.size());
    }

    @Test
    void sizeWhenPopulated()
    {
        assertEquals(POPULATED_KEYS.length, populatedMap.size());
    }

    @Test
    void isEmptyWhenEmpty()
    {
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    void isEmptyWhenPopulated()
    {
        assertFalse(populatedMap.isEmpty());
    }

    @Test
    void containsKey()
    {
        for (Right key : POPULATED_KEYS) {
            assertTrue(populatedMap.containsKey(key));
        }
        assertFalse(populatedMap.containsKey(Right.PROGRAM));
    }

    @Test
    void containsKeyWithNonRightKey()
    {
        assertFalse(populatedMap.containsKey("not a right"));
    }

    @Test
    void containsValue()
    {
        for (Right key : POPULATED_KEYS) {
            assertTrue(populatedMap.containsValue(populatedMap.get(key)));
        }
    }

    @Test
    void get()
    {
        for (Right key : POPULATED_KEYS) {
            assertNotNull(populatedMap.get(key));
        }
        assertNull(populatedMap.get(Right.PROGRAM));
    }

    @Test
    void getWithNonRightKey()
    {
        assertNull(populatedMap.get("not a right"));
    }

    @Test
    void put()
    {
        Object value = new Object();
        assertNull(emptyMap.put(Right.VIEW, value));
        assertEquals(value, emptyMap.get(Right.VIEW));
        assertEquals(1, emptyMap.size());
    }

    @Test
    void putReturnsOldValue()
    {
        Object oldValue = populatedMap.get(Right.VIEW);
        Object newObject = new Object();
        assertEquals(oldValue, populatedMap.put(Right.VIEW, newObject));
        assertEquals(newObject, populatedMap.get(Right.VIEW));
    }

    @Test
    void putWithNullValue()
    {
        Object value = new Object();
        emptyMap.put(Right.VIEW, value);
        assertEquals(value, emptyMap.put(Right.VIEW, null));
        assertNull(emptyMap.get(Right.VIEW));
    }

    @Test
    void remove()
    {
        Object value = populatedMap.get(Right.VIEW);
        assertEquals(value, populatedMap.remove(Right.VIEW));
        assertFalse(populatedMap.containsKey(Right.VIEW));
        assertEquals(POPULATED_KEYS.length - 1, populatedMap.size());
    }

    @Test
    void removeNonExistentKey()
    {
        assertNull(populatedMap.remove(Right.PROGRAM));
        assertEquals(POPULATED_KEYS.length, populatedMap.size());
    }

    @Test
    void removeWithNonRightKey()
    {
        assertNull(populatedMap.remove("not a right"));
    }

    @Test
    void putAllFromRightMap()
    {
        RightMap<Object> other = new RightMap<>();
        Object viewValue = new Object();
        Object programValue = new Object();
        other.put(Right.VIEW, viewValue);
        other.put(Right.PROGRAM, programValue);

        populatedMap.putAll(other);

        assertEquals(viewValue, populatedMap.get(Right.VIEW));
        assertTrue(populatedMap.containsKey(Right.PROGRAM));
        assertEquals(2, populatedMap.size());
    }

    @Test
    void putAllFromRegularMap()
    {
        Map<Right, Object> other = new HashMap<>();
        other.put(Right.VIEW, new Object());
        other.put(Right.PROGRAM, new Object());

        int oldSize = populatedMap.size();
        populatedMap.putAll(other);

        assertTrue(populatedMap.containsKey(Right.PROGRAM));
        assertEquals(other.get(Right.VIEW), populatedMap.get(Right.VIEW));
        assertEquals(oldSize + 1, populatedMap.size());
    }

    @Test
    void clear()
    {
        populatedMap.clear();
        assertEquals(0, populatedMap.size());
        assertTrue(populatedMap.isEmpty());
        for (Right key : POPULATED_KEYS) {
            assertFalse(populatedMap.containsKey(key));
        }
    }

    @Test
    void keySet()
    {
        assertEquals(POPULATED_KEYS.length, populatedMap.keySet().size());
        for (Right key : POPULATED_KEYS) {
            assertTrue(populatedMap.keySet().contains(key));
        }
        assertFalse(populatedMap.keySet().contains(Right.PROGRAM));
    }

    @Test
    void keySetRemove()
    {
        populatedMap.keySet().remove(Right.VIEW);
        assertFalse(populatedMap.containsKey(Right.VIEW));
        assertEquals(POPULATED_KEYS.length - 1, populatedMap.size());
    }

    @Test
    void values()
    {
        assertEquals(POPULATED_KEYS.length, populatedMap.values().size());
        for (Right key : POPULATED_KEYS) {
            assertTrue(populatedMap.values().contains(populatedMap.get(key)));
        }
    }

    @Test
    void entrySet()
    {
        assertEquals(POPULATED_KEYS.length, populatedMap.entrySet().size());
        for (Map.Entry<Right, Object> entry : populatedMap.entrySet()) {
            assertTrue(populatedMap.containsKey(entry.getKey()));
            assertEquals(populatedMap.get(entry.getKey()), entry.getValue());
        }
    }

    @Test
    void entrySetContains()
    {
        for (Map.Entry<Right, Object> entry : populatedMap.entrySet()) {
            assertTrue(populatedMap.entrySet().contains(entry));
        }
    }

    @Test
    void entrySetRemove()
    {
        Map.Entry<Right, Object> entry = populatedMap.entrySet().iterator().next();
        populatedMap.entrySet().remove(entry);
        assertFalse(populatedMap.containsKey(entry.getKey()));
        assertEquals(POPULATED_KEYS.length - 1, populatedMap.size());
    }

    @Test
    void entrySetToArray()
    {
        Object[] array = populatedMap.entrySet().toArray();
        assertEquals(POPULATED_KEYS.length, array.length);
    }

    @Test
    void entrySetToTypedArray()
    {
        @SuppressWarnings("unchecked")
        Map.Entry<Right, Object>[] array = populatedMap.entrySet().toArray(new Map.Entry[0]);
        assertEquals(POPULATED_KEYS.length, array.length);
    }

    @Test
    void equalsWhenSame()
    {
        RightMap<Object> other = new RightMap<>();
        for (Right key : POPULATED_KEYS) {
            other.put(key, populatedMap.get(key));
        }
        assertEquals(populatedMap, other);
    }

    @Test
    void equalsWhenDifferent()
    {
        RightMap<Object> other = new RightMap<>();
        for (Right key : POPULATED_KEYS) {
            other.put(key, newValue());
        }
        assertFalse(populatedMap.equals(other));
    }

    @Test
    void equalsForLargerMap()
    {
        RightMap<Object> largerMap = new RightMap<>();
        for (Right key : POPULATED_KEYS) {
            largerMap.put(key, populatedMap.get(key));
        }
        largerMap.put(Right.PROGRAM, newValue());
        assertFalse(populatedMap.equals(largerMap));
    }

    @Test
    void equalsForSmallerMap()
    {
        RightMap<Object> smallerMap = new RightMap<>();
        for (int i = 0; i < POPULATED_KEYS.length - 1; i++) {
            smallerMap.put(POPULATED_KEYS[i], populatedMap.get(POPULATED_KEYS[i]));
        }
        assertFalse(populatedMap.equals(smallerMap));
    }

    @Test
    void hashCodeConsistency()
    {
        RightMap<String> map1 = new RightMap<>();
        RightMap<String> map2 = new RightMap<>();
        for (Right key : POPULATED_KEYS) {
            map1.put(key, key.getName());
            map2.put(key, key.getName());
        }
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    void iteratorTraversesAllKeys()
    {
        List<Right> keys = new ArrayList<>();
        populatedMap.keySet().forEach(keys::add);
        assertEquals(POPULATED_KEYS.length, keys.size());
        for (Right key : POPULATED_KEYS) {
            assertTrue(keys.contains(key));
        }
    }

    @Test
    void iteratorRemove()
    {
        var iterator = populatedMap.keySet().iterator();
        assertTrue(iterator.hasNext());
        Right key = iterator.next();
        iterator.remove();
        assertFalse(populatedMap.containsKey(key));
        assertEquals(POPULATED_KEYS.length - 1, populatedMap.size());
    }

    @Test
    void toStringWhenEmpty()
    {
        assertEquals("[]", emptyMap.toString());
    }

    @Test
    void toStringWhenPopulated()
    {
        String str = populatedMap.toString();
        assertTrue(str.startsWith("["));
        assertTrue(str.endsWith("]"));
        for (Right key : POPULATED_KEYS) {
            assertTrue(str.contains(key.getName()));
        }
    }
}
