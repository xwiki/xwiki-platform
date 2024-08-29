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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.internal.document.DocumentUserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationPreferenceScriptService}.
 *
 * @since 9.7RC1
 * @version $Id$
 */
@ComponentList({
    DefaultTargetableNotificationPreferenceBuilder.class
})
@ComponentTest
class NotificationPreferenceScriptServiceTest
{
    @InjectMockComponents
    private NotificationPreferenceScriptService scriptService;

    @MockComponent
    private NotificationPreferenceManager notificationPreferenceManager;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @MockComponent
    private Provider<TargetableNotificationPreferenceBuilder> targetableNotificationPreferenceBuilderProvider;

    @BeforeEach
    void setUp(MockitoComponentManager componentManager) throws Exception
    {

        when(targetableNotificationPreferenceBuilderProvider.get())
            .thenReturn(componentManager.getInstance(TargetableNotificationPreferenceBuilder.class));
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
    void saveNotificationPreferences() throws Exception
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

        this.scriptService.saveNotificationPreferences(
                IOUtils.toString(getClass().getResourceAsStream("/preferences.json")), userRef);

        assertTrue(isOk.booleanValue());
    }

    @Test
    void isEventTypeEnabledForUser() throws Exception
    {
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");
        when(documentAccessBridge.getCurrentUserReference()).thenReturn(user);
        DocumentUserReference documentUserReference = new DocumentUserReference(user, true);

        when(notificationPreferenceManager.getAllPreferences(user)).thenReturn(Collections.emptyList());
        assertTrue(this.scriptService
            .isEventTypeEnabledForUser("update", NotificationFormat.ALERT, documentUserReference));

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

        assertTrue(this.scriptService
            .isEventTypeEnabledForUser("update", NotificationFormat.ALERT, documentUserReference));
        assertFalse(this.scriptService
            .isEventTypeEnabledForUser("update", NotificationFormat.EMAIL, documentUserReference));
        verify(this.documentAccessBridge, never()).getCurrentUserReference();

        assertTrue(this.scriptService.isEventTypeEnabledForUser("update", NotificationFormat.ALERT,
            CurrentUserReference.INSTANCE));
        assertFalse(this.scriptService.isEventTypeEnabledForUser("update", NotificationFormat.EMAIL,
            CurrentUserReference.INSTANCE));

        assertTrue(this.scriptService.isEventTypeEnabled("update", NotificationFormat.ALERT));
        assertFalse(this.scriptService.isEventTypeEnabled("update", NotificationFormat.EMAIL));
        verify(this.documentAccessBridge, times(4)).getCurrentUserReference();

        NotificationException notificationException = assertThrows(NotificationException.class,
            () -> this.scriptService.isEventTypeEnabledForUser("update", NotificationFormat.ALERT, new UserReference()
            {
                @Override
                public boolean isGlobal()
                {
                    return false;
                }
            }));
        assertEquals("The method isEventTypeEnabledForUser should only be used with DocumentUserReference, "
            + "the given reference was a []", notificationException.getMessage());
    }

    @Test
    void isEventTypeEnabledForWiki() throws Exception
    {
        WikiReference wiki = new WikiReference("whatever");

        when(notificationPreferenceManager.getAllPreferences(wiki)).thenReturn(Collections.emptyList());
        assertTrue(this.scriptService.isEventTypeEnabled("update", NotificationFormat.ALERT,
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
        assertTrue(this.scriptService.isEventTypeEnabled("update", NotificationFormat.ALERT,
                wiki.getName()));
        assertFalse(this.scriptService.isEventTypeEnabled("update", NotificationFormat.EMAIL,
                wiki.getName()));
    }

    @Test
    void saveNotificationPreferencesForCurrentWikiWithoutRight() throws Exception
    {
        when(documentAccessBridge.getCurrentDocumentReference()).thenReturn(
                new DocumentReference("wikiA", "SpaceA", "PageA"));
        AccessDeniedException e = mock(AccessDeniedException.class);
        doThrow(e).when(authorizationManager).checkAccess(Right.ADMIN, new WikiReference("wikiA"));

        String json = "";
        Exception caughtException = null;
        try {
            this.scriptService.saveNotificationPreferencesForCurrentWiki(json);
        } catch (Exception ex) {
            caughtException = ex;
        }

        assertNotNull(caughtException);
        assertEquals(e, caughtException);
    }

    @Test
    void saveNotificationPreferencesWithoutRight() throws Exception
    {
        DocumentReference userDoc = new DocumentReference("wikiA", "SpaceA", "UserA");
        AccessDeniedException e = mock(AccessDeniedException.class);
        doThrow(e).when(authorizationManager).checkAccess(Right.EDIT, userDoc);

        String json = "";
        Exception caughtException = null;
        try {
            this.scriptService.saveNotificationPreferences(json, userDoc);
        } catch (Exception ex) {
            caughtException = ex;
        }

        assertNotNull(caughtException);
        assertEquals(e, caughtException);
    }
}
