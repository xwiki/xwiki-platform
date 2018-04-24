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
import org.mockito.internal.util.collections.Sets;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.expression.AndNode;
import org.xwiki.notifications.filters.expression.EmptyNode;
import org.xwiki.notifications.filters.expression.EqualsNode;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.expression.PropertyValueNode;
import org.xwiki.notifications.filters.expression.StringValueNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private ConfigurationSource userPreferencesSource;
    private WikiDescriptorManager wikiDescriptorManager;
    private NotificationFilterManager notificationFilterManager;

    private DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "UserA");
    private Query query;
    private Date startDate;
    private Date pref1StartDate;
    private NotificationFilterPreference fakeFilterPreference;
    private NotificationPreference pref1;

    @Before
    public void setUp() throws Exception
    {
        queryManager = mocker.getInstance(QueryManager.class);
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        userPreferencesSource = mocker.getInstance(ConfigurationSource.class, "user");
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        notificationFilterManager = mocker.getInstance(NotificationFilterManager.class);

        startDate = new Date(10);

        query = mock(Query.class);
        when(queryManager.createQuery(anyString(), anyString())).thenReturn(query);

        pref1StartDate = new Date(100000000);

        pref1 = mock(NotificationPreference.class);
        when(pref1.getProperties()).thenReturn(Collections.singletonMap(
                NotificationPreferenceProperty.EVENT_TYPE, "create"));
        when(pref1.getFormat()).thenReturn(NotificationFormat.ALERT);
        when(pref1.getStartDate()).thenReturn(pref1StartDate);
        when(pref1.isNotificationEnabled()).thenReturn(true);

        fakeFilterPreference = mock(NotificationFilterPreference.class);
        when(fakeFilterPreference.isActive()).thenReturn(true);
        when(notificationFilterManager.getFilterPreferences(any(DocumentReference.class)))
                .thenReturn(Sets.newSet(fakeFilterPreference));

        when(userPreferencesSource.getProperty("displayHiddenDocuments", 0)).thenReturn(0);

        when(wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
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
        assertEquals("((DATE >= \"Thu Jan 01 01:00:00 CET 1970\" " +
                "AND (TYPE = \"create\" AND DATE >= \"Fri Jan 02 04:46:40 CET 1970\")) AND HIDDEN <> true) " +
                        "ORDER BY DATE DESC",
                node.toString());

        // Test 2
        mocker.getComponentUnderTest().generateQuery(parameters);


        verify(queryManager).createQuery(
                "where ((" +
                        "event.date >= :date_688218ea2b05763819a1e155109e4bf1e8921dd72e8b43d4c89c89133d4a5357) " +
                        "AND ((event.type = :value_fa8847b0c33183273f5945508b31c3208a9e4ece58ca47233a05628d8dba3799) " +
                        "AND (event.date >= :date_25db83d7521312b07fa98ca0023df696d1b94ee4fb7c49578c807f5aeb634f7a))) " +
                        "AND (event.hidden <> true) " +
                        "ORDER BY event.date DESC", Query.HQL);
        verify(query).bindValue("date_688218ea2b05763819a1e155109e4bf1e8921dd72e8b43d4c89c89133d4a5357", startDate);
        verify(query).bindValue("date_25db83d7521312b07fa98ca0023df696d1b94ee4fb7c49578c807f5aeb634f7a", pref1StartDate);
        verify(query).bindValue(eq("value_fa8847b0c33183273f5945508b31c3208a9e4ece58ca47233a05628d8dba3799"),
                eq("create"));

    }

    @Test
    public void generateQueryWhenHiddenDocsAreEnabled() throws Exception
    {
        // Mock
        when(userPreferencesSource.getProperty("displayHiddenDocuments", 0)).thenReturn(1);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = userReference;
        parameters.format = NotificationFormat.ALERT;
        parameters.fromDate = startDate;
        parameters.preferences = Arrays.asList(pref1);
        parameters.filterPreferences = Arrays.asList(fakeFilterPreference);
        ExpressionNode node = mocker.getComponentUnderTest().generateQueryExpression(parameters);

        // Verify
        assertEquals("(DATE >= \"Thu Jan 01 01:00:00 CET 1970\" " +
                "AND (TYPE = \"create\" AND DATE >= \"Fri Jan 02 04:46:40 CET 1970\")) " +
                "ORDER BY DATE DESC", node.toString());

        // Test 2
        mocker.getComponentUnderTest().generateQuery(parameters);


        verify(queryManager).createQuery(
                "where (" +
                        "event.date >= :date_688218ea2b05763819a1e155109e4bf1e8921dd72e8b43d4c89c89133d4a5357) " +
                        "AND ((event.type = :value_fa8847b0c33183273f5945508b31c3208a9e4ece58ca47233a05628d8dba3799) " +
                        "AND (event.date >= :date_25db83d7521312b07fa98ca0023df696d1b94ee4fb7c49578c807f5aeb634f7a)) " +
                        "ORDER BY event.date DESC", Query.HQL);
        verify(query).bindValue(eq("date_688218ea2b05763819a1e155109e4bf1e8921dd72e8b43d4c89c89133d4a5357"),
                eq(startDate));
        verify(query).bindValue(eq("value_fa8847b0c33183273f5945508b31c3208a9e4ece58ca47233a05628d8dba3799"),
                eq("create"));
        verify(query).bindValue("date_25db83d7521312b07fa98ca0023df696d1b94ee4fb7c49578c807f5aeb634f7a", pref1StartDate);
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
        assertEquals("((DATE >= \"Thu Jan 01 01:00:00 CET 1970\" " +
                "AND (TYPE = \"create\" AND DATE >= \"Fri Jan 02 04:46:40 CET 1970\")) AND HIDDEN <> true) " +
                "ORDER BY DATE DESC", node.toString());

        // Test 2
        mocker.getComponentUnderTest().generateQuery(parameters);

        verify(queryManager).createQuery(
                "where ((" +
                        "event.date >= :date_688218ea2b05763819a1e155109e4bf1e8921dd72e8b43d4c89c89133d4a5357) " +
                        "AND ((event.type = :value_fa8847b0c33183273f5945508b31c3208a9e4ece58ca47233a05628d8dba3799) " +
                        "AND (event.date >= :date_25db83d7521312b07fa98ca0023df696d1b94ee4fb7c49578c807f5aeb634f7a))) " +
                        "AND (event.hidden <> true) " +
                        "ORDER BY event.date DESC",
                Query.HQL);
    }

    @Test
    public void generateQueryWithUntilDate() throws Exception
    {
        Date untilDate = new Date(1000000000000L);

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
        assertEquals("(((DATE >= \"Thu Jan 01 01:00:00 CET 1970\" " +
                "AND (TYPE = \"create\" AND DATE >= \"Fri Jan 02 04:46:40 CET 1970\")) " +
                "AND DATE <= \"Sun Sep 09 03:46:40 CEST 2001\") AND HIDDEN <> true) " +
                "ORDER BY DATE DESC", node.toString());

        // Test 2
        mocker.getComponentUnderTest().generateQuery(parameters);


        verify(queryManager).createQuery(
                "where (((" +
                        "event.date >= :date_688218ea2b05763819a1e155109e4bf1e8921dd72e8b43d4c89c89133d4a5357) " +
                        "AND ((event.type = :value_fa8847b0c33183273f5945508b31c3208a9e4ece58ca47233a05628d8dba3799) " +
                        "AND (event.date >= :date_25db83d7521312b07fa98ca0023df696d1b94ee4fb7c49578c807f5aeb634f7a))) " +
                        "AND (event.date <= :date_582ce8e50c9ad1782bdd021604912ed119e6ab2ff58a094f23b3be0ce6105306)) " +
                        "AND (event.hidden <> true) " +
                        "ORDER BY event.date DESC",
                Query.HQL);
        verify(query).bindValue("date_688218ea2b05763819a1e155109e4bf1e8921dd72e8b43d4c89c89133d4a5357", startDate);
        verify(query).bindValue("date_25db83d7521312b07fa98ca0023df696d1b94ee4fb7c49578c807f5aeb634f7a", pref1StartDate);
        verify(query).bindValue("date_582ce8e50c9ad1782bdd021604912ed119e6ab2ff58a094f23b3be0ce6105306", untilDate);

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
                "AND DATE >= \"Fri Jan 02 04:46:40 CET 1970\") " +
                "AND NOT (ID IN (\"event1\", \"event2\"))) " +
                "AND DATE <= \"Sun Sep 09 03:46:40 CEST 2001\") " +
                "AND HIDDEN <> true) " +
                "ORDER BY DATE DESC",
                node.toString()
        );
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
        assertEquals("(((DATE >= \"Thu Jan 01 01:00:00 CET 1970\" "
                + "AND (TYPE = \"create\" AND DATE >= \"Fri Jan 02 04:46:40 CET 1970\")) AND HIDDEN <> true) "
                + "AND WIKI = \"Wiki xwiki\") "
                + "ORDER BY DATE DESC",
                node.toString()
        );
    }

    @Test
    public void generateQueryWithFilters() throws Exception
    {
        // Mocks
        NotificationFilter notificationFilter1 = mock(NotificationFilter.class);
        NotificationFilter notificationFilter2 = mock(NotificationFilter.class);

        when(notificationFilter1.filterExpression(any(DocumentReference.class), any(Collection.class),
                any(NotificationPreference.class)))
                .thenReturn(
                        new AndNode(
                                new EqualsNode(
                                        new PropertyValueNode(EventProperty.PAGE),
                                        new StringValueNode("someValue1")),
                                new EqualsNode(
                                        new StringValueNode("1"),
                                        new StringValueNode("1"))));

        when(notificationFilter2.filterExpression(any(DocumentReference.class), any(Collection.class),
                any(NotificationPreference.class)))
                .thenReturn(
                        new AndNode(
                                new EqualsNode(
                                        new PropertyValueNode(EventProperty.TYPE),
                                        new StringValueNode("someValue2")),
                                new EqualsNode(
                                        new StringValueNode("2"),
                                        new StringValueNode("2"))));

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

        assertEquals("(((DATE >= \"Thu Jan 01 01:00:00 CET 1970\" " +
                "AND (((TYPE = \"create\" AND DATE >= \"Fri Jan 02 04:46:40 CET 1970\") " +
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

        assertEquals("(((DATE >= \"Thu Jan 01 01:00:00 CET 1970\" " +
                "AND (TYPE = \"create\" AND DATE >= \"Fri Jan 02 04:46:40 CET 1970\")) " +
                "AND NOT (ID IN (\"event1\", \"event2\"))) " +
                "AND HIDDEN <> true) " +
                "ORDER BY DATE DESC",
                node.toString()
        );
    }

}
