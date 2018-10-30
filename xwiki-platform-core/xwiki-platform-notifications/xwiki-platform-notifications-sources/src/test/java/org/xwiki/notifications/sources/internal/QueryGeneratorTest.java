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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.expression.EmptyNode;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import org.apache.commons.codec.digest.DigestUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * @version $Id$
 */
@ComponentList({
    ExpressionNodeToHQLConverter.class,
    DefaultStringEntityReferenceSerializer.class,
    DefaultSymbolScheme.class
})
public class QueryGeneratorTest
{
    @Rule
    public final MockitoComponentMockingRule<QueryGenerator> mocker =
            new MockitoComponentMockingRule<>(QueryGenerator.class);

    private QueryManager queryManager;
    private EntityReferenceSerializer<String> serializer;
    private WikiDescriptorManager wikiDescriptorManager;
    private NotificationFilterManager notificationFilterManager;
    private DocumentAccessBridge documentAccessBridge;
    private RecordableEventDescriptorHelper recordableEventDescriptorHelper;

    private DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "UserA");
    private Query query;
    private Date startDate;
    private String startDateParamName;
    private Date pref1StartDate;
    private String pref1StartDateParamName;

    private NotificationFilterPreference fakeFilterPreference;
    private NotificationPreference pref1;

    @Before
    public void setUp() throws Exception
    {
        queryManager = mocker.getInstance(QueryManager.class);
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        notificationFilterManager = mocker.getInstance(NotificationFilterManager.class);
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        recordableEventDescriptorHelper = mocker.getInstance(RecordableEventDescriptorHelper.class);

        startDate = new Date(10);
        this.startDateParamName = String.format("date_%s", DigestUtils.sha256Hex(this.startDate.toString()));

        query = mock(Query.class);
        when(queryManager.createQuery(anyString(), anyString())).thenReturn(query);

        pref1StartDate = new Date(100000000);
        this.pref1StartDateParamName = String.format("date_%s", DigestUtils.sha256Hex(this.pref1StartDate.toString()));

        pref1 = mock(NotificationPreference.class);
        when(pref1.getProperties()).thenReturn(Collections.singletonMap(
                NotificationPreferenceProperty.EVENT_TYPE, "create"));
        when(pref1.getFormat()).thenReturn(NotificationFormat.ALERT);
        when(pref1.getStartDate()).thenReturn(pref1StartDate);
        when(pref1.isNotificationEnabled()).thenReturn(true);

        fakeFilterPreference = mock(NotificationFilterPreference.class);
        when(fakeFilterPreference.isActive()).thenReturn(true);

        when(documentAccessBridge.getProperty(userReference,
                new DocumentReference("xwiki", "XWiki", "XWikiUsers"),
                "displayHiddenDocuments")).thenReturn(0);

        when(wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");

        when(recordableEventDescriptorHelper.hasDescriptor(anyString(), any(DocumentReference.class))).thenReturn(true);
    }

    @Test
    public void generateQueryExpression() throws Exception
    {
        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = userReference;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = startDate;
        parameters.preferences = Arrays.asList(pref1);
        parameters.filterPreferences = Arrays.asList(fakeFilterPreference);
        ExpressionNode node = mocker.getComponentUnderTest().generateQueryExpression(parameters);

        // Verify
        assertEquals("((DATE >= \"" + this.startDate.toString() + "\" " +
            "AND (TYPE = \"create\" AND DATE >= \"" + this.pref1StartDate.toString() + "\")) AND HIDDEN <> true) " +
            "ORDER BY DATE DESC",
            node.toString());

        // Test 2
        mocker.getComponentUnderTest().generateQuery(parameters);

        verify(this.queryManager).createQuery(
            "where ((" +
                "event.date >= :" + this.startDateParamName + ") " +
                "AND ((event.type = :value_fa8847b0c33183273f5945508b31c3208a9e4ece58ca47233a05628d8dba3799) " +
                "AND (event.date >= :" + this.pref1StartDateParamName + "))) " +
                "AND (event.hidden <> true) " +
                "ORDER BY event.date DESC",
            Query.HQL);
        verify(this.query).bindValue(this.startDateParamName, this.startDate);
        verify(this.query).bindValue(this.pref1StartDateParamName, this.pref1StartDate);
        verify(this.query).bindValue(eq("value_fa8847b0c33183273f5945508b31c3208a9e4ece58ca47233a05628d8dba3799"),
            eq("create"));

    }

    @Test
    public void generateQueryWhenHiddenDocsAreEnabled() throws Exception
    {
        // Mock
        when(documentAccessBridge.getProperty(userReference,
                new DocumentReference("xwiki", "XWiki", "XWikiUsers"),
                "displayHiddenDocuments")).thenReturn(1);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = userReference;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = startDate;
        parameters.preferences = Arrays.asList(pref1);
        parameters.filterPreferences = Arrays.asList(fakeFilterPreference);
        ExpressionNode node = mocker.getComponentUnderTest().generateQueryExpression(parameters);

        // Verify
        assertEquals("(DATE >= \"" + this.startDate.toString() + "\" " +
            "AND (TYPE = \"create\" AND DATE >= \"" + this.pref1StartDate.toString() + "\")) " +
            "ORDER BY DATE DESC", node.toString());

        // Test 2
        mocker.getComponentUnderTest().generateQuery(parameters);

        verify(this.queryManager).createQuery(
            "where (" +
                "event.date >= :" + this.startDateParamName + ") " +
                "AND ((event.type = :value_fa8847b0c33183273f5945508b31c3208a9e4ece58ca47233a05628d8dba3799) " +
                "AND (event.date >= :" + this.pref1StartDateParamName + ")) " +
                "ORDER BY event.date DESC",
            Query.HQL);
        verify(this.query).bindValue(eq(this.startDateParamName), eq(this.startDate));
        verify(this.query).bindValue(eq("value_fa8847b0c33183273f5945508b31c3208a9e4ece58ca47233a05628d8dba3799"),
            eq("create"));
        verify(this.query).bindValue(this.pref1StartDateParamName, this.pref1StartDate);
    }

    @Test
    public void generateQueryWithNotOnlyUnread() throws Exception
    {
        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = userReference;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = startDate;
        parameters.preferences = Arrays.asList(pref1);
        parameters.filterPreferences = Arrays.asList(fakeFilterPreference);
        ExpressionNode node = mocker.getComponentUnderTest().generateQueryExpression(parameters);

        // Verify
        assertEquals("((DATE >= \"" + this.startDate.toString() + "\" " +
            "AND (TYPE = \"create\" AND DATE >= \"" + this.pref1StartDate.toString() + "\")) AND HIDDEN <> true) " +
            "ORDER BY DATE DESC", node.toString());

        // Test 2
        mocker.getComponentUnderTest().generateQuery(parameters);

        verify(this.queryManager).createQuery(
            "where ((" +
                "event.date >= :" + this.startDateParamName + ") " +
                "AND ((event.type = :value_fa8847b0c33183273f5945508b31c3208a9e4ece58ca47233a05628d8dba3799) " +
                "AND (event.date >= :" + this.pref1StartDateParamName + "))) " +
                "AND (event.hidden <> true) " +
                "ORDER BY event.date DESC",
            Query.HQL);
    }

    @Test
    public void generateQueryWithUntilDate() throws Exception
    {
        Date untilDate = new Date(1000000000000L);
        String untilDateParamName = String.format("date_%s", DigestUtils.sha256Hex(untilDate.toString()));

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = userReference;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = startDate;
        parameters.endDate = untilDate;
        parameters.preferences = Arrays.asList(pref1);
        parameters.filterPreferences = Arrays.asList(fakeFilterPreference);
        ExpressionNode node = mocker.getComponentUnderTest().generateQueryExpression(parameters);

        // Verify
        assertEquals("(((DATE >= \"" + this.startDate.toString() + "\" " +
            "AND (TYPE = \"create\" AND DATE >= \"" + this.pref1StartDate.toString() + "\")) " +
            "AND DATE <= \"" + untilDate.toString() + "\") AND HIDDEN <> true) " +
            "ORDER BY DATE DESC", node.toString());

        // Test 2
        mocker.getComponentUnderTest().generateQuery(parameters);

        verify(this.queryManager).createQuery(
            "where (((" +
                "event.date >= :" + this.startDateParamName + ") " +
                "AND ((event.type = :value_fa8847b0c33183273f5945508b31c3208a9e4ece58ca47233a05628d8dba3799) " +
                "AND (event.date >= :" + this.pref1StartDateParamName + "))) " +
                "AND (event.date <= :" + untilDateParamName + ")) " +
                "AND (event.hidden <> true) " +
                "ORDER BY event.date DESC",
            Query.HQL);
        verify(this.query).bindValue(this.startDateParamName, this.startDate);
        verify(this.query).bindValue(this.pref1StartDateParamName, this.pref1StartDate);
        verify(this.query).bindValue(untilDateParamName, untilDate);

    }

    @Test
    public void generateQueryWithUntilDateAndBlackList() throws Exception
    {
        Date untilDate = new Date(1000000000000L);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = userReference;
        parameters.format = NotificationFormat.ALERT;
        parameters.endDate = untilDate;
        parameters.blackList = Arrays.asList("event1", "event2");
        parameters.preferences = Arrays.asList(pref1);
        parameters.filterPreferences = Arrays.asList(fakeFilterPreference);
        ExpressionNode node = mocker.getComponentUnderTest().generateQueryExpression(parameters);

        // Verify
        assertEquals("((((TYPE = \"create\" " +
            "AND DATE >= \"" + this.pref1StartDate.toString() + "\") " +
            "AND NOT (ID IN (\"event1\", \"event2\"))) " +
            "AND DATE <= \"" + untilDate.toString() + "\") " +
            "AND HIDDEN <> true) " +
            "ORDER BY DATE DESC",
            node.toString());
    }

    @Test
    public void generateQueryWithLocalUser() throws Exception
    {
        // Test
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = userReference;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = startDate;
        parameters.preferences = Arrays.asList(pref1);
        parameters.filterPreferences = Arrays.asList(fakeFilterPreference);
        ExpressionNode node = mocker.getComponentUnderTest().generateQueryExpression(parameters);

        // Verify
        assertEquals("(((DATE >= \"" + this.startDate.toString() + "\" "
            + "AND (TYPE = \"create\" AND DATE >= \"" + this.pref1StartDate.toString() + "\")) AND HIDDEN <> true) "
            + "AND WIKI = \"Wiki xwiki\") "
            + "ORDER BY DATE DESC",
            node.toString());
    }

    @Test
    public void generateQueryWithFilters() throws Exception
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
        when(notificationFilterManager.getFiltersRelatedToNotificationPreference(anyCollection(),
                any(NotificationPreference.class))).thenAnswer(
                        invocationOnMock -> ((Collection)invocationOnMock.getArgument(0)).stream());

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = userReference;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = startDate;
        parameters.blackList = Arrays.asList("event1", "event2");
        parameters.filters = Arrays.asList(notificationFilter1, notificationFilter2);
        parameters.preferences = Arrays.asList(pref1);
        parameters.filterPreferences = Arrays.asList(fakeFilterPreference);
        ExpressionNode node = mocker.getComponentUnderTest().generateQueryExpression(parameters);

        assertEquals("(((DATE >= \"" + this.startDate.toString() + "\" " +
            "AND (((TYPE = \"create\" AND DATE >= \"" + this.pref1StartDate.toString() + "\") " +
            "AND (PAGE = \"someValue1\" AND \"1\" = \"1\")) " +
            "AND (TYPE = \"someValue2\" AND \"2\" = \"2\"))) " +
            "AND NOT (ID IN (\"event1\", \"event2\"))) " +
            "AND HIDDEN <> true) " +
            "ORDER BY DATE DESC", node.toString());
    }

    @Test
    public void generateQueryWithNoRelevantFilters() throws Exception
    {

        // Mocks
        NotificationFilter notificationFilter1 = mock(NotificationFilter.class);

        when(notificationFilter1.filterExpression(any(DocumentReference.class), any(Collection.class),
                any(NotificationPreference.class)))
                .thenReturn(new EmptyNode());

        when(notificationFilter1.matchesPreference(any(NotificationPreference.class))).thenReturn(true);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = userReference;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = startDate;
        parameters.blackList = Arrays.asList("event1", "event2");
        parameters.filters = Collections.singleton(notificationFilter1);
        parameters.preferences = Arrays.asList(pref1);
        parameters.filterPreferences = Arrays.asList(fakeFilterPreference);
        ExpressionNode node = mocker.getComponentUnderTest().generateQueryExpression(parameters);

        assertEquals("(((DATE >= \"" + this.startDate.toString() + "\" " +
            "AND (TYPE = \"create\" AND DATE >= \"" + this.pref1StartDate.toString() + "\")) " +
            "AND NOT (ID IN (\"event1\", \"event2\"))) " +
            "AND HIDDEN <> true) " +
            "ORDER BY DATE DESC",
            node.toString());
    }

    @Test
    public void generateQueryWithEventTypesThatHasNoDescriptor() throws Exception
    {
        // Mocks
        NotificationFilter notificationFilter1 = mock(NotificationFilter.class);

        when(notificationFilter1.filterExpression(any(DocumentReference.class), any(Collection.class),
                any(NotificationPreference.class)))
                .thenReturn(value(EventProperty.PAGE).eq(value("someValue1")).and(value("1").eq(value("1"))));

        when(notificationFilter1.matchesPreference(any(NotificationPreference.class))).thenReturn(true);
        when(notificationFilterManager.getFiltersRelatedToNotificationPreference(anyCollection(),
                any(NotificationPreference.class))).thenAnswer(
                invocationOnMock -> ((Collection)invocationOnMock.getArgument(0)).stream());

        // No matching descriptor
        when(recordableEventDescriptorHelper.hasDescriptor("create", userReference)).thenReturn(false);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = userReference;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = startDate;
        parameters.filters = Arrays.asList(notificationFilter1);
        parameters.preferences = Arrays.asList(pref1);
        parameters.filterPreferences = Arrays.asList(fakeFilterPreference);
        ExpressionNode node = mocker.getComponentUnderTest().generateQueryExpression(parameters);

        // Expectation: no filters on "create" event type because it has no descriptor
        assertEquals("(DATE >= \"" + this.startDate.toString() + "\" AND HIDDEN <> true) ORDER BY DATE DESC",
            node.toString());
    }

}
