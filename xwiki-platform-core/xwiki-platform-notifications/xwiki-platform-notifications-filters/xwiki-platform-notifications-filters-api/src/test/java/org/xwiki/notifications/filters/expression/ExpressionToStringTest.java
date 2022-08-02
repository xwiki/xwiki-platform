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
package org.xwiki.notifications.filters.expression;

import org.junit.jupiter.api.Test;
import org.xwiki.notifications.filters.expression.generics.AbstractNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @version $Id$
 */
public class ExpressionToStringTest
{
    @Test
    void test() throws Exception
    {
        AbstractNode node = new AndNode(
            new OrNode(new EqualsNode(new PropertyValueNode(EventProperty.WIKI), new StringValueNode("value1")),
                new NotEqualsNode(new PropertyValueNode(EventProperty.SPACE), new StringValueNode("value2"))),
            new NotNode(new StartsWith(new PropertyValueNode(EventProperty.PAGE), new StringValueNode("value3%"))));

        assertEquals("((WIKI = \"value1\" OR SPACE <> \"value2\") AND NOT (PAGE STARTS WITH \"value3%\"))",
            node.toString());
    }

    @Test
    void lesser()
    {
        assertEquals("WIKI <= \"value1\"",
            new LesserThanNode(new PropertyValueNode(EventProperty.WIKI), new StringValueNode("value1")).toString());
        assertEquals("WIKI <= \"value1\"",
            new LesserThanNode(new PropertyValueNode(EventProperty.WIKI), new StringValueNode("value1"), true)
                .toString());
        assertEquals("WIKI < \"value1\"",
            new LesserThanNode(new PropertyValueNode(EventProperty.WIKI), new StringValueNode("value1"), false)
                .toString());
    }

    @Test
    void greater()
    {
        assertEquals("WIKI >= \"value1\"",
            new GreaterThanNode(new PropertyValueNode(EventProperty.WIKI), new StringValueNode("value1")).toString());
        assertEquals("WIKI >= \"value1\"",
            new GreaterThanNode(new PropertyValueNode(EventProperty.WIKI), new StringValueNode("value1"), true)
                .toString());
        assertEquals("WIKI > \"value1\"",
            new GreaterThanNode(new PropertyValueNode(EventProperty.WIKI), new StringValueNode("value1"), false)
                .toString());
    }
}
