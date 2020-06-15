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

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.SimpleEventQuery.CompareQueryCondition.CompareType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
