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
package org.xwiki.notifications.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.collections.map.HashedMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFilter;
import org.xwiki.notifications.NotificationPreference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    private ModelBridge modelBridge;
    private EntityReferenceSerializer<String> serializer;
    private ConfigurationSource userPreferencesSource;
    private WikiDescriptorManager wikiDescriptorManager;
    private NotificationFilterManager notificationFilterManager;

    private DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "UserA");
    private Query query;
    private Date startDate;

    @Before
    public void setUp() throws Exception
    {
        queryManager = mocker.getInstance(QueryManager.class);
        modelBridge = mocker.getInstance(ModelBridge.class, "cached");
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        userPreferencesSource = mocker.getInstance(ConfigurationSource.class, "user");
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        notificationFilterManager = mocker.getInstance(NotificationFilterManager.class);

        startDate = new Date(10);

        query = mock(Query.class);
        when(queryManager.createQuery(anyString(), anyString())).thenReturn(query);

        when(modelBridge.getUserStartDate(userReference)).thenReturn(startDate);
        when(serializer.serialize(userReference)).thenReturn("xwiki:XWiki.UserA");

        NotificationPreference pref1 = new NotificationPreference("create", null, true);
        when(modelBridge.getNotificationsPreferences(userReference)).thenReturn(Arrays.asList(pref1));

        when(userPreferencesSource.getProperty("displayHiddenDocuments", 0)).thenReturn(0);

        when(wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
    }

    @Test
    public void generateQuery() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                true, null, null);

        // Verify
        verify(queryManager).createQuery(
                "where event.date >= :startDate AND event.user <> :user AND (event.type IN (:types))" +
                        " AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(eq("types"), eq(Arrays.asList("create")));
        verify(query).bindValue("startDate", startDate);
    }

    @Test
    public void generateQueryWhenHiddenDocsAreEnabled() throws Exception
    {
        // Mock
        when(userPreferencesSource.getProperty("displayHiddenDocuments", 0)).thenReturn(1);

        // Test
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                true, null, null);

        // Verify
        verify(queryManager).createQuery(
                "where event.date >= :startDate AND event.user <> :user AND (event.type IN (:types))" +
                        " AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(eq("types"), eq(Arrays.asList("create")));
        verify(query).bindValue("startDate", startDate);
    }

    @Test
    public void generateQueryWithNotOnlyUnread() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                false, null, null);

        // Verify
        verify(queryManager).createQuery(
                "where event.date >= :startDate AND event.user <> :user AND (event.type IN (:types))" +
                        " AND event.hidden <> true " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(eq("types"), eq(Arrays.asList("create")));
        verify(query).bindValue("startDate", startDate);
    }

    @Test
    public void generateQueryWithUntilDate() throws Exception
    {
        Date untilDate = new Date();

        // Test
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                true, untilDate, Collections.emptyList());

        // Verify
        verify(queryManager).createQuery(
                "where event.date >= :startDate AND event.user <> :user AND (event.type IN (:types))" +
                        " AND event.date <= :endDate AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(eq("types"), eq(Arrays.asList("create")));
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
                true, untilDate, Arrays.asList("event1", "event2"));

        // Verify
        verify(queryManager).createQuery(
                "where event.date >= :startDate AND event.user <> :user AND (event.type IN (:types))" +
                        " AND event.id NOT IN (:blackList) AND event.date <= :endDate AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(eq("types"), eq(Arrays.asList("create")));
        verify(query).bindValue("startDate", startDate);
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
                true, null, null);

        // Verify
        verify(queryManager).createQuery(
                "where event.date >= :startDate AND event.user <> :user AND (event.type IN (:types))" +
                        " AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true))" +
                        " AND event.wiki = :userWiki " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(eq("types"), eq(Arrays.asList("create")));
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
        when(notificationFilterManager.getAllNotificationFilters(any(DocumentReference.class))).thenReturn(
                Arrays.asList(notificationFilter1, notificationFilter2)
        );

        when(notificationFilter1.queryFilterOR(any(DocumentReference.class))).thenReturn("event.date = :someDate");
        when(notificationFilter2.queryFilterOR(any(DocumentReference.class))).thenReturn("event.eventType = :someVal");

        when(notificationFilter1.queryFilterAND(any(DocumentReference.class))).thenReturn("1=1");
        when(notificationFilter2.queryFilterAND(any(DocumentReference.class))).thenReturn("2=2");


        when(notificationFilter1.queryFilterParams(any(DocumentReference.class))).thenReturn(new HashedMap() {{
            put("someDate", "someValue1");
        }});
        when(notificationFilter2.queryFilterParams(any(DocumentReference.class))).thenReturn(new HashedMap() {{
            put("someVal", "someValue2");
        }});

        // Test
        mocker.getComponentUnderTest().generateQuery(
                new DocumentReference("xwiki", "XWiki", "UserA"),
                true, untilDate, Arrays.asList("event1", "event2"));

        // Verify
        verify(queryManager).createQuery(
                "where event.date >= :startDate AND event.user <> :user AND (event.type IN (:types))" +
                        " AND (event.date = :someDate OR event.eventType = :someVal)" +
                        " AND 1=1 AND 2=2" +
                        " AND event.id NOT IN (:blackList) AND event.date <= :endDate AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(eq("types"), eq(Arrays.asList("create")));
        verify(query).bindValue("startDate", startDate);
        verify(query).bindValue("endDate", untilDate);
        verify(query).bindValue("blackList", Arrays.asList("event1", "event2"));
        verify(query).bindValue("someDate", "someValue1");
        verify(query).bindValue("someVal", "someValue2");
    }

}
