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
package org.xwiki.notifications.preferences.script;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationPreferenceScriptService}.
 *
 * @since 9.7RC1
 * @version $Id$
 */
public class NotificationPreferenceScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<NotificationPreferenceScriptService> mocker =
            new MockitoComponentMockingRule<>(NotificationPreferenceScriptService.class);

    private NotificationPreferenceManager notificationPreferenceManager;
    private DocumentAccessBridge documentAccessBridge;

    @Before
    public void setUp() throws Exception
    {
        notificationPreferenceManager = mocker.getInstance(NotificationPreferenceManager.class);
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
    }

    @Test
    public void test() throws Exception
    {
        DocumentReference userRef = new DocumentReference("xwiki", "XWiki", "UserA");
        mocker.getComponentUnderTest().saveNotificationPreferences(
                IOUtils.toString(getClass().getResourceAsStream("/preferences.json")), userRef);

        verify(notificationPreferenceManager, times(1)).savePreferences(
                any(List.class));
    }

    @Test
    public void isEventTypeEnabled() throws Exception
    {
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");
        when(documentAccessBridge.getCurrentUserReference()).thenReturn(user);

        when(notificationPreferenceManager.getAllPreferences(user)).thenReturn(Collections.emptyList());
        assertFalse(mocker.getComponentUnderTest().isEventTypeEnabled("update", NotificationFormat.ALERT));

        NotificationPreference pref1 = mock(NotificationPreference.class);
        NotificationPreference pref2 = mock(NotificationPreference.class);

        when(pref1.getFormat()).thenReturn(NotificationFormat.EMAIL);
        Map<NotificationPreferenceProperty, Object> properties1 = new HashMap<>();
        properties1.put(NotificationPreferenceProperty.EVENT_TYPE, "update");
        when(pref1.getProperties()).thenReturn(properties1);

        when(pref2.getFormat()).thenReturn(NotificationFormat.ALERT);
        Map<NotificationPreferenceProperty, Object> properties2 = new HashMap<>();
        properties2.put(NotificationPreferenceProperty.EVENT_TYPE, "update");
        when(pref2.getProperties()).thenReturn(properties2);
        when(pref2.isNotificationEnabled()).thenReturn(true);

        when(notificationPreferenceManager.getAllPreferences(user)).thenReturn(Arrays.asList(pref1, pref2));
        assertTrue(mocker.getComponentUnderTest().isEventTypeEnabled("update", NotificationFormat.ALERT));
        assertFalse(mocker.getComponentUnderTest().isEventTypeEnabled("update", NotificationFormat.EMAIL));
    }

    @Test
    public void isEventTypeEnabledForWiki() throws Exception
    {
        WikiReference wiki = new WikiReference("whatever");

        when(notificationPreferenceManager.getAllPreferences(wiki)).thenReturn(Collections.emptyList());
        assertFalse(mocker.getComponentUnderTest().isEventTypeEnabled("update", NotificationFormat.ALERT,
                wiki.getName()));

        NotificationPreference pref1 = mock(NotificationPreference.class);
        NotificationPreference pref2 = mock(NotificationPreference.class);

        when(pref1.getFormat()).thenReturn(NotificationFormat.EMAIL);
        Map<NotificationPreferenceProperty, Object> properties1 = new HashMap<>();
        properties1.put(NotificationPreferenceProperty.EVENT_TYPE, "update");
        when(pref1.getProperties()).thenReturn(properties1);

        when(pref2.getFormat()).thenReturn(NotificationFormat.ALERT);
        Map<NotificationPreferenceProperty, Object> properties2 = new HashMap<>();
        properties2.put(NotificationPreferenceProperty.EVENT_TYPE, "update");
        when(pref2.getProperties()).thenReturn(properties2);
        when(pref2.isNotificationEnabled()).thenReturn(true);

        when(notificationPreferenceManager.getAllPreferences(wiki)).thenReturn(Arrays.asList(pref1, pref2));
        assertTrue(mocker.getComponentUnderTest().isEventTypeEnabled("update", NotificationFormat.ALERT,
                wiki.getName()));
        assertFalse(mocker.getComponentUnderTest().isEventTypeEnabled("update", NotificationFormat.EMAIL,
                wiki.getName()));
    }
}
