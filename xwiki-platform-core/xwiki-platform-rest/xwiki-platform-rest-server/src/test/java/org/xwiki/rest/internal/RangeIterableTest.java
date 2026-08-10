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
package org.xwiki.rest.internal;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validate {@link RangeIterable}.
 *
 * @version $Id$
 */
class RangeIterableTest
{
    private <T> List<T> collect(RangeIterable<T> range)
    {
        List<T> result = new ArrayList<>();
        range.forEach(result::add);
        return result;
    }

    @Test
    void iterateWindowWithinBounds()
    {
        assertEquals(List.of("b", "c"),
            collect(new RangeIterable<>(List.of("a", "b", "c", "d", "e"), 1, 2)));
    }

    @Test
    void negativeStartClampedToZero()
    {
        assertEquals(List.of("a", "b"),
            collect(new RangeIterable<>(List.of("a", "b", "c"), -5, 2)));
    }

    @Test
    void startBeyondSizeYieldsEmpty()
    {
        assertEquals(List.of(),
            collect(new RangeIterable<>(List.of("a", "b", "c"), 10, 2)));
    }

    @Test
    void negativeNumberMeansRemainderFromStart()
    {
        assertEquals(List.of("b", "c"),
            collect(new RangeIterable<>(List.of("a", "b", "c"), 1, -1)));
    }

    @Test
    void numberOverflowClampedToSize()
    {
        assertEquals(List.of("c"),
            collect(new RangeIterable<>(List.of("a", "b", "c"), 2, 10)));
    }

    @Test
    void emptyListYieldsEmpty()
    {
        assertEquals(List.of(), collect(new RangeIterable<>(List.of(), 0, 5)));
    }

    @Test
    void nextBeyondEndThrows()
    {
        // Documents current behaviour: next() is unguarded and relies on the caller checking
        // hasNext(). Exhausting the iterator and calling next() again reaches list.get() out of
        // range.
        var it = new RangeIterable<>(List.of("a"), 0, 1).iterator();
        it.next();
        assertFalse(it.hasNext());
        assertThrows(IndexOutOfBoundsException.class, it::next);
    }
}
