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

import org.junit.Before;
import org.junit.Test;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.expression.AndNode;
import org.xwiki.notifications.filters.expression.EmptyNode;
import org.xwiki.notifications.filters.expression.EqualsNode;
import org.xwiki.notifications.filters.expression.LikeNode;
import org.xwiki.notifications.filters.expression.NotEqualsNode;
import org.xwiki.notifications.filters.expression.NotNode;
import org.xwiki.notifications.filters.expression.OrNode;
import org.xwiki.notifications.filters.expression.PropertyValueNode;
import org.xwiki.notifications.filters.expression.StringValueNode;
import org.xwiki.notifications.filters.expression.generics.AbstractNode;
import org.xwiki.text.StringUtils;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.junit.Assert.assertEquals;
import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Unit tests for {@link NFExpressionToHQLParser}.
 */
public class NFExpressionToHQLParserTest
{
    private static final String TEST_VALUE_1 = "aRandomStringValue";
    private static final String TEST_VALUE_1_IDENTIFIER = String.format("value_%s", sha256Hex(TEST_VALUE_1));

    private static final String TEST_VALUE_2 = "another_ random_string_ & value";
    private static final String TEST_VALUE_2_IDENTIFIER = String.format("value_%s", sha256Hex(TEST_VALUE_2));

    private NFExpressionToHQLParser parser;

    @Before
    public void setUp()
    {
        parser = new NFExpressionToHQLParser();
    }

    @Test
    public void parseWithEmptyNode()
    {
        assertEquals(StringUtils.EMPTY, (new NFExpressionToHQLParser()).parse(new EmptyNode()));
    }

    @Test
    public void parseWithPropertyValueNode()
    {
        // Check with a PropertyValueNode first
        assertEquals("event.eventType", (new NFExpressionToHQLParser()).parse(new PropertyValueNode(
                NotificationFilterProperty.EVENT_TYPE)));
    }

    @Test
    public void parseWithStringValueNode()
    {
        assertEquals(String.format(":%s", TEST_VALUE_1_IDENTIFIER), parser.parse(new StringValueNode(TEST_VALUE_1)));
        assertEquals(TEST_VALUE_1, parser.getQueryParameters().get(TEST_VALUE_1_IDENTIFIER));
    }

    @Test
    public void parseWithNotNode()
    {
        AbstractNode testAST = new NotNode(new EqualsNode(new StringValueNode(TEST_VALUE_1),
                new StringValueNode(TEST_VALUE_2)));

        assertEquals(String.format(" NOT (:%s = :%s)", TEST_VALUE_1_IDENTIFIER, TEST_VALUE_2_IDENTIFIER),
                parser.parse(testAST));
    }

    @Test
    public void parseWithEqualsNode()
    {
        AbstractNode testAST = new EqualsNode(new StringValueNode(TEST_VALUE_1), new StringValueNode(TEST_VALUE_2));

        assertEquals(String.format(":%s = :%s", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER), parser.parse(testAST));
    }

    @Test
    public void parseWithNotEqualsNode()
    {
        AbstractNode testAST = new NotEqualsNode(new StringValueNode(TEST_VALUE_1), new StringValueNode(TEST_VALUE_2));

        assertEquals(String.format(":%s <> :%s", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER), parser.parse(testAST));
    }

    @Test
    public void parseWithOrNode()
    {
        AbstractNode testAST = value(TEST_VALUE_1).eq(value(TEST_VALUE_2))
                .or(value(TEST_VALUE_1).eq(value(TEST_VALUE_2)));

        assertEquals(String.format("(:%s = :%s) OR (:%s = :%s)", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER, TEST_VALUE_1_IDENTIFIER, TEST_VALUE_2_IDENTIFIER), parser.parse(testAST));
    }

    @Test
    public void parseWithAndNode()
    {
        AbstractNode testAST = value(TEST_VALUE_1).eq(value(TEST_VALUE_2))
                .and(value(TEST_VALUE_1).eq(value(TEST_VALUE_2)));

        assertEquals(String.format("(:%s = :%s) AND (:%s = :%s)", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER, TEST_VALUE_1_IDENTIFIER, TEST_VALUE_2_IDENTIFIER), parser.parse(testAST));
    }

    @Test
    public void parseWithLikeNode()
    {
        AbstractNode testAST = new LikeNode(new StringValueNode(TEST_VALUE_1), new StringValueNode(TEST_VALUE_2));

        assertEquals(String.format(":%s LIKE :%s ESCAPE '!'", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER), parser.parse(testAST));
    }
}
