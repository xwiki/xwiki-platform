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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.NotificationPreferenceProvider;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNotificationPreferenceManager}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@ComponentTest
class DefaultNotificationPreferenceManagerTest
{
    @InjectMockComponents
    private DefaultNotificationPreferenceManager manager;

    @MockComponent
    @Named("cached")
    private NotificationPreferenceModelBridge notificationPreferenceModelBridge;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private NotificationPreference mockPreference11;
    private NotificationPreference mockPreference12;
    private NotificationPreference mockPreference21;
    private NotificationPreference mockPreference22;
    private NotificationPreference mockPreference23;

    private NotificationPreferenceProvider mockPreferenceProvider1;
    private NotificationPreferenceProvider mockPreferenceProvider2;

    private class NotificationPreferenceImplementation extends AbstractNotificationPreference
    {
        NotificationPreferenceImplementation(boolean isNotificationEnabled, NotificationFormat format,
                NotificationPreferenceCategory category, Date startDate, String providerHint,
                Map<NotificationPreferenceProperty, Object> properties)
        {
            super(isNotificationEnabled, format, category, startDate, providerHint, properties);
        }
    }

    @BeforeEach
    void setUp()
    {
        Map<NotificationPreferenceProperty, Object> map1 = new HashMap<>();
        map1.put(NotificationPreferenceProperty.EVENT_TYPE, "update");
        Map<NotificationPreferenceProperty, Object> map2 = new HashMap<>();
        map1.put(NotificationPreferenceProperty.EVENT_TYPE, "addComment");
        this.mockPreference11 = new NotificationPreferenceImplementation(true, NotificationFormat.ALERT,
                NotificationPreferenceCategory.DEFAULT, null, "1", map1);
        this.mockPreference12 = new NotificationPreferenceImplementation(true, NotificationFormat.EMAIL,
                NotificationPreferenceCategory.DEFAULT, null, "1", map1);
        this.mockPreference21 = new NotificationPreferenceImplementation(false, NotificationFormat.ALERT,
                NotificationPreferenceCategory.DEFAULT, null, "2", map2);
        this.mockPreference22 = new NotificationPreferenceImplementation(false, NotificationFormat.EMAIL,
                NotificationPreferenceCategory.DEFAULT, null, "2", map2);
        this.mockPreference23 = new NotificationPreferenceImplementation(false, NotificationFormat.EMAIL,
                NotificationPreferenceCategory.DEFAULT, null, "2", map1);

        this.mockPreferenceProvider1 = mock(NotificationPreferenceProvider.class);
        this.mockPreferenceProvider2 = mock(NotificationPreferenceProvider.class);
    }

    private void mockPreferenceProviders(DocumentReference user) throws Exception
    {
        when(this.mockPreferenceProvider1.getPreferencesForUser(user))
                .thenReturn(List.of(this.mockPreference11, this.mockPreference12));
        when(this.mockPreferenceProvider2.getPreferencesForUser(user))
                .thenReturn(List.of(this.mockPreference21, this.mockPreference22, this.mockPreference23));

        this.componentManager.registerComponent(NotificationPreferenceProvider.class, "1",
                this.mockPreferenceProvider1);
        this.componentManager.registerComponent(NotificationPreferenceProvider.class, "2",
                this.mockPreferenceProvider2);

        when(this.mockPreferenceProvider1.getProviderPriority()).thenReturn(100);
        when(this.mockPreferenceProvider2.getProviderPriority()).thenReturn(200);
    }

    @Test
    void getNotificationPreferences() throws Exception
    {
        DocumentReference user = new DocumentReference("xwiki", "test", "user");

        mockPreferenceProviders(user);

        List<NotificationPreference> preferences = this.manager.getAllPreferences(user);

        assertEquals(4, preferences.size());
        assertTrue(preferences.contains(this.mockPreference11));
        assertTrue(preferences.contains(this.mockPreference12));
        assertTrue(preferences.contains(this.mockPreference21));
        assertTrue(preferences.contains(this.mockPreference22));
    }

    @Test
    void getNotificationPreferencesWithAdditionalParameters() throws Exception
    {
        DocumentReference user = new DocumentReference("xwiki", "test", "user");

        mockPreferenceProviders(user);

        List<NotificationPreference> preferences = this.manager.getPreferences(user,
                true, NotificationFormat.ALERT);

        assertEquals(1, preferences.size());
        assertTrue(preferences.contains(this.mockPreference11));

        preferences = this.manager.getPreferences(user,
                false, NotificationFormat.ALERT);

        assertEquals(1, preferences.size());
        assertTrue(preferences.contains(this.mockPreference21));

        preferences = this.manager.getPreferences(user,
                true, NotificationFormat.EMAIL);

        assertEquals(0, preferences.size());

        preferences = this.manager.getPreferences(user,
                false, NotificationFormat.EMAIL);

        assertEquals(2, preferences.size());
        assertTrue(preferences.contains(this.mockPreference22));
        assertTrue(preferences.contains(this.mockPreference23));
    }

    @Test
    void saveNotificationsPreferences() throws Exception
    {
        mockPreferenceProviders(new DocumentReference("xwiki", "test", "user"));

        this.manager.savePreferences(
                List.of(this.mockPreference11, this.mockPreference12, this.mockPreference21, this.mockPreference22));

        verify(this.mockPreferenceProvider1)
            .savePreferences(eq(List.of(this.mockPreference11, this.mockPreference12)));
    }
}
