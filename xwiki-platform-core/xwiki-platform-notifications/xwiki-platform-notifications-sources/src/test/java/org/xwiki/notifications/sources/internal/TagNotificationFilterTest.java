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
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Test for {@link TagNotificationFilter}.
 *
 * @version $Id$
 */
@ComponentTest
public class TagNotificationFilterTest
{
    @InjectMockComponents
    private TagNotificationFilter tagNotificationFilter;

    @MockComponent
    private QueryManager queryManager;

    @Test
    public void filterEvent()
    {
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
            tagNotificationFilter.filterEvent(null, null, null, null));
    }

    @Test
    public void matchesPreference()
    {
        assertFalse(tagNotificationFilter.matchesPreference(null));
    }

    @Test
    public void filterExpressionWithNotificationPreference()
    {
        assertNull(tagNotificationFilter.filterExpression(null, null, null));
    }

    @Test
    public void filterExpressionWithType() throws QueryException
    {
        assertNull(tagNotificationFilter.filterExpression(null, null,
            NotificationFilterType.INCLUSIVE,
            NotificationFormat.ALERT));

        assertNull(tagNotificationFilter.filterExpression(null,
            Collections.emptyList(),
            NotificationFilterType.INCLUSIVE, null));

        List<NotificationFilterPreference> preferenceList = Arrays.asList(
            new TagNotificationFilterPreference("foo", "mywiki"),
            new TagNotificationFilterPreference("bar", "otherwiki"),
            new TagNotificationFilterPreference("baz", "mywiki")
        );

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(any(), eq(Query.HQL))).thenReturn(query);

        List pagesHoldingTags = Arrays.asList("Page1", "Page2");
        when(query.execute()).thenReturn(pagesHoldingTags);

        ExpressionNode filter = value(EventProperty.PAGE).inStrings(pagesHoldingTags)
            .and(value(EventProperty.WIKI).eq(value("mywiki")));

        assertEquals(filter,
            tagNotificationFilter.filterExpression(null, preferenceList, NotificationFilterType.EXCLUSIVE, null));

        verify(query).bindValue("tagList", Arrays.asList("foo", "bar", "baz"));
        verify(query).setWiki("mywiki");

        ExpressionNode emptyPages = value(EventProperty.PAGE).inStrings(Arrays.asList())
            .and(value(EventProperty.WIKI).eq(value("mywiki")));

        List pagesForUnusedTags = Collections.EMPTY_LIST;
        when(query.execute()).thenReturn(pagesForUnusedTags);
        assertEquals(emptyPages, tagNotificationFilter.filterExpression(null,
            preferenceList, NotificationFilterType.EXCLUSIVE, null));
    }
}
