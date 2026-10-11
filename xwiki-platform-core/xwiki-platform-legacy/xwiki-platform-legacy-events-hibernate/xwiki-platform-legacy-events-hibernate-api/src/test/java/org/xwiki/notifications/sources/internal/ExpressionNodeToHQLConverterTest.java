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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.text.StringUtils;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Unit tests for {@link ExpressionNodeToHQLConverter}.
 */
@ComponentTest
class ExpressionNodeToHQLConverterTest
{
    @InjectMockComponents
    private ExpressionNodeToHQLConverter parser;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    private static final String TEST_VALUE_1 = "aRandomStringValue";
    private static final String TEST_VALUE_1_IDENTIFIER = String.format("value_%s", sha256Hex(TEST_VALUE_1));

    private static final String TEST_VALUE_2 = "another_ random_string_ & value";
    private static final String TEST_VALUE_2_IDENTIFIER = String.format("value_%s", sha256Hex(TEST_VALUE_2));

    @Test
    void parseWithEmptyNode()
    {
        assertEquals(StringUtils.EMPTY, this.parser.parse(new EmptyNode()).getQuery());
    }

    @Test
    void parseWithPropertyValueNode()
    {
        assertEquals("event.type", this.parser.parse(new PropertyValueNode(EventProperty.TYPE)).getQuery());
    }

    @Test
    void parseWithStringValueNode()
    {
        ExpressionNodeToHQLConverter.HQLQuery result = this.parser.parse(new StringValueNode(TEST_VALUE_1));
        assertEquals(String.format(":%s", TEST_VALUE_1_IDENTIFIER), result.getQuery());
        assertEquals(TEST_VALUE_1, result.getQueryParameters().get(TEST_VALUE_1_IDENTIFIER));
    }

    @Test
    void parseWithNotNode()
    {
        AbstractNode testAST = new NotNode(new EqualsNode(new StringValueNode(TEST_VALUE_1),
                new StringValueNode(TEST_VALUE_2)));

        assertEquals(String.format(" NOT (:%s = :%s)", TEST_VALUE_1_IDENTIFIER, TEST_VALUE_2_IDENTIFIER),
                this.parser.parse(testAST).getQuery());
    }

    @Test
    void parseWithEqualsNode()
    {
        AbstractNode testAST = new EqualsNode(new StringValueNode(TEST_VALUE_1), new StringValueNode(TEST_VALUE_2));

        assertEquals(String.format(":%s = :%s", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER), this.parser.parse(testAST).getQuery());
    }

    @Test
    void parseWithNotEqualsNode()
    {
        AbstractNode testAST = new NotEqualsNode(new StringValueNode(TEST_VALUE_1), new StringValueNode(TEST_VALUE_2));

        assertEquals(String.format(":%s <> :%s", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER), this.parser.parse(testAST).getQuery());
    }

    @Test
    void parseWithOrNode()
    {
        AbstractNode testAST = value(TEST_VALUE_1).eq(value(TEST_VALUE_2))
                .or(value(TEST_VALUE_1).eq(value(TEST_VALUE_2)));

        assertEquals(String.format("(:%s = :%s) OR (:%s = :%s)", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER, TEST_VALUE_1_IDENTIFIER, TEST_VALUE_2_IDENTIFIER),
                this.parser.parse(testAST).getQuery());
    }

    @Test
    void parseWithAndNode()
    {
        AbstractNode testAST = value(TEST_VALUE_1).eq(value(TEST_VALUE_2))
                .and(value(TEST_VALUE_1).eq(value(TEST_VALUE_2)));

        assertEquals(String.format("(:%s = :%s) AND (:%s = :%s)", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER, TEST_VALUE_1_IDENTIFIER, TEST_VALUE_2_IDENTIFIER),
                this.parser.parse(testAST).getQuery());
    }

    @Test
    void parseWithStartsWithNode()
    {
        AbstractNode testAST = new StartsWith(new StringValueNode(TEST_VALUE_1), new StringValueNode(TEST_VALUE_2));

        assertEquals(String.format(":%s LIKE concat(:%s, '%%') ESCAPE '!'", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER), this.parser.parse(testAST).getQuery());
    }

    @Test
    void parseEntityReferenceNode()
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "Main", "WebHome");
        when(this.serializer.serialize(documentReference)).thenReturn("xwiki:Main.WebHome");

        AbstractNode testAST = new EntityReferenceNode(documentReference);

        ExpressionNodeToHQLConverter.HQLQuery result = this.parser.parse(testAST);

        assertEquals(":entity_e9f8294b0de086574bed923c45695bc8afc27d3ede9c35ed44db8d2100929de4",
                result.getQuery());
        assertEquals("xwiki:Main.WebHome",
                result.getQueryParameters().get(
                        "entity_e9f8294b0de086574bed923c45695bc8afc27d3ede9c35ed44db8d2100929de4"));
    }

    @Test
    void parseWithBooleanValueNode()
    {
        assertEquals("true", this.parser.parse(new BooleanValueNode(true)).getQuery());
        assertEquals("false", this.parser.parse(new BooleanValueNode(false)).getQuery());
    }

    @Test
    void parseDateNode()
    {
        Date date = new Date(0);

        AbstractNode testAST = new DateValueNode(date);

        ExpressionNodeToHQLConverter.HQLQuery result = this.parser.parse(testAST);

        assertEquals(":date_" + DigestUtils.sha256Hex(date.toString()),
                result.getQuery());
        assertEquals(date,
                result.getQueryParameters().get(
                        "date_" + DigestUtils.sha256Hex(date.toString())));
    }

    @Test
    void parseWithGreaterThanNode()
    {
        AbstractNode testAST = new GreaterThanNode(new PropertyValueNode(EventProperty.DATE),
                new StringValueNode(TEST_VALUE_1));

        ExpressionNodeToHQLConverter.HQLQuery result = this.parser.parse(testAST);

        assertEquals(String.format("event.date >= :%s", TEST_VALUE_1_IDENTIFIER),
                result.getQuery());
        assertEquals(TEST_VALUE_1,
                result.getQueryParameters().get(TEST_VALUE_1_IDENTIFIER));
    }

    @Test
    void parseWithInNode()
    {
        AbstractNode testAST = new InNode(new PropertyValueNode(EventProperty.PAGE),
                List.of(new StringValueNode(TEST_VALUE_1), new StringValueNode(TEST_VALUE_2)));

        ExpressionNodeToHQLConverter.HQLQuery result = this.parser.parse(testAST);

        assertEquals(String.format("event.page IN (:%s, :%s)", TEST_VALUE_1_IDENTIFIER, TEST_VALUE_2_IDENTIFIER),
                result.getQuery());
        assertEquals(TEST_VALUE_1,
                result.getQueryParameters().get(TEST_VALUE_1_IDENTIFIER));
        assertEquals(TEST_VALUE_2,
                result.getQueryParameters().get(TEST_VALUE_2_IDENTIFIER));
    }

    @Test
    void parseWithLesserThanNode()
    {
        AbstractNode testAST = new LesserThanNode(new PropertyValueNode(EventProperty.DATE),
                new StringValueNode(TEST_VALUE_1));

        ExpressionNodeToHQLConverter.HQLQuery result = this.parser.parse(testAST);

        assertEquals(String.format("event.date <= :%s", TEST_VALUE_1_IDENTIFIER),
                result.getQuery());
        assertEquals(TEST_VALUE_1,
                result.getQueryParameters().get(TEST_VALUE_1_IDENTIFIER));
    }

    @Test
    void parseWithOrderBy()
    {
        AbstractNode testAST = new OrderByNode(
                new EqualsNode(
                        new PropertyValueNode(EventProperty.SPACE),
                        new PropertyValueNode(EventProperty.PAGE)
                ),
                new PropertyValueNode(EventProperty.DATE),
                OrderByNode.Order.ASC
        );

        ExpressionNodeToHQLConverter.HQLQuery result = this.parser.parse(testAST);

        assertEquals("event.space = event.page ORDER BY event.date ASC",
                result.getQuery());

    }

    @Test
    void parseWithInListOfReadEventsNode()
    {
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "userA");

        when(this.serializer.serialize(user)).thenReturn("xwiki:XWiki.UserA");

        AbstractNode testAST = new NotNode(
                new InListOfReadEventsNode(user)
        );

        ExpressionNodeToHQLConverter.HQLQuery result = this.parser.parse(testAST);

        assertEquals(" NOT (" +
                        "event IN (select status.activityEvent from LegacyEventStatus status " +
                        "where status.activityEvent = event and status.entityId = :userStatusRead " +
                        "and status.read = true))",
                result.getQuery());
        assertEquals("xwiki:XWiki.UserA", result.getQueryParameters().get("userStatusRead"));
    }

    @Test
    void parseWithInSubQueryNode()
    {
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "userA");

        when(this.serializer.serialize(user)).thenReturn("xwiki:XWiki.UserA");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 12345);
        AbstractNode testAST = new NotNode(
                new InSubQueryNode(value(EventProperty.GROUP_ID),
                        "select fb.name in FooBar fb where fb.id = :id", parameters)
        );

        ExpressionNodeToHQLConverter.HQLQuery result = this.parser.parse(testAST);

        assertEquals(" NOT (event.requestId IN (select fb.name in FooBar fb where fb.id = :id))",
                result.getQuery());
        assertEquals(12345, result.getQueryParameters().get("id"));
    }

    @Test
    void parseWithConcatNode()
    {
        AbstractNode testAST = value(TEST_VALUE_1).concat(value(TEST_VALUE_2)).concat(value(TEST_VALUE_2));
        ExpressionNodeToHQLConverter.HQLQuery result = this.parser.parse(testAST);
        assertEquals(String.format("CONCAT(CONCAT(:%s, :%s), :%s)", TEST_VALUE_1_IDENTIFIER,
                TEST_VALUE_2_IDENTIFIER, TEST_VALUE_2_IDENTIFIER), result.getQuery());
    }
}
