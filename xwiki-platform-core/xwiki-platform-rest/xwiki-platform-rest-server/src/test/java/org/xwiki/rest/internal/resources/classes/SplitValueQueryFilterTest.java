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
package org.xwiki.rest.internal.resources.classes;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link SplitValueQueryFilter}.
 * 
 * @version $Id$
 * @since 10.6RC1
 */
public class SplitValueQueryFilterTest
{
    private SplitValueQueryFilter filter = new SplitValueQueryFilter(",|", 2, "a");

    @Test
    public void filterResults()
    {
        List<Object> results = Arrays.asList(new Object[] {"alice,|bob,", 3L}, new Object[] {",carol", 2L},
            new Object[] {"denis|carol|alice", 1L}, new Object[] {"Carol", 1L});

        List<Object> filteredResults = this.filter.filterResults(results);
        assertEquals(2, filteredResults.size());
        assertArrayEquals(new Object[] {"alice", 4L}, (Object[]) filteredResults.get(0));
        assertArrayEquals(new Object[] {"carol", 3L}, (Object[]) filteredResults.get(1));
    }
}
