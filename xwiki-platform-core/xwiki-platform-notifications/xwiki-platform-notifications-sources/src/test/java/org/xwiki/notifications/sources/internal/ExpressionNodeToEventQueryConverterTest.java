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
package org.xwiki.notifications.sources.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.query.CompareQueryCondition;
import org.xwiki.eventstream.query.CompareQueryCondition.CompareType;
import org.xwiki.eventstream.query.GroupQueryCondition;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.notifications.filters.expression.AndNode;
import org.xwiki.notifications.filters.expression.EqualsNode;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.GreaterThanNode;
import org.xwiki.notifications.filters.expression.LesserThanNode;
import org.xwiki.notifications.filters.expression.OrNode;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Validate {@link ExpressionNodeToEventQueryConverter}.
 * 
 * @version $Id$
 */
@ComponentTest
class ExpressionNodeToEventQueryConverterTest
{
    @InjectMockComponents
    private ExpressionNodeToEventQueryConverter converter;

    @Test
    void orandMix() throws EventStreamException
    {
        EqualsNode equalsNode = new EqualsNode(value(EventProperty.TYPE), value("typevalue"));
        GreaterThanNode greaterthanNode =
            new GreaterThanNode(value(EventProperty.APPLICATION), value("applicationvalue"));
        AndNode andnode = new AndNode(equalsNode, greaterthanNode);

        SimpleEventQuery query = this.converter.parse(andnode);

        assertEquals(2, query.getConditions().size());
        assertEquals(new CompareQueryCondition(Event.FIELD_TYPE, "typevalue", CompareType.EQUALS),
            query.getConditions().get(0));
        assertEquals(
            new CompareQueryCondition(Event.FIELD_APPLICATION, "applicationvalue", CompareType.GREATER_OR_EQUALS),
            query.getConditions().get(1));

        LesserThanNode lesserNode = new LesserThanNode(value(EventProperty.TITLE), value("titlevalue"));
        OrNode ornode = new OrNode(lesserNode, andnode);

        query = this.converter.parse(ornode);

        assertEquals(1, query.getConditions().size());
        GroupQueryCondition group1 = (GroupQueryCondition) query.getConditions().get(0);
        assertEquals(2, group1.getConditions().size());
        assertTrue(group1.isOr());
        assertEquals(new CompareQueryCondition(Event.FIELD_TITLE, "titlevalue", CompareType.LESS_OR_EQUALS),
            group1.getConditions().get(0));
        GroupQueryCondition group12 = (GroupQueryCondition) group1.getConditions().get(1);
        assertEquals(2, group12.getConditions().size());
        assertFalse(group12.isOr());
        assertEquals(new CompareQueryCondition(Event.FIELD_TYPE, "typevalue", CompareType.EQUALS),
            group12.getConditions().get(0));
        assertEquals(
            new CompareQueryCondition(Event.FIELD_APPLICATION, "applicationvalue", CompareType.GREATER_OR_EQUALS),
            group12.getConditions().get(1));
    }
}
