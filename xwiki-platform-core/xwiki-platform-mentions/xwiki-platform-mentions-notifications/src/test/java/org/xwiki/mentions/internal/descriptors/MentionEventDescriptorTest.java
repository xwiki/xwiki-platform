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
package org.xwiki.mentions.internal.descriptors;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.mentions.events.MentionEvent;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.TargetableNotificationPreference;
import org.xwiki.notifications.preferences.TargetableNotificationPreferenceBuilder;
import org.xwiki.notifications.preferences.internal.DefaultTargetableNotificationPreferenceBuilder;
import org.xwiki.notifications.preferences.internal.WikiNotificationPreferenceProvider;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MentionEventDescriptor}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ DefaultTargetableNotificationPreferenceBuilder.class })
class MentionEventDescriptorTest
{
    @InjectMockComponents
    private MentionEventDescriptor mentionEventDescriptor;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private NotificationPreferenceManager notificationPreferenceManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext context;

    @BeforeComponent("initializeWithSavedPreference")
    void beforeInitializeWithSavedPreference() throws NotificationException
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);

        WikiReference wikiReference = new WikiReference("foo");
        when(this.context.getWikiReference()).thenReturn(wikiReference);
        NotificationPreference notificationPreference1 = mock(NotificationPreference.class);
        NotificationPreference notificationPreference2 = mock(NotificationPreference.class);
        NotificationPreference notificationPreference3 = mock(NotificationPreference.class);
        when(this.notificationPreferenceManager.getAllPreferences(wikiReference)).thenReturn(Arrays.asList(
            notificationPreference1,
            notificationPreference2,
            notificationPreference3
        ));
        when(notificationPreference2.getProperties()).thenReturn(Collections.singletonMap(
            NotificationPreferenceProperty.EVENT_TYPE, MentionEvent.EVENT_TYPE));
        when(notificationPreference2.getStartDate()).thenReturn(new Date(0));
    }

    @Test
    void initializeWithSavedPreference() throws NotificationException
    {
        verify(this.notificationPreferenceManager, never()).savePreferences(any());
    }

    @BeforeComponent("initializeWithUnsavedPreference")
    void beforeInitializeWithUnsavedPreference() throws NotificationException
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);

        WikiReference wikiReference = new WikiReference("bar");
        when(this.context.getWikiReference()).thenReturn(wikiReference);
        NotificationPreference notificationPreference1 = mock(NotificationPreference.class);
        NotificationPreference notificationPreference2 = mock(NotificationPreference.class);
        NotificationPreference notificationPreference3 = mock(NotificationPreference.class);
        when(this.notificationPreferenceManager.getAllPreferences(wikiReference)).thenReturn(Arrays.asList(
            notificationPreference1,
            notificationPreference2,
            notificationPreference3
        ));

        // pref1 has a wrong event type but a start date
        when(notificationPreference1.getProperties()).thenReturn(
            Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "bar"));
        when(notificationPreference1.getStartDate()).thenReturn(new Date(0));

        // pref 2 has the right event type but no start date
        when(notificationPreference2.getProperties()).thenReturn(Collections.singletonMap(
            NotificationPreferenceProperty.EVENT_TYPE, MentionEvent.EVENT_TYPE));

        // pref 3 has no a null empty map and no start date
    }

    @Test
    void initializeWithUnsavedPreference() throws NotificationException, ComponentLookupException
    {
        Map<NotificationPreferenceProperty, Object> properties = new HashMap<>();
        properties.put(NotificationPreferenceProperty.EVENT_TYPE, MentionEvent.EVENT_TYPE);

        TargetableNotificationPreferenceBuilder targetableNotificationPreferenceBuilder =
            this.componentManager.getInstance(TargetableNotificationPreferenceBuilder.class);

        // Create the preference
        TargetableNotificationPreference notificationPreference = targetableNotificationPreferenceBuilder
            .prepare()
            .setCategory(NotificationPreferenceCategory.DEFAULT)
            .setEnabled(true)
            .setFormat(NotificationFormat.ALERT)
            .setProperties(properties)
            .setProviderHint(WikiNotificationPreferenceProvider.NAME)
            .setStartDate(new Date())
            .setTarget(new WikiReference("bar"))
            .build();

        verify(this.notificationPreferenceManager).savePreferences(Collections.singletonList(notificationPreference));
    }
}