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
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProvider;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    private ModelBridge modelBridge;

    private ComponentManager componentManager;

    private NotificationPreference mockPreference11;
    private NotificationPreference mockPreference12;
    private NotificationPreference mockPreference21;
    private NotificationPreference mockPreference22;

    private NotificationPreferenceProvider mockPreferenceProvider1;
    private NotificationPreferenceProvider mockPreferenceProvider2;

    @Before
    public void setUp() throws Exception
    {
        modelBridge = mocker.registerMockComponent(ModelBridge.class, "cached");

        componentManager = mocker.registerMockComponent(ComponentManager.class);

        mockPreference11 = mock(NotificationPreference.class);
        when(mockPreference11.getFormat()).thenReturn(NotificationFormat.ALERT);
        when(mockPreference11.isNotificationEnabled()).thenReturn(true);
        when(mockPreference11.getProviderHint()).thenReturn("1");

        mockPreference12 = mock(NotificationPreference.class);
        when(mockPreference12.getFormat()).thenReturn(NotificationFormat.EMAIL);
        when(mockPreference12.isNotificationEnabled()).thenReturn(true);
        when(mockPreference12.getProviderHint()).thenReturn("1");

        mockPreference21 = mock(NotificationPreference.class);
        when(mockPreference21.getFormat()).thenReturn(NotificationFormat.ALERT);
        when(mockPreference21.isNotificationEnabled()).thenReturn(false);
        when(mockPreference21.getProviderHint()).thenReturn("2");

        mockPreference22 = mock(NotificationPreference.class);
        when(mockPreference22.getFormat()).thenReturn(NotificationFormat.EMAIL);
        when(mockPreference22.isNotificationEnabled()).thenReturn(false);
        when(mockPreference22.getProviderHint()).thenReturn("2");

        mockPreferenceProvider1 = mock(NotificationPreferenceProvider.class);
        mockPreferenceProvider2 = mock(NotificationPreferenceProvider.class);
    }

    private void mockPreferenceProviders(DocumentReference user) throws Exception
    {


        when(mockPreferenceProvider1.getPreferencesForUser(user))
                .thenReturn(Arrays.asList(mockPreference11, mockPreference12));
        when(mockPreferenceProvider2.getPreferencesForUser(user))
                .thenReturn(Arrays.asList(mockPreference21, mockPreference22));

        when(componentManager.hasComponent(any(), any())).thenReturn(true);

        when(componentManager.getInstance(any(), eq("1")))
                .thenReturn(mockPreferenceProvider1);
        when(componentManager.getInstance(any(), eq("2")))
                .thenReturn(mockPreferenceProvider2);

        when(componentManager.getInstanceList(NotificationPreferenceProvider.class))
                .thenReturn(Arrays.asList(mockPreferenceProvider1, mockPreferenceProvider2));
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

        assertEquals(1, preferences.size());
        assertTrue(preferences.contains(mockPreference12));

        preferences = mocker.getComponentUnderTest().getPreferences(user,
                false, NotificationFormat.EMAIL);

        assertEquals(1, preferences.size());
        assertTrue(preferences.contains(mockPreference22));
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
