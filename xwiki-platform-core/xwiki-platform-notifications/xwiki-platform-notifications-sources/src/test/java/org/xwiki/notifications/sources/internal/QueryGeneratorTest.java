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
import org.mockito.Mockito;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

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

        query = Mockito.mock(Query.class);
        Mockito.when(queryManager.createQuery(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(query);

        Mockito.when(serializer.serialize(userReference)).thenReturn("xwiki:XWiki.UserA");

        pref1StartDate = new Date(100);
        NotificationPreference pref1 = new NotificationPreference("create", true,
                NotificationFormat.ALERT, pref1StartDate);
        Mockito.when(notificationPreferenceManager.getNotificationsPreferences(userReference)).thenReturn(Arrays.asList(pref1));

        Mockito.when(userPreferencesSource.getProperty("displayHiddenDocuments", 0)).thenReturn(0);

        Mockito.when(wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
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
        Mockito.verify(queryManager).createQuery(
                "where event.user <> :user AND event.date >= :startDate AND (((("
                        + "event.type = :type_0 AND event.date >= :date_0))))" +
                        " AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        Mockito.verify(query).bindValue("user", "xwiki:XWiki.UserA");
        Mockito.verify(query).bindValue("startDate", startDate);
        Mockito.verify(query).bindValue(ArgumentMatchers.eq("type_0"), ArgumentMatchers.eq("create"));
        Mockito.verify(query).bindValue(ArgumentMatchers.eq("date_0"), ArgumentMatchers.eq(pref1StartDate));
    }

    @Test
    public void generateQueryWhenHiddenDocsAreEnabled() throws Exception
    {
        // Mock
        Mockito.when(userPreferencesSource.getProperty("displayHiddenDocuments", 0)).thenReturn(1);

        // Test
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                NotificationFormat.ALERT,
                true, null, startDate, null);

        // Verify
        Mockito.verify(queryManager).createQuery(
                "where event.user <> :user AND event.date >= :startDate AND (((("
                        + "event.type = :type_0 AND event.date >= :date_0))))" +
                        " AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        Mockito.verify(query).bindValue("user", "xwiki:XWiki.UserA");
        Mockito.verify(query).bindValue(ArgumentMatchers.eq("type_0"), ArgumentMatchers.eq("create"));
        Mockito.verify(query).bindValue(ArgumentMatchers.eq("date_0"), ArgumentMatchers.eq(pref1StartDate));
        Mockito.verify(query).bindValue("startDate", startDate);
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
        Mockito.verify(queryManager).createQuery(
                "where event.user <> :user AND event.date >= :startDate AND (((("
                        + "event.type = :type_0 AND event.date >= :date_0))))" +
                        " AND event.hidden <> true " +
                        "order by event.date DESC", Query.HQL);
        Mockito.verify(query).bindValue("user", "xwiki:XWiki.UserA");
        Mockito.verify(query).bindValue(ArgumentMatchers.eq("type_0"), ArgumentMatchers.eq("create"));
        Mockito.verify(query).bindValue(ArgumentMatchers.eq("date_0"), ArgumentMatchers.eq(pref1StartDate));
        Mockito.verify(query).bindValue("startDate", startDate);
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
        Mockito.verify(queryManager).createQuery(
                "where event.user <> :user AND event.date >= :startDate AND (((("
                        + "event.type = :type_0 AND event.date >= :date_0))))" +
                        " AND event.date <= :endDate AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        Mockito.verify(query).bindValue("user", "xwiki:XWiki.UserA");
        Mockito.verify(query).bindValue(ArgumentMatchers.eq("type_0"), ArgumentMatchers.eq("create"));
        Mockito.verify(query).bindValue(ArgumentMatchers.eq("date_0"), ArgumentMatchers.eq(pref1StartDate));
        Mockito.verify(query).bindValue("startDate", startDate);
        Mockito.verify(query).bindValue("endDate", untilDate);
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
        Mockito.verify(queryManager).createQuery(
                "where event.user <> :user AND (((("
                        + "event.type = :type_0 AND event.date >= :date_0))))" +
                        " AND event.id NOT IN (:blackList) AND event.date <= :endDate AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        Mockito.verify(query).bindValue("user", "xwiki:XWiki.UserA");
        Mockito.verify(query).bindValue(ArgumentMatchers.eq("type_0"), ArgumentMatchers.eq("create"));
        Mockito.verify(query).bindValue(ArgumentMatchers.eq("date_0"), ArgumentMatchers.eq(pref1StartDate));
        Mockito.verify(query, Mockito.never()).bindValue(ArgumentMatchers.eq("startDate"), ArgumentMatchers.any(Date.class));
        Mockito.verify(query).bindValue("endDate", untilDate);
        Mockito.verify(query).bindValue("blackList", Arrays.asList("event1", "event2"));
    }

    @Test
    public void generateQueryWithLocalUser() throws Exception
    {
        // Test
        Mockito.when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                NotificationFormat.ALERT,
                true, null, startDate, null);

        // Verify
        Mockito.verify(queryManager).createQuery(
                "where event.user <> :user AND event.date >= :startDate AND (((("
                        + "event.type = :type_0 AND event.date >= :date_0))))" +
                        " AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true))" +
                        " AND event.wiki = :userWiki " +
                        "order by event.date DESC", Query.HQL);
        Mockito.verify(query).bindValue("user", "xwiki:XWiki.UserA");
        Mockito.verify(query).bindValue(ArgumentMatchers.eq("type_0"), ArgumentMatchers.eq("create"));
        Mockito.verify(query).bindValue(ArgumentMatchers.eq("date_0"), ArgumentMatchers.eq(pref1StartDate));
        Mockito.verify(query).bindValue("startDate", startDate);
        Mockito.verify(query).bindValue("userWiki", "xwiki");
    }

    @Test
    public void generateQueryWithFilters() throws Exception
    {
        Date untilDate = new Date();

        // Mocks
        NotificationFilter notificationFilter1 = Mockito.mock(NotificationFilter.class);
        NotificationFilter notificationFilter2 = Mockito.mock(NotificationFilter.class);
        Mockito.when(notificationFilterManager.getAllNotificationFilters(ArgumentMatchers.any(DocumentReference.class))).thenReturn(
                Arrays.asList(notificationFilter1, notificationFilter2)
        );

        Mockito.when(notificationFilter1.queryFilterOR(
                ArgumentMatchers.any(DocumentReference.class), ArgumentMatchers.any(NotificationFormat.class), ArgumentMatchers
                        .anyMap()))
                .thenReturn("event.date = :someDate");
        Mockito.when(notificationFilter2.queryFilterOR(
                ArgumentMatchers.any(DocumentReference.class), ArgumentMatchers.any(NotificationFormat.class), ArgumentMatchers
                        .anyMap()))
                .thenReturn("event.eventType = :someVal");

        Mockito.when(notificationFilter1.queryFilterAND(
                ArgumentMatchers.any(DocumentReference.class), ArgumentMatchers.any(NotificationFormat.class), ArgumentMatchers
                        .anyMap()))
                .thenReturn("1=1");
        Mockito.when(notificationFilter2.queryFilterAND(
                ArgumentMatchers.any(DocumentReference.class), ArgumentMatchers.any(NotificationFormat.class), ArgumentMatchers
                        .anyMap()))
                .thenReturn("2=2");


        Mockito.when(notificationFilter1.queryFilterParams(ArgumentMatchers.any(DocumentReference.class), ArgumentMatchers
                        .any(NotificationFormat.class),
                ArgumentMatchers.any(List.class))).thenReturn(new HashedMap() {{
            put("someDate", "someValue1");
        }});
        Mockito.when(notificationFilter2.queryFilterParams(ArgumentMatchers.any(DocumentReference.class), ArgumentMatchers
                        .any(NotificationFormat.class),
                ArgumentMatchers.any(List.class))).thenReturn(new HashedMap() {{
            put("someVal", "someValue2");
        }});

        // Test
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                NotificationFormat.ALERT,
                true, untilDate, startDate, Arrays.asList("event1", "event2"));

        // Verify
        Mockito.verify(queryManager).createQuery(
                "where event.user <> :user AND event.date >= :startDate AND " +
                        "((((event.type = :type_0 AND event.date >= :date_0)"
                        + " AND (event.date = :someDate OR event.eventType = :someVal) AND 1=1 AND 2=2)))" +
                        " AND event.id NOT IN (:blackList) AND event.date <= :endDate AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        Mockito.verify(query).bindValue("user", "xwiki:XWiki.UserA");
        Mockito.verify(query).bindValue(ArgumentMatchers.eq("type_0"), ArgumentMatchers.eq("create"));
        Mockito.verify(query).bindValue(ArgumentMatchers.eq("date_0"), ArgumentMatchers.eq(pref1StartDate));
        Mockito.verify(query).bindValue("startDate", startDate);
        Mockito.verify(query).bindValue("endDate", untilDate);
        Mockito.verify(query).bindValue("blackList", Arrays.asList("event1", "event2"));
        Mockito.verify(query).bindValue("someDate", "someValue1");
        Mockito.verify(query).bindValue("someVal", "someValue2");
    }

}
