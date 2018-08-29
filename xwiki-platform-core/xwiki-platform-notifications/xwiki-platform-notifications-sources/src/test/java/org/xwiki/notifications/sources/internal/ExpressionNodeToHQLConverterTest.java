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

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.filters.expression.BooleanValueNode;
import org.xwiki.notifications.filters.expression.DateValueNode;
import org.xwiki.notifications.filters.expression.EmptyNode;
import org.xwiki.notifications.filters.expression.EntityReferenceNode;
import org.xwiki.notifications.filters.expression.EqualsNode;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.GreaterThanNode;
import org.xwiki.notifications.filters.expression.InNode;
import org.xwiki.notifications.filters.expression.InSubQueryNode;
import org.xwiki.notifications.filters.expression.LesserThanNode;
import org.xwiki.notifications.filters.expression.NotEqualsNode;
import org.xwiki.notifications.filters.expression.NotNode;
import org.xwiki.notifications.filters.expression.PropertyValueNode;
import org.xwiki.notifications.filters.expression.StartsWith;
import org.xwiki.notifications.filters.expression.StringValueNode;
import org.xwiki.notifications.filters.expression.generics.AbstractNode;
import org.xwiki.notifications.filters.internal.status.InListOfReadEventsNode;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.text.StringUtils;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Unit tests for {@link ExpressionNodeToHQLConverter}.
 */
public class ExpressionNodeToHQLConverterTest
{
    @Rule
    public final MockitoComponentMockingRule<ExpressionNodeToHQLConverter> mocker =
            new MockitoComponentMockingRule<>(ExpressionNodeToHQLConverter.class);

    private static final String TEST_VALUE_1 = "aRandomStringValue";
    private static final String TEST_VALUE_1_IDENTIFIER = String.format("value_%s", sha256Hex(TEST_VALUE_1));

    private static final String TEST_VALUE_2 = "another_ random_string_ & value";
    private static final String TEST_VALUE_2_IDENTIFIER = String.format("value_%s", sha256Hex(TEST_VALUE_2));

    private ExpressionNodeToHQLConverter parser;
    private EntityReferenceSerializer<String> serializer;

    @Before
    public void setUp() throws Exception
    {
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        parser = mocker.getComponentUnderTest();
    }

    @Test
    public void parseWithEmptyNode()
    {
        assertEquals(StringUtils.EMPTY, parser.parse(new EmptyNode()).getQuery());
    }

    @Test
    public void parseWithPropertyValueNode()
    {
        // Check with a PropertyValueNode first
        assertEquals("event.type", parser.parse(new PropertyValueNode(EventProperty.TYPE)).getQuery());
    }

    @Test
    public void parseWithStringValueNode()
    {
        ExpressionNodeToHQLConverter.HQLQuery result = parser.parse(new StringValueNode(TEST_VALUE_1));
        assertEquals(String.format(":%s", TEST_VALUE_1_IDENTIFIER), result.getQuery());
        assertEquals(TEST_VALUE_1, result.getQueryParameters().get(TEST_VALUE_1_IDENTIFIER));
    }

    @Test
    public void parseWithNotNode()
    {
        AbstractNode testAST = new NotNode(new EqualsNode(new StringValueNode(TEST_VALUE_1),
                new StringValueNode(TEST_VALUE_2)));

        assertEquals(String.format(" NOT (:%s = :%s)", TEST_VALUE_1_IDENTIFIER, TEST_VALUE_2_IDENTIFIER),
                parser.parse(testAST).getQuery());
    }

    @Test
    public void parseWithEqualsNode()
    {
        AbstractNode testAST = new EqualsNode(new StringValueNode(TEST_VALUE_1), new StringValueNode(TEST_VALUE_2));

        assertEquals(String.format(":%s = :%s", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER), parser.parse(testAST).getQuery());
    }

    @Test
    public void parseWithNotEqualsNode()
    {
        AbstractNode testAST = new NotEqualsNode(new StringValueNode(TEST_VALUE_1), new StringValueNode(TEST_VALUE_2));

        assertEquals(String.format(":%s <> :%s", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER), parser.parse(testAST).getQuery());
    }

    @Test
    public void parseWithOrNode()
    {
        AbstractNode testAST = value(TEST_VALUE_1).eq(value(TEST_VALUE_2))
                .or(value(TEST_VALUE_1).eq(value(TEST_VALUE_2)));

        assertEquals(String.format("(:%s = :%s) OR (:%s = :%s)", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER, TEST_VALUE_1_IDENTIFIER, TEST_VALUE_2_IDENTIFIER),
                parser.parse(testAST).getQuery());
    }

    @Test
    public void parseWithAndNode()
    {
        AbstractNode testAST = value(TEST_VALUE_1).eq(value(TEST_VALUE_2))
                .and(value(TEST_VALUE_1).eq(value(TEST_VALUE_2)));

        assertEquals(String.format("(:%s = :%s) AND (:%s = :%s)", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER, TEST_VALUE_1_IDENTIFIER, TEST_VALUE_2_IDENTIFIER),
                parser.parse(testAST).getQuery());
    }

    @Test
    public void parseWithStartsWithNode()
    {
        AbstractNode testAST = new StartsWith(new StringValueNode(TEST_VALUE_1), new StringValueNode(TEST_VALUE_2));

        assertEquals(String.format(":%s LIKE concat(:%s, '%%') ESCAPE '!'", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER), parser.parse(testAST).getQuery());
    }

    @Test
    public void parseEntityReferenceNode()
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "Main", "WebHome");
        when(serializer.serialize(documentReference)).thenReturn("xwiki:Main.WebHome");

        AbstractNode testAST = new EntityReferenceNode(documentReference);

        ExpressionNodeToHQLConverter.HQLQuery result = parser.parse(testAST);

        assertEquals(":entity_e9f8294b0de086574bed923c45695bc8afc27d3ede9c35ed44db8d2100929de4",
                result.getQuery());
        assertEquals("xwiki:Main.WebHome",
                result.getQueryParameters().get(
                        "entity_e9f8294b0de086574bed923c45695bc8afc27d3ede9c35ed44db8d2100929de4"));
    }

    @Test
    public void parseWithBooleanValueNode()
    {
        assertEquals("true", parser.parse(new BooleanValueNode(true)).getQuery());
        assertEquals("false", parser.parse(new BooleanValueNode(false)).getQuery());
    }

    @Test
    public void parseDateNode()
    {
        Date date = new Date(0);

        AbstractNode testAST = new DateValueNode(date);

        ExpressionNodeToHQLConverter.HQLQuery result = parser.parse(testAST);

        assertEquals(":date_688218ea2b05763819a1e155109e4bf1e8921dd72e8b43d4c89c89133d4a5357",
                result.getQuery());
        assertEquals(date,
                result.getQueryParameters().get(
                        "date_688218ea2b05763819a1e155109e4bf1e8921dd72e8b43d4c89c89133d4a5357"));
    }

    @Test
    public void parseWithGreaterThanNode()
    {
        AbstractNode testAST = new GreaterThanNode(new PropertyValueNode(EventProperty.DATE),
                new StringValueNode(TEST_VALUE_1));

        ExpressionNodeToHQLConverter.HQLQuery result = parser.parse(testAST);

        assertEquals(String.format("event.date >= :%s", TEST_VALUE_1_IDENTIFIER),
                result.getQuery());
        assertEquals(TEST_VALUE_1,
                result.getQueryParameters().get(TEST_VALUE_1_IDENTIFIER));
    }

    @Test
    public void parseWithInNode()
    {
        AbstractNode testAST = new InNode(new PropertyValueNode(EventProperty.PAGE),
                Arrays.asList(new StringValueNode(TEST_VALUE_1), new StringValueNode(TEST_VALUE_2)));

        ExpressionNodeToHQLConverter.HQLQuery result = parser.parse(testAST);

        assertEquals(String.format("event.page IN (:%s, :%s)", TEST_VALUE_1_IDENTIFIER, TEST_VALUE_2_IDENTIFIER),
                result.getQuery());
        assertEquals(TEST_VALUE_1,
                result.getQueryParameters().get(TEST_VALUE_1_IDENTIFIER));
        assertEquals(TEST_VALUE_2,
                result.getQueryParameters().get(TEST_VALUE_2_IDENTIFIER));
    }

    @Test
    public void parseWithLesserThanNode()
    {
        AbstractNode testAST = new LesserThanNode(new PropertyValueNode(EventProperty.DATE),
                new StringValueNode(TEST_VALUE_1));

        ExpressionNodeToHQLConverter.HQLQuery result = parser.parse(testAST);

        assertEquals(String.format("event.date <= :%s", TEST_VALUE_1_IDENTIFIER),
                result.getQuery());
        assertEquals(TEST_VALUE_1,
                result.getQueryParameters().get(TEST_VALUE_1_IDENTIFIER));
    }

    @Test
    public void parseWithOrderBy()
    {
        AbstractNode testAST = new OrderByNode(
                new EqualsNode(
                        new PropertyValueNode(EventProperty.SPACE),
                        new PropertyValueNode(EventProperty.PAGE)
                ),
                new PropertyValueNode(EventProperty.DATE),
                OrderByNode.Order.ASC
        );

        ExpressionNodeToHQLConverter.HQLQuery result = parser.parse(testAST);

        assertEquals("event.space = event.page ORDER BY event.date ASC",
                result.getQuery());

    }

    @Test
    public void parseWithInListOfReadEventsNode()
    {
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "userA");

        when(serializer.serialize(user)).thenReturn("xwiki:XWiki.UserA");

        AbstractNode testAST = new NotNode(
                new InListOfReadEventsNode(user)
        );

        ExpressionNodeToHQLConverter.HQLQuery result = parser.parse(testAST);

        assertEquals(" NOT (" +
                        "event IN (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :userStatusRead " +
                        "and status.read = true))",
                result.getQuery());
        assertEquals("xwiki:XWiki.UserA", result.getQueryParameters().get("userStatusRead"));
    }

    @Test
    public void parseWithInSubQueryNode()
    {
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "userA");

        when(serializer.serialize(user)).thenReturn("xwiki:XWiki.UserA");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 12345);
        AbstractNode testAST = new NotNode(
                new InSubQueryNode(value(EventProperty.GROUP_ID),
                        "select fb.name in FooBar fb where fb.id = :id", parameters)
        );

        ExpressionNodeToHQLConverter.HQLQuery result = parser.parse(testAST);

        assertEquals(" NOT (event.requestId IN (select fb.name in FooBar fb where fb.id = :id))",
                result.getQuery());
        assertEquals(12345, result.getQueryParameters().get("id"));
    }

    @Test
    public void parseWithConcatNode()
    {
        AbstractNode testAST = value(TEST_VALUE_1).concat(value(TEST_VALUE_2)).concat(value(TEST_VALUE_2));
        ExpressionNodeToHQLConverter.HQLQuery result = parser.parse(testAST);
        assertEquals(String.format("CONCAT(CONCAT(:%s, :%s), :%s)", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER, TEST_VALUE_2_IDENTIFIER), result.getQuery());
    }
}
