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
import org.xwiki.eventstream.query.CompareQueryCondition;
import org.xwiki.eventstream.query.CompareQueryCondition.CompareType;
import org.xwiki.eventstream.query.GroupQueryCondition;
import org.xwiki.eventstream.query.QueryCondition;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.eventstream.query.SortableEventQuery.SortClause;
import org.xwiki.eventstream.query.SortableEventQuery.SortClause.Order;
import org.xwiki.eventstream.query.StatusQueryCondition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link SimpleEventQuery}.
 * 
 * @version $Id$
 */
class SimpleEventQueryTest
{
    @Test
    void less()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.less("property", "value");

        assertEquals(1, query.getConditions().size());
        assertEquals("property", ((CompareQueryCondition) query.getConditions().get(0)).getProperty());
        assertEquals("value", ((CompareQueryCondition) query.getConditions().get(0)).getValue());
        assertEquals(CompareType.LESS, ((CompareQueryCondition) query.getConditions().get(0)).getType());
    }

    @Test
    void lessOrEq()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.lessOrEq("property", "value");

        assertEquals(1, query.getConditions().size());
        assertEquals("property", ((CompareQueryCondition) query.getConditions().get(0)).getProperty());
        assertEquals("value", ((CompareQueryCondition) query.getConditions().get(0)).getValue());
        assertEquals(CompareType.LESS_OR_EQUALS, ((CompareQueryCondition) query.getConditions().get(0)).getType());
    }

    @Test
    void greater()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.greater("property", "value");

        assertEquals(1, query.getConditions().size());
        assertEquals("property", ((CompareQueryCondition) query.getConditions().get(0)).getProperty());
        assertFalse(((CompareQueryCondition) query.getConditions().get(0)).isParameter());
        assertEquals("value", ((CompareQueryCondition) query.getConditions().get(0)).getValue());
        assertEquals(CompareType.GREATER, ((CompareQueryCondition) query.getConditions().get(0)).getType());
    }

    @Test
    void greaterOrEq()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.greaterOrEq("property", "value");

        assertEquals(1, query.getConditions().size());
        assertEquals("property", ((CompareQueryCondition) query.getConditions().get(0)).getProperty());
        assertEquals("value", ((CompareQueryCondition) query.getConditions().get(0)).getValue());
        assertEquals(CompareType.GREATER_OR_EQUALS, ((CompareQueryCondition) query.getConditions().get(0)).getType());
    }

    @Test
    void startsWith()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.startsWith("property", "value");

        assertEquals(1, query.getConditions().size());
        assertEquals("property", ((CompareQueryCondition) query.getConditions().get(0)).getProperty());
        assertFalse(((CompareQueryCondition) query.getConditions().get(0)).isParameter());
        assertEquals("value", ((CompareQueryCondition) query.getConditions().get(0)).getValue());
        assertEquals(CompareType.STARTS_WITH, ((CompareQueryCondition) query.getConditions().get(0)).getType());
    }

    @Test
    void endsWith()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.endsWith("property", "value");

        assertEquals(1, query.getConditions().size());
        assertEquals("property", ((CompareQueryCondition) query.getConditions().get(0)).getProperty());
        assertFalse(((CompareQueryCondition) query.getConditions().get(0)).isParameter());
        assertEquals("value", ((CompareQueryCondition) query.getConditions().get(0)).getValue());
        assertEquals(CompareType.ENDS_WITH, ((CompareQueryCondition) query.getConditions().get(0)).getType());
    }

    @Test
    void contains()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.contains("property", "value");

        assertEquals(1, query.getConditions().size());
        assertEquals("property", ((CompareQueryCondition) query.getConditions().get(0)).getProperty());
        assertFalse(((CompareQueryCondition) query.getConditions().get(0)).isCustom());
        assertEquals("value", ((CompareQueryCondition) query.getConditions().get(0)).getValue());
        assertEquals(CompareType.CONTAINS, ((CompareQueryCondition) query.getConditions().get(0)).getType());
    }

    @Test
    void after()
    {
        Date date = new Date();

        SimpleEventQuery query = new SimpleEventQuery();

        query.after(date);

        assertEquals(1, query.getConditions().size());
        assertEquals(Event.FIELD_DATE, ((CompareQueryCondition) query.getConditions().get(0)).getProperty());
        assertSame(date, ((CompareQueryCondition) query.getConditions().get(0)).getValue());
        assertEquals(CompareType.GREATER, ((CompareQueryCondition) query.getConditions().get(0)).getType());
    }

    @Test
    void before()
    {
        Date date = new Date();

        SimpleEventQuery query = new SimpleEventQuery();

        query.before(date);

        assertEquals(1, query.getConditions().size());
        assertEquals(Event.FIELD_DATE, ((CompareQueryCondition) query.getConditions().get(0)).getProperty());
        assertSame(date, ((CompareQueryCondition) query.getConditions().get(0)).getValue());
        assertEquals(CompareType.LESS, ((CompareQueryCondition) query.getConditions().get(0)).getType());
    }

    @Test
    void status()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.withStatus(true);

        assertEquals(1, query.getConditions().size());
        assertEquals(new StatusQueryCondition(null, true, false), query.getConditions().get(0));

        query.withStatus("entity");

        assertEquals(2, query.getConditions().size());
        assertEquals(new StatusQueryCondition(null, true, false), query.getConditions().get(0));
        assertEquals(new StatusQueryCondition("entity", null, false), query.getConditions().get(1));

        query.withStatus("entity2", true);

        assertEquals(3, query.getConditions().size());
        assertEquals(new StatusQueryCondition(null, true, false), query.getConditions().get(0));
        assertEquals(new StatusQueryCondition("entity", null, false), query.getConditions().get(1));
        assertEquals(new StatusQueryCondition("entity2", true, false), query.getConditions().get(2));
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

        List<QueryCondition> conditions = query.getConditions();

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

    @Test
    void openClose()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.open();

        assertEquals(1, query.getConditions().size());

        query.close();

        assertTrue(query.getConditions().isEmpty());

        query.open();
        query.eq("property", "value");
        query.close();

        assertEquals(1, query.getConditions().size());

        GroupQueryCondition group = (GroupQueryCondition) query.getConditions().get(0);

        assertEquals(1, group.getConditions().size());
        assertEquals(new CompareQueryCondition("property", "value", CompareType.EQUALS), group.getConditions().get(0));

        query = new SimpleEventQuery();

        query.open();
        query.open();
        query.eq("property", "value");
        query.close();
        query.eq("property2", "value2");
        query.close();

        assertEquals(1, query.getConditions().size());

        GroupQueryCondition group1 = (GroupQueryCondition) query.getConditions().get(0);

        assertEquals(2, group1.getConditions().size());

        GroupQueryCondition group2 = (GroupQueryCondition) group1.getConditions().get(0);

        assertEquals(1, group2.getConditions().size());
        assertEquals(new CompareQueryCondition("property", "value", CompareType.EQUALS), group2.getConditions().get(0));

        assertEquals(new CompareQueryCondition("property2", "value2", CompareType.EQUALS),
            group1.getConditions().get(1));

        query = new SimpleEventQuery();

        query.not();
        query.open();
        query.eq("property1", "value1");
        query.eq("property2", "value2");
        query.close();

        assertEquals(1, query.getConditions().size());
        group = (GroupQueryCondition) query.getConditions().get(0);
        assertEquals(2, group.getConditions().size());
        assertEquals(new CompareQueryCondition("property1", "value1", CompareType.EQUALS),
            group.getConditions().get(0));
        assertEquals(new CompareQueryCondition("property2", "value2", CompareType.EQUALS),
            group.getConditions().get(1));
    }

    @Test
    void or()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.or();

        assertTrue(query.getConditions().isEmpty());

        query.eq("property", "value");

        assertEquals(1, query.getConditions().size());
        assertEquals(new CompareQueryCondition("property", "value", CompareType.EQUALS), query.getConditions().get(0));

        query.or();
        query.eq("property2", "value2");

        assertEquals(2, query.getConditions().size());
        assertTrue(query.isOr());
        assertEquals(new CompareQueryCondition("property", "value", CompareType.EQUALS), query.getConditions().get(0));
        assertEquals(new CompareQueryCondition("property2", "value2", CompareType.EQUALS),
            query.getConditions().get(1));

        query = new SimpleEventQuery();

        query.eq("property1", "value1");
        query.or();
        query.eq("property2", "value2");
        query.or();
        query.eq("property3", "value3");

        assertEquals(3, query.getConditions().size());
        assertTrue(query.isOr());
        assertEquals(new CompareQueryCondition("property1", "value1", CompareType.EQUALS),
            query.getConditions().get(0));
        assertEquals(new CompareQueryCondition("property2", "value2", CompareType.EQUALS),
            query.getConditions().get(1));
        assertEquals(new CompareQueryCondition("property3", "value3", CompareType.EQUALS),
            query.getConditions().get(2));

        query = new SimpleEventQuery();

        query.or();
        query.open();
        query.eq("property1", "value1");
        query.or();
        query.eq("property2", "value2");
        query.eq("property3", "value3");
        query.close();

        assertEquals(1, query.getConditions().size());
        GroupQueryCondition group1 = (GroupQueryCondition) query.getConditions().get(0);
        assertEquals(2, group1.getConditions().size());
        assertTrue(group1.isOr());
        assertEquals(new CompareQueryCondition("property1", "value1", CompareType.EQUALS),
            group1.getConditions().get(0));
        GroupQueryCondition group2 = (GroupQueryCondition) group1.getConditions().get(1);
        assertEquals(2, group2.getConditions().size());
        assertFalse(group2.isOr());
        assertEquals(new CompareQueryCondition("property2", "value2", CompareType.EQUALS),
            group2.getConditions().get(0));
        assertEquals(new CompareQueryCondition("property3", "value3", CompareType.EQUALS),
            group2.getConditions().get(1));

        query = new SimpleEventQuery();

        query.open();
        query.eq("property1", "value1");
        query.or();
        query.open();
        query.eq("property2", "value2");
        query.and();
        query.eq("property3", "value3");
        query.close();
        query.close();

        assertEquals(1, query.getConditions().size());
        group1 = (GroupQueryCondition) query.getConditions().get(0);
        assertEquals(2, group1.getConditions().size());
        assertTrue(group1.isOr());
        assertEquals(new CompareQueryCondition("property1", "value1", CompareType.EQUALS),
            group1.getConditions().get(0));
        group2 = (GroupQueryCondition) group1.getConditions().get(1);
        assertEquals(2, group2.getConditions().size());
        assertFalse(group2.isOr());
        assertEquals(new CompareQueryCondition("property2", "value2", CompareType.EQUALS),
            group2.getConditions().get(0));
        assertEquals(new CompareQueryCondition("property3", "value3", CompareType.EQUALS),
            group2.getConditions().get(1));

        query = new SimpleEventQuery();

        query.eq("property1", "value1");
        query.and();
        query.eq("property2", "value2");
        query.or();
        query.eq("property3", "value3");
        query.and();
        query.eq("property4", "value4");
        query.or();
        query.eq("property5", "value5");

        assertEquals(2, query.getConditions().size());
        assertEquals(new CompareQueryCondition("property1", "value1", CompareType.EQUALS),
            query.getConditions().get(0));
        group1 = (GroupQueryCondition) query.getConditions().get(1);
        assertTrue(group1.isOr());
        assertEquals(2, group1.getConditions().size());
        assertEquals(new CompareQueryCondition("property2", "value2", CompareType.EQUALS),
            group1.getConditions().get(0));
        group2 = (GroupQueryCondition) group1.getConditions().get(1);
        assertFalse(group2.isOr());
        assertEquals(2, group2.getConditions().size());
        assertEquals(new CompareQueryCondition("property3", "value3", CompareType.EQUALS),
            group2.getConditions().get(0));
        GroupQueryCondition group3 = (GroupQueryCondition) group2.getConditions().get(1);
        assertTrue(group3.isOr());
        assertEquals(2, group3.getConditions().size());
        assertEquals(new CompareQueryCondition("property4", "value4", CompareType.EQUALS),
            group3.getConditions().get(0));
        assertEquals(new CompareQueryCondition("property5", "value5", CompareType.EQUALS),
            group3.getConditions().get(1));

        query = new SimpleEventQuery();
        
        query.eq("property1", "value1");
        query.open();
        query.eq("property2", "value2");
        query.or();
        query.eq("property3", "value3");
        query.and();
        query.eq("property4", "value4");
        query.close();
        query.eq("property5", "value5");

        assertEquals(3, query.getConditions().size());
        assertEquals(new CompareQueryCondition("property1", "value1", CompareType.EQUALS),
            query.getConditions().get(0));
        group1 = (GroupQueryCondition) query.getConditions().get(1);
        assertTrue(group1.isOr());
        assertEquals(2, group1.getConditions().size());
        assertEquals(new CompareQueryCondition("property2", "value2", CompareType.EQUALS),
            group1.getConditions().get(0));
        group2 = (GroupQueryCondition) group1.getConditions().get(1);
        assertFalse(group2.isOr());
        assertEquals(2, group2.getConditions().size());
        assertEquals(new CompareQueryCondition("property3", "value3", CompareType.EQUALS),
            group2.getConditions().get(0));
        assertEquals(new CompareQueryCondition("property4", "value4", CompareType.EQUALS),
            group2.getConditions().get(1));
        assertEquals(new CompareQueryCondition("property5", "value5", CompareType.EQUALS),
            query.getConditions().get(2));

        query = new SimpleEventQuery();

        query.open();
        query.open();
        query.eq("property1", "value1");
        query.or();
        query.eq("property2", "value2");
        query.close();
        query.close();
        query.eq("property3", "value3");

        assertEquals(2, query.getConditions().size());
        assertFalse(query.isOr());
        group1 = (GroupQueryCondition) query.getConditions().get(0);
        assertTrue(group1.isOr());
        assertEquals(new CompareQueryCondition("property1", "value1", CompareType.EQUALS),
            group1.getConditions().get(0));
        assertEquals(new CompareQueryCondition("property2", "value2", CompareType.EQUALS),
            group1.getConditions().get(1));
        assertEquals(new CompareQueryCondition("property3", "value3", CompareType.EQUALS),
            query.getConditions().get(1));
    }

    @Test
    void equalsandhascode()
    {
        SimpleEventQuery query1 = new SimpleEventQuery();
        SimpleEventQuery query1bis = new SimpleEventQuery();

        SimpleEventQuery query2 = new SimpleEventQuery();
        query2.setLimit(42);
        SimpleEventQuery query2bis = new SimpleEventQuery();
        query2bis.setLimit(42);

        SimpleEventQuery query3 = new SimpleEventQuery();
        query3.setOffset(42);
        SimpleEventQuery query3bis = new SimpleEventQuery();
        query3bis.setOffset(42);

        SimpleEventQuery query4 = new SimpleEventQuery();
        query4.eq("prop", "value");
        SimpleEventQuery query4bis = new SimpleEventQuery();
        query4bis.eq("prop", "value");

        assertTrue(query1.equals(query1));
        assertTrue(query1bis.equals(query1bis));

        assertTrue(query2.equals(query2));
        assertTrue(query2bis.equals(query2bis));

        assertTrue(query3.equals(query3));
        assertTrue(query3bis.equals(query3bis));

        assertTrue(query4.equals(query4));
        assertTrue(query4bis.equals(query4bis));

        assertFalse(query1.equals(query2));
        assertFalse(query1.equals(query3));
        assertFalse(query1.equals(query4));
        assertFalse(query1.equals(null));
    }

    @Test
    void parameter()
    {
        SimpleEventQuery query = new SimpleEventQuery();

        query.parameter().greater("property", "value");

        assertEquals(1, query.getConditions().size());
        assertEquals("property", ((CompareQueryCondition) query.getConditions().get(0)).getProperty());
        assertTrue(((CompareQueryCondition) query.getConditions().get(0)).isParameter());
        assertEquals("value", ((CompareQueryCondition) query.getConditions().get(0)).getValue());
        assertEquals(CompareType.GREATER, ((CompareQueryCondition) query.getConditions().get(0)).getType());
    }
}
