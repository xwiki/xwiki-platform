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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.notifications.sources.ParametrizedNotificationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNotificationManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultNotificationManagerTest
{
    private static final String USER_ID = "xwiki:XWiki.TestUser";

    private static final DocumentReference USER_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "TestUser");

    @InjectMockComponents
    private DefaultNotificationManager notificationManager;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private NotificationPreferenceManager notificationPreferenceManager;

    @MockComponent
    private ParametrizedNotificationManager parametrizedNotificationManager;

    @MockComponent
    private DefaultNotificationParametersFactory parametersFactory;

    private final List<CompositeEvent> expected = List.of(mock(CompositeEvent.class));

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.documentReferenceResolver.resolve(USER_ID)).thenReturn(USER_REFERENCE);
        when(this.parametrizedNotificationManager.getEvents(any())).thenReturn(this.expected);
    }

    @Test
    void getEventsWithUserAndCount() throws Exception
    {
        NotificationParameters notificationParameters = new NotificationParameters();
        notificationParameters.user = USER_REFERENCE;
        notificationParameters.format = NotificationFormat.ALERT;
        notificationParameters.expectedCount = 10;
        notificationParameters.endDate = null;
        notificationParameters.endDateIncluded = true;
        notificationParameters.fromDate = null;
        notificationParameters.blackList = List.of();

        when(this.parametrizedNotificationManager.getEvents(notificationParameters)).thenReturn(this.expected);

        List<CompositeEvent> result = this.notificationManager.getEvents(USER_ID, 10);

        assertSame(this.expected, result);
        verify(this.parametrizedNotificationManager).getEvents(notificationParameters);
    }

    @Test
    void getEventsWithUntilDateAndBlackList() throws Exception
    {
        Date untilDate = new Date();
        List<String> blackList = List.of("event1", "event2");

        NotificationParameters notificationParameters = new NotificationParameters();
        notificationParameters.user = USER_REFERENCE;
        notificationParameters.format = NotificationFormat.ALERT;
        notificationParameters.expectedCount = 5;
        notificationParameters.endDate = untilDate;
        notificationParameters.endDateIncluded = true;
        notificationParameters.fromDate = null;
        notificationParameters.blackList = blackList;
        when(this.parametrizedNotificationManager.getEvents(notificationParameters)).thenReturn(this.expected);

        List<CompositeEvent> result = this.notificationManager.getEvents(USER_ID, 5, untilDate, blackList);

        assertSame(this.expected, result);
        verify(this.parametrizedNotificationManager).getEvents(notificationParameters);
    }

    @Test
    void getEventsWithFromDateAndUntilDate() throws Exception
    {
        Date untilDate = new Date();
        Date fromDate = new Date();

        NotificationParameters notificationParameters = new NotificationParameters();
        notificationParameters.user = USER_REFERENCE;
        notificationParameters.format = NotificationFormat.ALERT;
        notificationParameters.expectedCount = 10;
        notificationParameters.endDate = untilDate;
        notificationParameters.endDateIncluded = true;
        notificationParameters.fromDate = fromDate;
        notificationParameters.blackList = List.of();
        when(this.parametrizedNotificationManager.getEvents(notificationParameters)).thenReturn(this.expected);

        List<CompositeEvent> result =
            this.notificationManager.getEvents(USER_ID, 10, untilDate, fromDate, List.of());

        assertSame(this.expected, result);
        verify(this.parametrizedNotificationManager).getEvents(notificationParameters);
    }

    @Test
    void getEventsWithExplicitFormat() throws Exception
    {
        Date untilDate = new Date();
        Date fromDate = new Date();

        NotificationParameters notificationParameters = new NotificationParameters();
        notificationParameters.user = USER_REFERENCE;
        notificationParameters.format = NotificationFormat.EMAIL;
        notificationParameters.expectedCount = 3;
        notificationParameters.endDate = untilDate;
        notificationParameters.endDateIncluded = true;
        notificationParameters.fromDate = fromDate;
        notificationParameters.blackList = List.of();
        when(this.parametrizedNotificationManager.getEvents(notificationParameters)).thenReturn(this.expected);

        List<CompositeEvent> result = this.notificationManager.getEvents(
            USER_ID, NotificationFormat.EMAIL, 3, untilDate, fromDate, List.of());

        assertSame(this.expected, result);
        verify(this.parametrizedNotificationManager).getEvents(notificationParameters);
    }

    @Test
    void getEventsWithUntilDateIncludedFlag() throws Exception
    {
        Date untilDate = new Date();

        NotificationParameters notificationParameters = new NotificationParameters();
        notificationParameters.user = USER_REFERENCE;
        notificationParameters.format = NotificationFormat.ALERT;
        notificationParameters.expectedCount = 5;
        notificationParameters.endDate = untilDate;
        notificationParameters.endDateIncluded = false;
        notificationParameters.fromDate = null;
        notificationParameters.blackList = List.of();

        List<CompositeEvent> result = this.notificationManager.getEvents(
            USER_ID, NotificationFormat.ALERT, 5, untilDate, false, null, List.of());

        assertSame(this.expected, result);
        verify(this.parametrizedNotificationManager).getEvents(notificationParameters);
    }

    @Test
    void getEventsWithUntilDateIncluded() throws Exception
    {
        Date untilDate = new Date();
        NotificationParameters notificationParameters = new NotificationParameters();
        notificationParameters.user = USER_REFERENCE;
        notificationParameters.format = NotificationFormat.ALERT;
        notificationParameters.expectedCount = 5;
        notificationParameters.endDate = untilDate;
        notificationParameters.endDateIncluded = true;
        notificationParameters.fromDate = null;
        notificationParameters.blackList = List.of();

        this.notificationManager.getEvents(
            USER_ID, NotificationFormat.ALERT, 5, untilDate, true, null, List.of());

        verify(this.parametrizedNotificationManager).getEvents(notificationParameters);
    }

    @Test
    void getEventsCountReturnsListSize() throws Exception
    {
        List<CompositeEvent> events = List.of(
            mock(CompositeEvent.class),
            mock(CompositeEvent.class),
            mock(CompositeEvent.class)
        );

        NotificationParameters notificationParameters = new NotificationParameters();
        notificationParameters.user = USER_REFERENCE;
        notificationParameters.format = NotificationFormat.ALERT;
        notificationParameters.expectedCount = 10;
        notificationParameters.onlyUnread = true;

        when(this.parametrizedNotificationManager.getEvents(any(NotificationParameters.class)))
            .thenReturn(events);

        long count = this.notificationManager.getEventsCount(USER_ID, 10);

        assertEquals(3, count);
        verify(this.parametrizedNotificationManager).getEvents(notificationParameters);
    }

    @Test
    void getEventsCountWithMaxCountZero() throws Exception
    {
        when(this.parametrizedNotificationManager.getEvents(any(NotificationParameters.class)))
            .thenReturn(List.of());

        long count = this.notificationManager.getEventsCount(USER_ID, 0);

        assertEquals(0, count);
    }

    @Test
    void getPreferencesForCurrentUser() throws Exception
    {
        DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "CurrentUser");
        List<NotificationPreference> prefs = List.of(mock(NotificationPreference.class));

        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(currentUser);
        when(this.notificationPreferenceManager.getAllPreferences(currentUser)).thenReturn(prefs);

        List<NotificationPreference> result = this.notificationManager.getPreferences();

        assertSame(prefs, result);
    }

    @Test
    void getPreferencesForUserId() throws Exception
    {
        List<NotificationPreference> prefs = List.of(mock(NotificationPreference.class));

        when(this.notificationPreferenceManager.getAllPreferences(USER_REFERENCE)).thenReturn(prefs);

        List<NotificationPreference> result = this.notificationManager.getPreferences(USER_ID);

        assertSame(prefs, result);
    }

    @Test
    void setStartDate() throws Exception
    {
        Date startDate = new Date();

        this.notificationManager.setStartDate(USER_ID, startDate);

        verify(this.notificationPreferenceManager).setStartDateForUser(USER_REFERENCE, startDate);
    }

    @Test
    void getEventsCallsParametersFactory() throws Exception
    {
        NotificationParameters notificationParameters = new NotificationParameters();
        notificationParameters.user = USER_REFERENCE;
        notificationParameters.format = NotificationFormat.ALERT;
        notificationParameters.expectedCount = 5;

        when(this.parametrizedNotificationManager.getEvents(any(NotificationParameters.class)))
            .thenReturn(List.of());

        this.notificationManager.getEvents(USER_ID, 5);

        verify(this.parametersFactory).useUserPreferences(notificationParameters);
    }
}