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

import java.util.Map;
import com.google.common.collect.testing.MapInterfaceTest;

/**
 * Test Map interface of RightMap using guava test library.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class RightMapTest extends MapInterfaceTest<Right, Object>
{
    private Map<Right, Object> populatedMap;
    private Object valueNotInPopulatedMaplatedMap;

    public RightMapTest()
    {
        super(false, true, true, true, true, true);
    }

    @Override
    protected Map<Right, Object> makeEmptyMap()
    {
        return new RightMap<Object>();
    }

    private Object getNewValue() {
        return new Object() {
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

    @Override
    protected Map<Right, Object> makePopulatedMap()
    {
        if (populatedMap == null) {
            populatedMap = new RightMap<Object>();
            populatedMap.put(Right.VIEW, getNewValue());
            populatedMap.put(Right.EDIT, getNewValue());
            populatedMap.put(Right.COMMENT, getNewValue());
            populatedMap.put(Right.DELETE, getNewValue());
            populatedMap.put(Right.ADMIN, getNewValue());
        }
        Map<Right, Object> newMap = new RightMap<Object>();
        for (Map.Entry<Right, Object> entry : populatedMap.entrySet()) {
            newMap.put(entry.getKey(), entry.getValue());
        }
        return newMap;
    }

    @Override
    protected Right getKeyNotInPopulatedMap()
    {
        return Right.PROGRAM;
    }

    @Override
    protected Object getValueNotInPopulatedMap()
    {
        if (valueNotInPopulatedMaplatedMap == null) {
            valueNotInPopulatedMaplatedMap = getNewValue();
        }
        return valueNotInPopulatedMaplatedMap;
    }

    public void testEqualsForLargerMap() {
        if (!supportsPut) {
            return;
        }

        final Map<Right, Object> map;
        final Map<Right, Object> largerMap;
        try {
            map = makePopulatedMap();
            largerMap = makePopulatedMap();
            largerMap.put(getKeyNotInPopulatedMap(), getValueNotInPopulatedMap());
        } catch (UnsupportedOperationException e) {
            return;
        }

        assertFalse(map.equals(largerMap));
    }
}
