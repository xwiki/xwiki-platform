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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.preferences.TargetableNotificationPreference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserProfileNotificationPreferenceProvider}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public class UserProfileNotificationPreferenceProviderTest
{
    @Rule
    public final MockitoComponentMockingRule<UserProfileNotificationPreferenceProvider> mocker =
            new MockitoComponentMockingRule<>(UserProfileNotificationPreferenceProvider.class);

    private ModelBridge cachedModelBridge;

    @Before
    public void setUp() throws Exception
    {
        cachedModelBridge = mocker.registerMockComponent(ModelBridge.class, "cached");
    }

    @Test
    public void providerName() throws Exception
    {
        assertEquals("userProfile", UserProfileNotificationPreferenceProvider.NAME);
    }

    @Test
    public void getProviderPriority() throws Exception
    {
        assertEquals(500, mocker.getComponentUnderTest().getProviderPriority());
    }

    @Test
    public void savePreferencesWithTargetable() throws Exception
    {
        DocumentReference userReference = new DocumentReference("wiki", "space", "user");
        DocumentReference userReference2 = new DocumentReference("wiki", "space", "user2");

        TargetableNotificationPreference pref1 = mock(TargetableNotificationPreference.class);
        when(pref1.getTarget()).thenReturn(userReference);
        TargetableNotificationPreference pref2 = mock(TargetableNotificationPreference.class);
        when(pref2.getTarget()).thenReturn(userReference);
        TargetableNotificationPreference pref3 = mock(TargetableNotificationPreference.class);
        when(pref3.getTarget()).thenReturn(userReference2);

        mocker.getComponentUnderTest().savePreferences(Arrays.asList(pref1, pref2, pref3));

        verify(cachedModelBridge, times(1)).saveNotificationsPreferences(eq(userReference),
                any(List.class));
        verify(cachedModelBridge, times(1)).saveNotificationsPreferences(eq(userReference2),
                any(List.class));
    }
}
