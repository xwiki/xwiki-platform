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
package org.xwiki.eventstream;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.query.CompareQueryCondition;
import org.xwiki.eventstream.query.SimpleEventQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link SimpleEventQuery}.
 * 
 * @version $Id$
 */
public class EqualEventQueryTest
{
    @Test
    void conditions()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        assertTrue(query.getConditions().isEmpty());

        query.eq("key1", "value1");
        query.eq("key2", "value2");

        assertEquals(2, query.getConditions().size());

        assertEquals("key1", ((CompareQueryCondition) query.getConditions().get(0)).getProperty());
        assertEquals("value1", ((CompareQueryCondition) query.getConditions().get(0)).getValue());
        assertEquals("key2", ((CompareQueryCondition) query.getConditions().get(1)).getProperty());
        assertEquals("value2", ((CompareQueryCondition) query.getConditions().get(1)).getValue());

        SimpleEventQuery queryStatus = new SimpleEventQuery(0, 0)
            .eq(Event.FIELD_ID, "id1")
            .withStatus("bar");
        SimpleEventQuery queryMail = new SimpleEventQuery(0, 0)
            .eq(Event.FIELD_ID, "id1")
            .withMail("bar");
        assertNotEquals(queryMail, queryStatus);
    }

    @Test
    void offset()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        assertEquals(0, query.getOffset());

        query.setOffset(42);

        assertEquals(42, query.getOffset());
    }

    @Test
    void limit()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        assertEquals(-1, query.getLimit());

        query.setLimit(42);

        assertEquals(42, query.getLimit());
    }

    @Test
    void constructors()
    {
        SimpleEventQuery query = new SimpleEventQuery(42, 43).eq("key", "value");

        assertEquals(1, query.getConditions().size());
        assertEquals("key", ((CompareQueryCondition) query.getConditions().get(0)).getProperty());
        assertEquals("value", ((CompareQueryCondition) query.getConditions().get(0)).getValue());
        assertEquals(42, query.getOffset());
        assertEquals(43, query.getLimit());
    }
}
