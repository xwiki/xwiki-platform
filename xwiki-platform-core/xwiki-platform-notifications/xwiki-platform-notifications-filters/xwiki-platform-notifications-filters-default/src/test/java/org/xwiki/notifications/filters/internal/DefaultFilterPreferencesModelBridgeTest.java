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
package org.xwiki.notifications.filters.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultFilterPreferencesModelBridge}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultFilterPreferencesModelBridgeTest
{
    private static final LocalDocumentReference TOGGLEABLE_FILTER_PREFERENCE_CLASS =
        new LocalDocumentReference(Arrays.asList("XWiki", "Notifications", "Code"), "ToggleableFilterPreferenceClass");

    private static final String FIELD_FILTER_NAME = "filterName";

    private static final String FIELD_IS_ENABLED = "isEnabled";

    @InjectMockComponents
    private DefaultFilterPreferencesModelBridge defaultModelBridge;

    @MockComponent
    private NotificationFilterPreferenceStore notificationFilterPreferenceStore;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private final DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");
    private final WikiReference wikiReference = new WikiReference("foo");

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);
    }

    @Test
    void saveFilterPreferencesForUser() throws NotificationException
    {
        List<NotificationFilterPreference> filterPreferenceList = Arrays.asList(
            mock(NotificationFilterPreference.class),
            mock(NotificationFilterPreference.class)
        );
        this.defaultModelBridge.saveFilterPreferences(user, filterPreferenceList);
        verify(this.notificationFilterPreferenceStore).saveFilterPreferences(user, filterPreferenceList);
    }

    @Test
    void saveFilterPreferencesForWiki() throws NotificationException
    {
        List<NotificationFilterPreference> filterPreferenceList = Arrays.asList(
            mock(NotificationFilterPreference.class),
            mock(NotificationFilterPreference.class)
        );
        this.defaultModelBridge.saveFilterPreferences(wikiReference, filterPreferenceList);
        verify(this.notificationFilterPreferenceStore).saveFilterPreferences(wikiReference, filterPreferenceList);
    }

    @Test
    void setFilterPreferenceEnabledForUser() throws NotificationException
    {
        String filterPrefName = "filter1";
        NotificationFilterPreference filterPreference = mock(NotificationFilterPreference.class);
        when(this.notificationFilterPreferenceStore.getFilterPreference(this.user, filterPrefName))
            .thenReturn(filterPreference);
        when(filterPreference.isEnabled()).thenReturn(true);

        // the filter does not exist: nothing happens
        this.defaultModelBridge.setFilterPreferenceEnabled(this.user, "filter2", false);
        verify(this.notificationFilterPreferenceStore).getFilterPreference(this.user, "filter2");
        verify(this.notificationFilterPreferenceStore, never())
            .saveFilterPreferences(any(DocumentReference.class), any());

        // the filter exists but is already set to false: nothing happens
        this.defaultModelBridge.setFilterPreferenceEnabled(this.user, filterPrefName, true);
        verify(filterPreference).isEnabled();
        verify(filterPreference, never()).setEnabled(true);
        verify(this.notificationFilterPreferenceStore, never())
            .saveFilterPreferences(any(DocumentReference.class), any());

        this.defaultModelBridge.setFilterPreferenceEnabled(this.user, filterPrefName, false);
        verify(filterPreference).setEnabled(false);
        verify(this.notificationFilterPreferenceStore)
            .saveFilterPreferences(this.user, Collections.singletonList(filterPreference));
    }

    @Test
    void setFilterPreferenceEnabledForWiki() throws NotificationException
    {
        String filterPrefName = "filterA";
        NotificationFilterPreference filterPreference = mock(NotificationFilterPreference.class);
        when(this.notificationFilterPreferenceStore.getFilterPreference(this.wikiReference, filterPrefName))
            .thenReturn(filterPreference);
        when(filterPreference.isEnabled()).thenReturn(true);

        // the filter does not exist: nothing happens
        this.defaultModelBridge.setFilterPreferenceEnabled(this.wikiReference, "filter2", false);
        verify(this.notificationFilterPreferenceStore).getFilterPreference(this.wikiReference, "filter2");
        verify(this.notificationFilterPreferenceStore, never())
            .saveFilterPreferences(any(WikiReference.class), any());

        // the filter exists but is already set to false: nothing happens
        this.defaultModelBridge.setFilterPreferenceEnabled(this.wikiReference, filterPrefName, true);
        verify(filterPreference).isEnabled();
        verify(filterPreference, never()).setEnabled(true);
        verify(this.notificationFilterPreferenceStore, never())
            .saveFilterPreferences(any(WikiReference.class), any());

        this.defaultModelBridge.setFilterPreferenceEnabled(this.wikiReference, filterPrefName, false);
        verify(filterPreference).setEnabled(false);
        verify(this.notificationFilterPreferenceStore)
            .saveFilterPreferences(this.wikiReference, Collections.singletonList(filterPreference));
    }

    @Test
    void deleteFilterPreferencesWiki() throws Exception
    {
        this.defaultModelBridge.deleteFilterPreferences(this.wikiReference);
        verify(this.notificationFilterPreferenceStore).deleteFilterPreference(this.wikiReference);
    }

    @Test
    void deleteFilterPreferencesUser() throws Exception
    {
        DocumentReference deletedUserDocumentReference = new DocumentReference("xwiki", "wXWiki", "DeletedUser");
        this.defaultModelBridge.deleteFilterPreferences(deletedUserDocumentReference);
        verify(this.notificationFilterPreferenceStore).deleteFilterPreferences(deletedUserDocumentReference);
    }

    @Test
    void getToggleableFilterActivations() throws Exception
    {
        DocumentReference user = new DocumentReference("subwiki", "XWiki", "Foo");
        WikiReference currentWiki = mock(WikiReference.class, "currentWiki");
        XWikiContext context = mock(XWikiContext.class);
        when(context.getWikiReference()).thenReturn(currentWiki);
        when(this.contextProvider.get()).thenReturn(context);
        XWiki xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);

        XWikiDocument userDoc = mock(XWikiDocument.class, "userDoc");
        when(xwiki.getDocument(user, context)).thenReturn(userDoc);


        NotificationFilter filter1 = mock(NotificationFilter.class, "filter1");
        ToggleableNotificationFilter filter2 = mock(ToggleableNotificationFilter.class, "filter2");
        ToggleableNotificationFilter filter3 = mock(ToggleableNotificationFilter.class, "filter3");
        ToggleableNotificationFilter filter4 = mock(ToggleableNotificationFilter.class, "filter4");
        ToggleableNotificationFilter filter5 = mock(ToggleableNotificationFilter.class, "filter5");
        ToggleableNotificationFilter filter6 = mock(ToggleableNotificationFilter.class, "filter6");
        ToggleableNotificationFilter filter7 = mock(ToggleableNotificationFilter.class, "filter7");

        this.componentManager.registerComponent(NotificationFilter.class, "filter1", filter1);
        this.componentManager.registerComponent(NotificationFilter.class, "filter2", filter2);
        this.componentManager.registerComponent(NotificationFilter.class, "filter3", filter3);
        this.componentManager.registerComponent(NotificationFilter.class, "filter4", filter4);
        this.componentManager.registerComponent(NotificationFilter.class, "filter5", filter5);
        this.componentManager.registerComponent(NotificationFilter.class, "filter6", filter6);
        this.componentManager.registerComponent(NotificationFilter.class, "filter7", filter7);

        when(filter2.getName()).thenReturn("filter2");
        when(filter2.isEnabledByDefault()).thenReturn(true);

        when(filter3.getName()).thenReturn("filter3");
        when(filter3.isEnabledByDefault()).thenReturn(true);

        when(filter4.getName()).thenReturn("filter4");
        when(filter4.isEnabledByDefault()).thenReturn(true);

        when(filter5.getName()).thenReturn("filter5");
        when(filter5.isEnabledByDefault()).thenReturn(false);

        when(filter6.getName()).thenReturn("filter6");
        when(filter6.isEnabledByDefault()).thenReturn(false);

        when(filter7.getName()).thenReturn("filter7");
        when(filter7.isEnabledByDefault()).thenReturn(false);

        BaseObject filter2Obj = mock(BaseObject.class, "filter2Obj");
        BaseObject filter4Obj = mock(BaseObject.class, "filter4Obj");
        when(userDoc.getXObject(TOGGLEABLE_FILTER_PREFERENCE_CLASS, FIELD_FILTER_NAME,
            "filter2", false)).thenReturn(filter2Obj);
        when(filter2Obj.getNumber()).thenReturn(2);
        when(userDoc.getXObject(TOGGLEABLE_FILTER_PREFERENCE_CLASS, FIELD_FILTER_NAME,
            "filter4", false)).thenReturn(filter4Obj);
        when(filter4Obj.getNumber()).thenReturn(14);

        when(filter2Obj.getIntValue(FIELD_IS_ENABLED, 1)).thenReturn(1);
        when(filter4Obj.getIntValue(FIELD_IS_ENABLED, 1)).thenReturn(0);

        BaseObject filter6Obj = mock(BaseObject.class, "filter6Obj");
        BaseObject filter7Obj = mock(BaseObject.class, "filter7Obj");
        when(userDoc.getXObject(TOGGLEABLE_FILTER_PREFERENCE_CLASS, FIELD_FILTER_NAME,
            "filter6", false)).thenReturn(filter6Obj);
        when(filter6Obj.getNumber()).thenReturn(16);
        when(userDoc.getXObject(TOGGLEABLE_FILTER_PREFERENCE_CLASS, FIELD_FILTER_NAME,
            "filter7", false)).thenReturn(filter7Obj);
        when(filter7Obj.getNumber()).thenReturn(17);

        when(filter6Obj.getIntValue(FIELD_IS_ENABLED, 0)).thenReturn(1);
        when(filter7Obj.getIntValue(FIELD_IS_ENABLED, 0)).thenReturn(0);

        Map<String, ToggleableNotificationFilterActivation> expectedResult = Map.of(
            "filter2", new ToggleableNotificationFilterActivation("filter2", true, user, 2),
            "filter3", new ToggleableNotificationFilterActivation("filter3", true, user, -1),
            "filter4", new ToggleableNotificationFilterActivation("filter4", false, user, 14),
            "filter5", new ToggleableNotificationFilterActivation("filter5", false, user, -1),
            "filter6", new ToggleableNotificationFilterActivation("filter6", true, user, 16),
            "filter7", new ToggleableNotificationFilterActivation("filter7", false, user, 17)
        );
        assertEquals(expectedResult, this.defaultModelBridge.getToggleableFilterActivations(user));
        verify(context).setWikiReference(user.getWikiReference());
        verify(context).setWikiReference(currentWiki);
    }
}
