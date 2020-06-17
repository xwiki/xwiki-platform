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

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.SimpleEventQuery.CompareQueryCondition;
import org.xwiki.eventstream.SimpleEventQuery.CompareQueryCondition.CompareType;
import org.xwiki.eventstream.SortableEventQuery.SortClause;
import org.xwiki.eventstream.SortableEventQuery.SortClause.Order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link SimpleEventQuery}.
 * 
 * @version $Id$
 */
public class SimpleEventQueryTest
{
    @Test
    void less()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.less("property", "value");

        assertEquals(1, query.getConditions().size());
        assertEquals("property", query.getConditions().get(0).getProperty());
        assertEquals("value", query.getConditions().get(0).getValue());
        assertEquals(CompareType.LESS, query.getConditions().get(0).getType());
    }

    @Test
    void lessOrEq()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.lessOrEq("property", "value");

        assertEquals(1, query.getConditions().size());
        assertEquals("property", query.getConditions().get(0).getProperty());
        assertEquals("value", query.getConditions().get(0).getValue());
        assertEquals(CompareType.LESS_OR_EQUALS, query.getConditions().get(0).getType());
    }

    @Test
    void greater()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.greater("property", "value");

        assertEquals(1, query.getConditions().size());
        assertEquals("property", query.getConditions().get(0).getProperty());
        assertEquals("value", query.getConditions().get(0).getValue());
        assertEquals(CompareType.GREATER, query.getConditions().get(0).getType());
    }

    @Test
    void greaterOrEq()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.greaterOrEq("property", "value");

        assertEquals(1, query.getConditions().size());
        assertEquals("property", query.getConditions().get(0).getProperty());
        assertEquals("value", query.getConditions().get(0).getValue());
        assertEquals(CompareType.GREATER_OR_EQUALS, query.getConditions().get(0).getType());
    }

    @Test
    void after()
    {
        Date date = new Date();

        SimpleEventQuery query = new SimpleEventQuery();

        query.after(date);

        assertEquals(1, query.getConditions().size());
        assertEquals(Event.FIELD_DATE, query.getConditions().get(0).getProperty());
        assertSame(date, query.getConditions().get(0).getValue());
        assertEquals(CompareType.GREATER, query.getConditions().get(0).getType());
    }

    @Test
    void before()
    {
        Date date = new Date();

        SimpleEventQuery query = new SimpleEventQuery();

        query.before(date);

        assertEquals(1, query.getConditions().size());
        assertEquals(Event.FIELD_DATE, query.getConditions().get(0).getProperty());
        assertSame(date, query.getConditions().get(0).getValue());
        assertEquals(CompareType.LESS, query.getConditions().get(0).getType());
    }

    @Test
    void status()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        assertNull(query.getStatusRead());
        assertNull(query.getStatusEntityId());

        query.withStatus(true);

        assertTrue(query.getStatusRead());

        query.withStatus(false);

        assertFalse(query.getStatusRead());

        query.withStatus("entity");

        assertFalse(query.getStatusRead());
        assertEquals("entity", query.getStatusEntityId());

        query.withStatus("entity2", true);

        assertTrue(query.getStatusRead());
        assertEquals("entity2", query.getStatusEntityId());
    }

    @Test
    void equalsSortClause()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.addSort("property", Order.ASC);
        query.addSort("property", Order.DESC);
        query.addSort("property", Order.ASC);
        query.addSort("property2", Order.ASC);

        List<SortClause> sorts = query.getSorts();

        assertEquals(sorts.get(0), sorts.get(2));
        assertEquals(sorts.get(0).hashCode(), sorts.get(2).hashCode());

        assertNotEquals(sorts.get(0), sorts.get(1));
        assertNotEquals(sorts.get(0).hashCode(), sorts.get(1).hashCode());
        assertNotEquals(sorts.get(0), sorts.get(3));
        assertNotEquals(sorts.get(0).hashCode(), sorts.get(3).hashCode());
    }

    @Test
    void equalsCompareQueryCondition()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.eq("property", "value");
        query.eq("property", "value2");
        query.not().eq("property", "value");
        query.eq("property", "value");
        query.less("property", "value");
        query.eq("property2", "value");
        query.eq("property", "value2");

        List<CompareQueryCondition> conditions = query.getConditions();

        assertEquals(conditions.get(0), conditions.get(3));
        assertEquals(conditions.get(0).hashCode(), conditions.get(3).hashCode());

        assertNotEquals(conditions.get(0), conditions.get(1));
        assertNotEquals(conditions.get(0).hashCode(), conditions.get(1).hashCode());
        assertNotEquals(conditions.get(0), conditions.get(2));
        assertNotEquals(conditions.get(0).hashCode(), conditions.get(2).hashCode());
        assertNotEquals(conditions.get(0), conditions.get(4));
        assertNotEquals(conditions.get(0).hashCode(), conditions.get(4).hashCode());
        assertNotEquals(conditions.get(0), conditions.get(5));
        assertNotEquals(conditions.get(0).hashCode(), conditions.get(5).hashCode());
        assertNotEquals(conditions.get(0), conditions.get(6));
        assertNotEquals(conditions.get(0).hashCode(), conditions.get(6).hashCode());
    }
}
