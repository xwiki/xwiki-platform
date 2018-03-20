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
package org.xwiki.notifications.preferences.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.TargetableNotificationPreferenceBuilder;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since 10.3RC1
 * @since 9.11.4
 */
public class XWikiEventTypesEnablerTest
{
    @Rule
    public final MockitoComponentMockingRule<XWikiEventTypesEnabler> mocker =
            new MockitoComponentMockingRule<>(XWikiEventTypesEnabler.class);

    private NotificationPreferenceManager notificationPreferenceManager;
    private DocumentAccessBridge documentAccessBridge;
    private Provider<TargetableNotificationPreferenceBuilder> targetableNotificationPreferenceBuilderProvider;
    private DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "UserA");

    @Before
    public void setUp() throws Exception
    {
        notificationPreferenceManager = mocker.getInstance(NotificationPreferenceManager.class);
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        targetableNotificationPreferenceBuilderProvider = mock(Provider.class);
        when(targetableNotificationPreferenceBuilderProvider.get())
                .thenReturn(new DefaultTargetableNotificationPreferenceBuilder());
        mocker.registerComponent(new DefaultParameterizedType(null, Provider.class,
                TargetableNotificationPreferenceBuilder.class), targetableNotificationPreferenceBuilderProvider);
        when(documentAccessBridge.getCurrentUserReference()).thenReturn(currentUser);
    }

    @Test
    public void whenNothingIsEnabled() throws Exception
    {

        doAnswer(invocationOnMock -> {
            List<NotificationPreference> preferencesToSave = invocationOnMock.getArgument(0);
            // Ensure there is a preference to save for each event type and each format = 8 items
            assertEquals(8, preferencesToSave.size());
            return null;
        }).when(notificationPreferenceManager).savePreferences(anyList());

        mocker.getComponentUnderTest().ensureXWikiNotificationsAreEnabled();
        verify(notificationPreferenceManager).savePreferences(anyList());
    }

    @Test
    public void whenOtherEventTypesIsEnabled() throws Exception
    {
        NotificationPreference pref = mock(NotificationPreference.class);
        when(pref.isNotificationEnabled()).thenReturn(true);
        Map<NotificationPreferenceProperty, Object> properties = new HashMap<>();
        properties.put(NotificationPreferenceProperty.EVENT_TYPE, "blog");
        when(pref.getProperties()).thenReturn(properties);
        when(notificationPreferenceManager.getAllPreferences(eq(currentUser))).thenReturn(Arrays.asList(pref));

        doAnswer(invocationOnMock -> {
            List<NotificationPreference> preferencesToSave = invocationOnMock.getArgument(0);
            // Ensure there is a preference to save for each event type and each format = 8 items
            assertEquals(8, preferencesToSave.size());
            return null;
        }).when(notificationPreferenceManager).savePreferences(anyList());

        mocker.getComponentUnderTest().ensureXWikiNotificationsAreEnabled();
        verify(notificationPreferenceManager).savePreferences(anyList());
    }

    @Test
    public void whenOneXWikiEventTypesIsEnabled() throws Exception
    {
        NotificationPreference pref = mock(NotificationPreference.class);
        when(pref.isNotificationEnabled()).thenReturn(true);
        Map<NotificationPreferenceProperty, Object> properties = new HashMap<>();
        properties.put(NotificationPreferenceProperty.EVENT_TYPE, "update");
        when(pref.getProperties()).thenReturn(properties);
        when(notificationPreferenceManager.getAllPreferences(eq(currentUser))).thenReturn(Arrays.asList(pref));

        mocker.getComponentUnderTest().ensureXWikiNotificationsAreEnabled();
        verify(notificationPreferenceManager, never()).savePreferences(anyList());
    }
}
