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

import javax.inject.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.TargetableNotificationPreferenceBuilder;
import org.xwiki.notifications.preferences.internal.AbstractNotificationPreference;
import org.xwiki.notifications.preferences.internal.DefaultTargetableNotificationPreferenceBuilder;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationPreferenceScriptService}.
 *
 * @since 9.7RC1
 * @version $Id$
 */
@ComponentList(TargetableNotificationPreferenceBuilder.class)
public class NotificationPreferenceScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<NotificationPreferenceScriptService> mocker =
            new MockitoComponentMockingRule<>(NotificationPreferenceScriptService.class);

    private NotificationPreferenceManager notificationPreferenceManager;
    private DocumentAccessBridge documentAccessBridge;
    private ContextualAuthorizationManager authorizationManager;
    private Provider<TargetableNotificationPreferenceBuilder> targetableNotificationPreferenceBuilderProvider;

    @Before
    public void setUp() throws Exception
    {
        notificationPreferenceManager = mocker.getInstance(NotificationPreferenceManager.class);
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        authorizationManager = mocker.getInstance(ContextualAuthorizationManager.class);
        targetableNotificationPreferenceBuilderProvider = mock(Provider.class);
        when(targetableNotificationPreferenceBuilderProvider.get())
                .thenReturn(new DefaultTargetableNotificationPreferenceBuilder());
        mocker.registerComponent(new DefaultParameterizedType(null, Provider.class,
                TargetableNotificationPreferenceBuilder.class), targetableNotificationPreferenceBuilderProvider);
    }

    private class NotificationPreferenceImpl extends AbstractNotificationPreference
    {
        private NotificationPreferenceImpl(boolean isNotificationEnabled, NotificationFormat format,
                String eventType)
        {
            super(isNotificationEnabled, format, null, null, null, new HashMap<>());
            properties.put(NotificationPreferenceProperty.EVENT_TYPE, eventType);
        }
    }

    @Test
    public void saveNotificationPreferences() throws Exception
    {
        DocumentReference userRef = new DocumentReference("xwiki", "XWiki", "UserA");

        NotificationPreferenceImpl existingPref1 = new NotificationPreferenceImpl(true,
                NotificationFormat.ALERT, "create");
        NotificationPreferenceImpl existingPref2 = new NotificationPreferenceImpl(true,
                NotificationFormat.EMAIL, "update");
        NotificationPreferenceImpl existingPref3 = new NotificationPreferenceImpl(false,
                NotificationFormat.EMAIL, "delete");

        when(notificationPreferenceManager.getAllPreferences(eq(userRef))).thenReturn(
                Arrays.asList(existingPref1, existingPref2, existingPref3));

        final MutableBoolean isOk = new MutableBoolean(false);
        doAnswer(invocationOnMock -> {
                List<NotificationPreference> prefsToSave = invocationOnMock.getArgument(0);
                // 1 of the preferences contained in the JSON file should be saved because the inherited preference
                // is the same
                assertEquals(9, prefsToSave.size());

                assertTrue(prefsToSave.contains(existingPref1));
                assertTrue(prefsToSave.contains(existingPref2));
                assertFalse(prefsToSave.contains(existingPref3));

                isOk.setTrue();
                return true;
        }).when(notificationPreferenceManager).savePreferences(any(List.class));

        mocker.getComponentUnderTest().saveNotificationPreferences(
                IOUtils.toString(getClass().getResourceAsStream("/preferences.json")), userRef);

        assertTrue(isOk.booleanValue());
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

    @Test
    public void saveNotificationPreferencesForCurrentWikiWithoutRight() throws Exception
    {
        when(documentAccessBridge.getCurrentDocumentReference()).thenReturn(
                new DocumentReference("wikiA", "SpaceA", "PageA"));
        AccessDeniedException e = mock(AccessDeniedException.class);
        doThrow(e).when(authorizationManager).checkAccess(Right.ADMIN, new WikiReference("wikiA"));

        String json = "";
        Exception caughtException = null;
        try {
            mocker.getComponentUnderTest().saveNotificationPreferencesForCurrentWiki(json);
        } catch (Exception ex) {
            caughtException = ex;
        }

        assertNotNull(caughtException);
        assertEquals(e, caughtException);
    }

    @Test
    public void saveNotificationPreferencesWithoutRight() throws Exception
    {
        DocumentReference userDoc = new DocumentReference("wikiA", "SpaceA", "UserA");
        AccessDeniedException e = mock(AccessDeniedException.class);
        doThrow(e).when(authorizationManager).checkAccess(Right.EDIT, userDoc);

        String json = "";
        Exception caughtException = null;
        try {
            mocker.getComponentUnderTest().saveNotificationPreferences(json, userDoc);
        } catch (Exception ex) {
            caughtException = ex;
        }

        assertNotNull(caughtException);
        assertEquals(e, caughtException);
    }
}
