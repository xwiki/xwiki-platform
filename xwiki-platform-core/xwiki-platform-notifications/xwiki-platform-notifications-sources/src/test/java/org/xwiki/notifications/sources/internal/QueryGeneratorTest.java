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
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.map.HashedMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.NotificationProperty;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class QueryGeneratorTest
{
    @Rule
    public final MockitoComponentMockingRule<QueryGenerator> mocker =
            new MockitoComponentMockingRule<>(QueryGenerator.class);

    private QueryManager queryManager;
    private NotificationPreferenceManager notificationPreferenceManager;
    private EntityReferenceSerializer<String> serializer;
    private ConfigurationSource userPreferencesSource;
    private WikiDescriptorManager wikiDescriptorManager;
    private NotificationFilterManager notificationFilterManager;

    private DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "UserA");
    private Query query;
    private Date startDate;
    private Date pref1StartDate;

    @Before
    public void setUp() throws Exception
    {
        queryManager = mocker.getInstance(QueryManager.class);
        notificationPreferenceManager = mocker.getInstance(NotificationPreferenceManager.class);
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        userPreferencesSource = mocker.getInstance(ConfigurationSource.class, "user");
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        notificationFilterManager = mocker.getInstance(NotificationFilterManager.class);

        startDate = new Date(10);

        query = mock(Query.class);
        when(queryManager.createQuery(anyString(), anyString())).thenReturn(query);

        when(serializer.serialize(userReference)).thenReturn("xwiki:XWiki.UserA");

        pref1StartDate = new Date(100);

        NotificationPreference pref1 = mock(NotificationPreference.class);
        when(pref1.getProperties()).thenReturn(Collections.singletonMap(NotificationProperty.EVENT_TYPE, "create"));
        when(pref1.getFormat()).thenReturn(NotificationFormat.ALERT);
        when(pref1.getStartDate()).thenReturn(pref1StartDate);
        when(pref1.isNotificationEnabled()).thenReturn(true);

        when(notificationPreferenceManager.getNotificationsPreferences(userReference, true,
                NotificationFormat.ALERT)).thenReturn(Arrays.asList(pref1));

        when(userPreferencesSource.getProperty("displayHiddenDocuments", 0)).thenReturn(0);

        when(wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
    }

    @Test
    public void generateQuery() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                NotificationFormat.ALERT,
                true, null, startDate, null);

        // Verify
        verify(queryManager).createQuery(
                "where event.user <> :user AND event.date >= :startDate AND (((("
                        + "event.type = :type_0 AND event.date >= :date_0))))" +
                        " AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue("startDate", startDate);
        verify(query).bindValue(ArgumentMatchers.eq("type_0"), eq("create"));
        verify(query).bindValue(ArgumentMatchers.eq("date_0"), eq(pref1StartDate));
    }

    @Test
    public void generateQueryWhenHiddenDocsAreEnabled() throws Exception
    {
        // Mock
        when(userPreferencesSource.getProperty("displayHiddenDocuments", 0)).thenReturn(1);

        // Test
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                NotificationFormat.ALERT,
                true, null, startDate, null);

        // Verify
        verify(queryManager).createQuery(
                "where event.user <> :user AND event.date >= :startDate AND (((("
                        + "event.type = :type_0 AND event.date >= :date_0))))" +
                        " AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(ArgumentMatchers.eq("type_0"), eq("create"));
        verify(query).bindValue(ArgumentMatchers.eq("date_0"), eq(pref1StartDate));
        verify(query).bindValue("startDate", startDate);
    }

    @Test
    public void generateQueryWithNotOnlyUnread() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                NotificationFormat.ALERT,
                false, null, startDate, null);

        // Verify
        verify(queryManager).createQuery(
                "where event.user <> :user AND event.date >= :startDate AND (((("
                        + "event.type = :type_0 AND event.date >= :date_0))))" +
                        " AND event.hidden <> true " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(ArgumentMatchers.eq("type_0"), eq("create"));
        verify(query).bindValue(ArgumentMatchers.eq("date_0"), eq(pref1StartDate));
        verify(query).bindValue("startDate", startDate);
    }

    @Test
    public void generateQueryWithUntilDate() throws Exception
    {
        Date untilDate = new Date();

        // Test
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                NotificationFormat.ALERT,
                true, untilDate, startDate, Collections.emptyList());

        // Verify
        verify(queryManager).createQuery(
                "where event.user <> :user AND event.date >= :startDate AND (((("
                        + "event.type = :type_0 AND event.date >= :date_0))))" +
                        " AND event.date <= :endDate AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(ArgumentMatchers.eq("type_0"), eq("create"));
        verify(query).bindValue(ArgumentMatchers.eq("date_0"), eq(pref1StartDate));
        verify(query).bindValue("startDate", startDate);
        verify(query).bindValue("endDate", untilDate);
    }

    @Test
    public void generateQueryWithUntilDateAndBlackList() throws Exception
    {
        Date untilDate = new Date();

        // Test
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                NotificationFormat.ALERT,
                true, untilDate, null, Arrays.asList("event1", "event2"));

        // Verify
        verify(queryManager).createQuery(
                "where event.user <> :user AND (((("
                        + "event.type = :type_0 AND event.date >= :date_0))))" +
                        " AND event.id NOT IN (:blackList) AND event.date <= :endDate AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(ArgumentMatchers.eq("type_0"), eq("create"));
        verify(query).bindValue(ArgumentMatchers.eq("date_0"), eq(pref1StartDate));
        verify(query, never()).bindValue(ArgumentMatchers.eq("startDate"), any(Date.class));
        verify(query).bindValue("endDate", untilDate);
        verify(query).bindValue("blackList", Arrays.asList("event1", "event2"));
    }

    @Test
    public void generateQueryWithLocalUser() throws Exception
    {
        // Test
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                NotificationFormat.ALERT,
                true, null, startDate, null);

        // Verify
        verify(queryManager).createQuery(
                "where event.user <> :user AND event.date >= :startDate AND (((("
                        + "event.type = :type_0 AND event.date >= :date_0))))" +
                        " AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true))" +
                        " AND event.wiki = :userWiki " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(ArgumentMatchers.eq("type_0"), eq("create"));
        verify(query).bindValue(ArgumentMatchers.eq("date_0"), eq(pref1StartDate));
        verify(query).bindValue("startDate", startDate);
        verify(query).bindValue("userWiki", "xwiki");
    }

    @Test
    public void generateQueryWithFilters() throws Exception
    {
        Date untilDate = new Date();

        // Mocks
        NotificationFilter notificationFilter1 = mock(NotificationFilter.class);
        NotificationFilter notificationFilter2 = mock(NotificationFilter.class);
        when(notificationFilterManager.getNotificationFilters(any(DocumentReference.class),
                any(NotificationPreference.class))).thenReturn(Arrays.asList(notificationFilter1, notificationFilter2));

        when(notificationFilter1.queryFilterOR(
                any(DocumentReference.class), any(NotificationFormat.class), ArgumentMatchers
                        .anyMap()))
                .thenReturn("event.date = :someDate");
        when(notificationFilter2.queryFilterOR(
                any(DocumentReference.class), any(NotificationFormat.class), ArgumentMatchers
                        .anyMap()))
                .thenReturn("event.eventType = :someVal");

        when(notificationFilter1.queryFilterAND(
                any(DocumentReference.class), any(NotificationFormat.class), ArgumentMatchers
                        .anyMap()))
                .thenReturn("1=1");
        when(notificationFilter2.queryFilterAND(
                any(DocumentReference.class), any(NotificationFormat.class), ArgumentMatchers
                        .anyMap()))
                .thenReturn("2=2");


        when(notificationFilter1.queryFilterParams(any(DocumentReference.class),
                any(NotificationFormat.class),
                any(List.class))).thenReturn(new HashedMap() {{
            put("someDate", "someValue1");
        }});
        when(notificationFilter2.queryFilterParams(any(DocumentReference.class),
                any(NotificationFormat.class),
                any(List.class))).thenReturn(new HashedMap() {{
            put("someVal", "someValue2");
        }});

        when(notificationFilter1.matchesPreference(any(NotificationPreference.class))).thenReturn(true);
        when(notificationFilter2.matchesPreference(any(NotificationPreference.class))).thenReturn(true);

        // Test
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                NotificationFormat.ALERT,
                true, untilDate, startDate, Arrays.asList("event1", "event2"));

        // Verify
        verify(queryManager).createQuery(
                "where event.user <> :user AND event.date >= :startDate AND " +
                        "((((event.type = :type_0 AND event.date >= :date_0)"
                        + " AND (event.date = :someDate OR event.eventType = :someVal) AND 1=1 AND 2=2)))" +
                        " AND event.id NOT IN (:blackList) AND event.date <= :endDate AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(ArgumentMatchers.eq("type_0"), eq("create"));
        verify(query).bindValue(ArgumentMatchers.eq("date_0"), eq(pref1StartDate));
        verify(query).bindValue("startDate", startDate);
        verify(query).bindValue("endDate", untilDate);
        verify(query).bindValue("blackList", Arrays.asList("event1", "event2"));
        verify(query).bindValue("someDate", "someValue1");
        verify(query).bindValue("someVal", "someValue2");
    }

}
