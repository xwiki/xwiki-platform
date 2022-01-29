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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.query.CompareQueryCondition;
import org.xwiki.eventstream.query.CompareQueryCondition.CompareType;
import org.xwiki.eventstream.query.GroupQueryCondition;
import org.xwiki.eventstream.query.InQueryCondition;
import org.xwiki.eventstream.query.MailEntityQueryCondition;
import org.xwiki.eventstream.query.QueryCondition;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.eventstream.query.SortableEventQuery.SortClause;
import org.xwiki.eventstream.query.SortableEventQuery.SortClause.Order;
import org.xwiki.eventstream.query.StatusQueryCondition;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.expression.EmptyNode;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.NotNode;
import org.xwiki.notifications.filters.internal.status.ForUserNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Validate {@link EventQueryGenerator} and {@link ExpressionNodeToEventQueryConverter}.
 * 
 * @version $Id$
 */
@ComponentList({DefaultStringEntityReferenceSerializer.class, DefaultSymbolScheme.class, QueryExpressionGenerator.class,
    ExpressionNodeToEventQueryConverter.class})
class EventQueryGeneratorTest extends AbstractQueryGeneratorTest
{
    @InjectMockComponents
    private EventQueryGenerator generator;

    @Test
    void generateQueryExpression() throws Exception
    {
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = USER_REFERENCE;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = this.startDate;
        parameters.preferences = Arrays.asList(this.pref1);
        parameters.filterPreferences = Arrays.asList(this.fakeFilterPreference);

        SimpleEventQuery query = this.generator.generateQuery(parameters);

        Iterator<QueryCondition> conditions = query.getConditions().iterator();

        assertEquals(new CompareQueryCondition(Event.FIELD_DATE, this.startDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_TYPE, "create", CompareType.EQUALS, false),
            conditions.next());
        assertEquals(
            new CompareQueryCondition(Event.FIELD_DATE, this.pref1StartDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_HIDDEN, true, CompareType.EQUALS, true), conditions.next());

        List<SortClause> sortClause = query.getSorts();

        assertEquals(new SortClause(Event.FIELD_DATE, Order.DESC), sortClause.get(0));
    }

    @Test
    void generateQueryWhenHiddenDocsAreEnabled() throws Exception
    {
        UserProperties userProperties = mock(UserProperties.class);
        when(userProperties.displayHiddenDocuments()).thenReturn(true);
        when(this.userPropertiesResolver.resolve(any(UserReference.class))).thenReturn(userProperties);

        NotificationParameters parameters = new NotificationParameters();
        parameters.user = USER_REFERENCE;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = this.startDate;
        parameters.preferences = Arrays.asList(this.pref1);
        parameters.filterPreferences = Arrays.asList(this.fakeFilterPreference);

        SimpleEventQuery query = this.generator.generateQuery(parameters);

        Iterator<QueryCondition> conditions = query.getConditions().iterator();

        assertEquals(new CompareQueryCondition(Event.FIELD_DATE, this.startDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_TYPE, "create", CompareType.EQUALS, false),
            conditions.next());
        assertEquals(
            new CompareQueryCondition(Event.FIELD_DATE, this.pref1StartDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());

        List<SortClause> sortClause = query.getSorts();

        assertEquals(new SortClause(Event.FIELD_DATE, Order.DESC), sortClause.get(0));
    }

    @Test
    void generateQueryWithUntilDate() throws Exception
    {
        Date untilDate = new Date(1000000000000L);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = USER_REFERENCE;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = this.startDate;
        parameters.endDate = untilDate;
        parameters.preferences = Arrays.asList(this.pref1);
        parameters.filterPreferences = Arrays.asList(this.fakeFilterPreference);

        SimpleEventQuery query = this.generator.generateQuery(parameters);

        Iterator<QueryCondition> conditions = query.getConditions().iterator();

        assertEquals(new CompareQueryCondition(Event.FIELD_DATE, this.startDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_TYPE, "create", CompareType.EQUALS, false),
            conditions.next());
        assertEquals(
            new CompareQueryCondition(Event.FIELD_DATE, this.pref1StartDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_DATE, untilDate, CompareType.LESS_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_HIDDEN, true, CompareType.EQUALS, true), conditions.next());

        List<SortClause> sortClause = query.getSorts();

        assertEquals(new SortClause(Event.FIELD_DATE, Order.DESC), sortClause.get(0));
    }

    @Test
    void generateQueryWithUntilDateAndBlackList() throws Exception
    {
        Date untilDate = new Date(1000000000000L);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = USER_REFERENCE;
        parameters.format = NotificationFormat.ALERT;
        parameters.endDate = untilDate;
        parameters.blackList = Arrays.asList("event1", "event2");
        parameters.preferences = Arrays.asList(pref1);
        parameters.filterPreferences = Arrays.asList(fakeFilterPreference);

        SimpleEventQuery query = this.generator.generateQuery(parameters);

        Iterator<QueryCondition> conditions = query.getConditions().iterator();

        assertEquals(new CompareQueryCondition(Event.FIELD_TYPE, "create", CompareType.EQUALS, false),
            conditions.next());
        assertEquals(
            new CompareQueryCondition(Event.FIELD_DATE, this.pref1StartDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new InQueryCondition(true, Event.FIELD_ID, Arrays.asList("event1", "event2")), conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_DATE, untilDate, CompareType.LESS_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_HIDDEN, true, CompareType.EQUALS, true), conditions.next());

        List<SortClause> sortClause = query.getSorts();

        assertEquals(new SortClause(Event.FIELD_DATE, Order.DESC), sortClause.get(0));
    }

    @Test
    void generateQueryWithLocalUser() throws Exception
    {
        // Test
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = USER_REFERENCE;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = this.startDate;
        parameters.preferences = Arrays.asList(this.pref1);
        parameters.filterPreferences = Arrays.asList(this.fakeFilterPreference);

        SimpleEventQuery query = this.generator.generateQuery(parameters);

        Iterator<QueryCondition> conditions = query.getConditions().iterator();

        assertEquals(new CompareQueryCondition(Event.FIELD_DATE, this.startDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_TYPE, "create", CompareType.EQUALS, false),
            conditions.next());
        assertEquals(
            new CompareQueryCondition(Event.FIELD_DATE, this.pref1StartDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_HIDDEN, true, CompareType.EQUALS, true), conditions.next());
        assertEquals(
            new CompareQueryCondition(Event.FIELD_WIKI, USER_REFERENCE.getWikiReference(), CompareType.EQUALS, false),
            conditions.next());

        List<SortClause> sortClause = query.getSorts();

        assertEquals(new SortClause(Event.FIELD_DATE, Order.DESC), sortClause.get(0));
    }

    @Test
    void generateQueryWithFilters() throws Exception
    {
        // Mocks
        NotificationFilter notificationFilter1 = mock(NotificationFilter.class);
        NotificationFilter notificationFilter2 = mock(NotificationFilter.class);

        when(notificationFilter1.filterExpression(any(DocumentReference.class), any(Collection.class),
            any(NotificationPreference.class)))
                .thenReturn(value(EventProperty.PAGE).eq(value("someValue1")).and(value("1").eq(value("1"))));

        when(notificationFilter2.filterExpression(any(DocumentReference.class), any(Collection.class),
            any(NotificationPreference.class)))
                .thenReturn(value(EventProperty.TYPE).eq(value("someValue2")).and(value("2").eq(value("2"))));

        when(notificationFilter1.matchesPreference(any(NotificationPreference.class))).thenReturn(true);
        when(notificationFilter2.matchesPreference(any(NotificationPreference.class))).thenReturn(true);
        when(this.notificationFilterManager.getFiltersRelatedToNotificationPreference(anyCollection(),
            any(NotificationPreference.class)))
                .thenAnswer(invocationOnMock -> ((Collection) invocationOnMock.getArgument(0)).stream());

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = USER_REFERENCE;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = this.startDate;
        parameters.blackList = Arrays.asList("event1", "event2");
        parameters.filters = Arrays.asList(notificationFilter1, notificationFilter2);
        parameters.preferences = Arrays.asList(this.pref1);
        parameters.filterPreferences = Arrays.asList(this.fakeFilterPreference);

        SimpleEventQuery query = this.generator.generateQuery(parameters);

        Iterator<QueryCondition> conditions = query.getConditions().iterator();

        assertEquals(new CompareQueryCondition(Event.FIELD_DATE, this.startDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_TYPE, "create", CompareType.EQUALS, false),
            conditions.next());
        assertEquals(
            new CompareQueryCondition(Event.FIELD_DATE, this.pref1StartDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_DOCUMENT, "someValue1", CompareType.EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_TYPE, "someValue2", CompareType.EQUALS, false),
            conditions.next());
        assertEquals(new InQueryCondition(true, Event.FIELD_ID, Arrays.asList("event1", "event2")), conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_HIDDEN, true, CompareType.EQUALS, true), conditions.next());

        List<SortClause> sortClause = query.getSorts();

        assertEquals(new SortClause(Event.FIELD_DATE, Order.DESC), sortClause.get(0));
    }

    @Test
    void generateQueryWithNoRelevantFilters() throws Exception
    {
        // Mocks
        NotificationFilter notificationFilter1 = mock(NotificationFilter.class);

        when(notificationFilter1.filterExpression(any(DocumentReference.class), any(Collection.class),
            any(NotificationPreference.class))).thenReturn(new EmptyNode());

        when(notificationFilter1.matchesPreference(any(NotificationPreference.class))).thenReturn(true);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = USER_REFERENCE;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = this.startDate;
        parameters.blackList = Arrays.asList("event1", "event2");
        parameters.filters = Collections.singleton(notificationFilter1);
        parameters.preferences = Arrays.asList(this.pref1);
        parameters.filterPreferences = Arrays.asList(this.fakeFilterPreference);

        SimpleEventQuery query = this.generator.generateQuery(parameters);

        Iterator<QueryCondition> conditions = query.getConditions().iterator();

        assertEquals(new CompareQueryCondition(Event.FIELD_DATE, this.startDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_TYPE, "create", CompareType.EQUALS, false),
            conditions.next());
        assertEquals(
            new CompareQueryCondition(Event.FIELD_DATE, this.pref1StartDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new InQueryCondition(true, Event.FIELD_ID, Arrays.asList("event1", "event2")), conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_HIDDEN, true, CompareType.EQUALS, true), conditions.next());

        List<SortClause> sortClause = query.getSorts();

        assertEquals(new SortClause(Event.FIELD_DATE, Order.DESC), sortClause.get(0));
    }

    @Test
    void generateQueryWithEventTypesThatHasNoDescriptor() throws Exception
    {
        // Mocks
        NotificationFilter notificationFilter1 = mock(NotificationFilter.class);

        when(notificationFilter1.filterExpression(any(DocumentReference.class), any(Collection.class),
            any(NotificationPreference.class)))
                .thenReturn(value(EventProperty.PAGE).eq(value("someValue1")).and(value("1").eq(value("1"))));

        when(notificationFilter1.matchesPreference(any(NotificationPreference.class))).thenReturn(true);
        when(this.notificationFilterManager.getFiltersRelatedToNotificationPreference(anyCollection(),
            any(NotificationPreference.class)))
                .thenAnswer(invocationOnMock -> ((Collection) invocationOnMock.getArgument(0)).stream());

        // No matching descriptor
        when(this.recordableEventDescriptorHelper.hasDescriptor("create", USER_REFERENCE)).thenReturn(false);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = USER_REFERENCE;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = this.startDate;
        parameters.filters = Arrays.asList(notificationFilter1);
        parameters.preferences = Arrays.asList(this.pref1);
        parameters.filterPreferences = Arrays.asList(this.fakeFilterPreference);

        SimpleEventQuery query = this.generator.generateQuery(parameters);

        Iterator<QueryCondition> conditions = query.getConditions().iterator();

        assertEquals(new CompareQueryCondition(Event.FIELD_DATE, this.startDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_HIDDEN, true, CompareType.EQUALS, true), conditions.next());

        List<SortClause> sortClause = query.getSorts();

        assertEquals(new SortClause(Event.FIELD_DATE, Order.DESC), sortClause.get(0));
    }

    @Test
    void generateQueryWithPreFiltering() throws Exception
    {
        // Mocks
        NotificationFilter notificationFilter1 = mock(NotificationFilter.class);
        when(notificationFilter1.filterExpression(any(DocumentReference.class), any(Collection.class),
            any(NotificationPreference.class))).thenReturn(new ForUserNode(USER_REFERENCE, true));
        when(notificationFilter1.matchesPreference(any(NotificationPreference.class))).thenReturn(true);
        when(this.notificationFilterManager.getFiltersRelatedToNotificationPreference(anyCollection(),
            any(NotificationPreference.class)))
                .thenAnswer(invocationOnMock -> ((Collection) invocationOnMock.getArgument(0)).stream());

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = USER_REFERENCE;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = this.startDate;
        parameters.filters = Arrays.asList(notificationFilter1);
        parameters.preferences = Arrays.asList(this.pref1);
        parameters.filterPreferences = Arrays.asList(this.fakeFilterPreference);

        SimpleEventQuery query = this.generator.generateQuery(parameters);

        Iterator<QueryCondition> conditions = query.getConditions().iterator();

        assertEquals(new CompareQueryCondition(Event.FIELD_DATE, this.startDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_TYPE, "create", CompareType.EQUALS, false),
            conditions.next());
        assertEquals(
            new CompareQueryCondition(Event.FIELD_DATE, this.pref1StartDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new StatusQueryCondition(USER_REFERENCE.toString(), true, false), conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_HIDDEN, true, CompareType.EQUALS, true), conditions.next());

        List<SortClause> sortClause = query.getSorts();

        assertEquals(new SortClause(Event.FIELD_DATE, Order.DESC), sortClause.get(0));
    }

    @Test
    void generateQueryWithPreFilteringNoUser() throws Exception
    {
        // Mocks
        NotificationFilter notificationFilter1 = mock(NotificationFilter.class);
        when(notificationFilter1.filterExpression(any(DocumentReference.class), any(Collection.class),
            any(NotificationPreference.class))).thenReturn(new ForUserNode(null, true));
        when(notificationFilter1.matchesPreference(any(NotificationPreference.class))).thenReturn(true);
        when(this.notificationFilterManager.getFiltersRelatedToNotificationPreference(anyCollection(),
            any(NotificationPreference.class)))
                .thenAnswer(invocationOnMock -> ((Collection) invocationOnMock.getArgument(0)).stream());

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = USER_REFERENCE;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = this.startDate;
        parameters.filters = Arrays.asList(notificationFilter1);
        parameters.preferences = Arrays.asList(this.pref1);
        parameters.filterPreferences = Arrays.asList(this.fakeFilterPreference);

        SimpleEventQuery query = this.generator.generateQuery(parameters);

        Iterator<QueryCondition> conditions = query.getConditions().iterator();

        assertEquals(new CompareQueryCondition(Event.FIELD_DATE, this.startDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_TYPE, "create", CompareType.EQUALS, false),
            conditions.next());
        assertEquals(
            new CompareQueryCondition(Event.FIELD_DATE, this.pref1StartDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new StatusQueryCondition(null, true, false), conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_HIDDEN, true, CompareType.EQUALS, true), conditions.next());

        List<SortClause> sortClause = query.getSorts();

        assertEquals(new SortClause(Event.FIELD_DATE, Order.DESC), sortClause.get(0));
    }

    @Test
    void generateQueryWithPreFilteringOnlyUser() throws Exception
    {
        // Mocks
        NotificationFilter notificationFilter1 = mock(NotificationFilter.class);
        when(notificationFilter1.filterExpression(any(DocumentReference.class), any(Collection.class),
            any(NotificationPreference.class))).thenReturn(new ForUserNode(USER_REFERENCE, null));
        when(notificationFilter1.matchesPreference(any(NotificationPreference.class))).thenReturn(true);
        when(this.notificationFilterManager.getFiltersRelatedToNotificationPreference(anyCollection(),
            any(NotificationPreference.class)))
                .thenAnswer(invocationOnMock -> ((Collection) invocationOnMock.getArgument(0)).stream());

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = USER_REFERENCE;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = startDate;
        parameters.filters = Arrays.asList(notificationFilter1);
        parameters.preferences = Arrays.asList(pref1);
        parameters.filterPreferences = Arrays.asList(fakeFilterPreference);

        SimpleEventQuery query = this.generator.generateQuery(parameters);

        Iterator<QueryCondition> conditions = query.getConditions().iterator();

        assertEquals(new CompareQueryCondition(Event.FIELD_DATE, this.startDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_TYPE, "create", CompareType.EQUALS, false),
            conditions.next());
        assertEquals(
            new CompareQueryCondition(Event.FIELD_DATE, this.pref1StartDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new StatusQueryCondition(USER_REFERENCE.toString(), null, false), conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_HIDDEN, true, CompareType.EQUALS, true), conditions.next());

        List<SortClause> sortClause = query.getSorts();

        assertEquals(new SortClause(Event.FIELD_DATE, Order.DESC), sortClause.get(0));
    }

    @Test
    void generateQueryWithPreFilteringEmailReadFilter() throws Exception
    {
        // Mocks
        NotificationFilter notificationFilter1 = mock(NotificationFilter.class);
        when(notificationFilter1.filterExpression(any(DocumentReference.class), any(Collection.class),
            any(NotificationPreference.class))).thenReturn(new ForUserNode(USER_REFERENCE, true));
        when(notificationFilter1.matchesPreference(any(NotificationPreference.class))).thenReturn(true);

        NotificationFilter notificationFilter2 = mock(NotificationFilter.class);
        when(notificationFilter2.filterExpression(any(DocumentReference.class), any(Collection.class),
            any(NotificationPreference.class)))
                .thenReturn(new NotNode(new ForUserNode(USER_REFERENCE, true, NotificationFormat.EMAIL)));
        when(notificationFilter2.matchesPreference(any(NotificationPreference.class))).thenReturn(true);

        when(this.notificationFilterManager.getFiltersRelatedToNotificationPreference(anyCollection(),
            any(NotificationPreference.class)))
                .thenAnswer(invocationOnMock -> ((Collection) invocationOnMock.getArgument(0)).stream());

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = USER_REFERENCE;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = this.startDate;
        parameters.filters = Arrays.asList(notificationFilter1, notificationFilter2);
        parameters.preferences = Arrays.asList(this.pref1);
        parameters.filterPreferences = Arrays.asList(this.fakeFilterPreference);

        SimpleEventQuery query = this.generator.generateQuery(parameters);

        Iterator<QueryCondition> conditions = query.getConditions().iterator();

        assertEquals(new CompareQueryCondition(Event.FIELD_DATE, this.startDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_TYPE, "create", CompareType.EQUALS, false),
            conditions.next());
        assertEquals(
            new CompareQueryCondition(Event.FIELD_DATE, this.pref1StartDate, CompareType.GREATER_OR_EQUALS, false),
            conditions.next());
        assertEquals(new StatusQueryCondition(USER_REFERENCE.toString(), true, false), conditions.next());
        assertEquals(
            new GroupQueryCondition(false, true, new MailEntityQueryCondition(SERIALIZED_USER_REFERENCE, false),
                new StatusQueryCondition(SERIALIZED_USER_REFERENCE, true, false)),
            conditions.next());
        assertEquals(new CompareQueryCondition(Event.FIELD_HIDDEN, true, CompareType.EQUALS, true), conditions.next());

        List<SortClause> sortClause = query.getSorts();

        assertEquals(new SortClause(Event.FIELD_DATE, Order.DESC), sortClause.get(0));
    }
}
