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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.NotificationPreferenceProvider;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNotificationPreferenceManager}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public class DefaultNotificationPreferenceManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<DefaultNotificationPreferenceManager> mocker =
            new MockitoComponentMockingRule<>(DefaultNotificationPreferenceManager.class);

    private NotificationPreferenceModelBridge notificationPreferenceModelBridge;

    private NotificationPreference mockPreference11;
    private NotificationPreference mockPreference12;
    private NotificationPreference mockPreference21;
    private NotificationPreference mockPreference22;
    private NotificationPreference mockPreference23;

    private NotificationPreferenceProvider mockPreferenceProvider1;
    private NotificationPreferenceProvider mockPreferenceProvider2;

    private class NotificationPreferenceImplementation extends AbstractNotificationPreference
    {
        public NotificationPreferenceImplementation(boolean isNotificationEnabled, NotificationFormat format,
                NotificationPreferenceCategory category, Date startDate, String providerHint,
                Map<NotificationPreferenceProperty, Object> properties)
        {
            super(isNotificationEnabled, format, category, startDate, providerHint, properties);
        }
    }

    @Before
    public void setUp() throws Exception
    {
        notificationPreferenceModelBridge = mocker.registerMockComponent(NotificationPreferenceModelBridge.class, "cached");

        Map<NotificationPreferenceProperty, Object> map1 = new HashMap<>();
        map1.put(NotificationPreferenceProperty.EVENT_TYPE, "update");
        Map<NotificationPreferenceProperty, Object> map2 = new HashMap<>();
        map1.put(NotificationPreferenceProperty.EVENT_TYPE, "addComment");
        mockPreference11 = new NotificationPreferenceImplementation(true, NotificationFormat.ALERT,
                NotificationPreferenceCategory.DEFAULT, null, "1", map1);
        mockPreference12 = new NotificationPreferenceImplementation(true, NotificationFormat.EMAIL,
                NotificationPreferenceCategory.DEFAULT, null, "1", map1);
        mockPreference21 = new NotificationPreferenceImplementation(false, NotificationFormat.ALERT,
                NotificationPreferenceCategory.DEFAULT, null, "2", map2);
        mockPreference22 = new NotificationPreferenceImplementation(false, NotificationFormat.EMAIL,
                NotificationPreferenceCategory.DEFAULT, null, "2", map2);
        mockPreference23 = new NotificationPreferenceImplementation(false, NotificationFormat.EMAIL,
                NotificationPreferenceCategory.DEFAULT, null, "2", map1);

        mockPreferenceProvider1 = mock(NotificationPreferenceProvider.class);
        mockPreferenceProvider2 = mock(NotificationPreferenceProvider.class);
    }

    private void mockPreferenceProviders(DocumentReference user) throws Exception
    {
        when(mockPreferenceProvider1.getPreferencesForUser(user))
                .thenReturn(Arrays.asList(mockPreference11, mockPreference12));
        when(mockPreferenceProvider2.getPreferencesForUser(user))
                .thenReturn(Arrays.asList(mockPreference21, mockPreference22, mockPreference23));

        mocker.registerComponent(NotificationPreferenceProvider.class, "1", mockPreferenceProvider1);
        mocker.registerComponent(NotificationPreferenceProvider.class, "2", mockPreferenceProvider2);

        when(mockPreferenceProvider1.getProviderPriority()).thenReturn(100);
        when(mockPreferenceProvider2.getProviderPriority()).thenReturn(200);
    }

    @Test
    public void getNotificationPreferences() throws Exception
    {
        DocumentReference user = new DocumentReference("xwiki", "test", "user");

        mockPreferenceProviders(user);

        List<NotificationPreference> preferences = mocker.getComponentUnderTest().getAllPreferences(user);

        assertEquals(4, preferences.size());
        assertTrue(preferences.contains(mockPreference11));
        assertTrue(preferences.contains(mockPreference12));
        assertTrue(preferences.contains(mockPreference21));
        assertTrue(preferences.contains(mockPreference22));
    }

    @Test
    public void getNotificationPreferencesWithAdditionalParameters() throws Exception
    {
        DocumentReference user = new DocumentReference("xwiki", "test", "user");

        mockPreferenceProviders(user);

        List<NotificationPreference> preferences = mocker.getComponentUnderTest().getPreferences(user,
                true, NotificationFormat.ALERT);

        assertEquals(1, preferences.size());
        assertTrue(preferences.contains(mockPreference11));

        preferences = mocker.getComponentUnderTest().getPreferences(user,
                false, NotificationFormat.ALERT);

        assertEquals(1, preferences.size());
        assertTrue(preferences.contains(mockPreference21));

        preferences = mocker.getComponentUnderTest().getPreferences(user,
                true, NotificationFormat.EMAIL);

        assertEquals(0, preferences.size());

        preferences = mocker.getComponentUnderTest().getPreferences(user,
                false, NotificationFormat.EMAIL);

        assertEquals(2, preferences.size());
        assertTrue(preferences.contains(mockPreference22));
        assertTrue(preferences.contains(mockPreference23));
    }

    @Test
    public void saveNotificationsPreferences() throws Exception
    {
        mockPreferenceProviders(new DocumentReference("xwiki", "test", "user"));

        mocker.getComponentUnderTest()
                .savePreferences(
                        Arrays.asList(mockPreference11, mockPreference12, mockPreference21, mockPreference22));

        verify(mockPreferenceProvider1, times(1))
                .savePreferences(eq(Arrays.asList(mockPreference11, mockPreference12)));
    }
}
