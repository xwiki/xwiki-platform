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
import java.util.HashSet;
import java.util.Set;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link UserAddedEventListener}.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@ComponentTest
public class UserAddedEventListenerTest
{
    @InjectMockComponents
    private UserAddedEventListener userAddedEventListener;

    @MockComponent
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext xWikiContext;
    private XWiki xWiki;

    @BeforeEach
    void setup()
    {
        this.xWikiContext = mock(XWikiContext.class);
        this.xWiki = mock(XWiki.class);
        when(this.xWikiContext.getWiki()).thenReturn(this.xWiki);
        when(this.contextProvider.get()).thenReturn(this.xWikiContext);
    }

    @Test
    void onEvent() throws NotificationException, XWikiException
    {
        XWikiDocument userDoc = mock(XWikiDocument.class);
        DocumentReference documentReference = new DocumentReference("foo", "XWiki", "User");
        when(userDoc.getDocumentReference()).thenReturn(documentReference);
        when(userDoc.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE))
            .thenReturn(mock(BaseObject.class));
        WikiReference wikiReference = new WikiReference("foo");
        WikiReference currentWikiReference = new WikiReference("current");
        when(this.xWikiContext.getWikiReference()).thenReturn(currentWikiReference);

        NotificationFilterPreference filterPref1 = mock(NotificationFilterPreference.class);
        when(filterPref1.getFilterName()).thenReturn("filter1");
        NotificationFilterPreference filterPref2 = mock(NotificationFilterPreference.class);
        when(filterPref2.getFilterName()).thenReturn("filter2");
        NotificationFilterPreference filterPref3 = mock(NotificationFilterPreference.class);
        when(filterPref3.getFilterName()).thenReturn("filter3");
        when(this.notificationFilterPreferenceManager.getFilterPreferences(wikiReference))
            .thenReturn(Arrays.asList(filterPref1, filterPref2, filterPref3));

        Set<NotificationFilterPreference> expectedSet = new HashSet<>();
        DefaultNotificationFilterPreference expectedFilterPref1 = new DefaultNotificationFilterPreference();
        expectedFilterPref1.setFilterName("filter1");
        expectedFilterPref1.setProviderHint(UserProfileNotificationFilterPreferenceProvider.HINT);
        expectedSet.add(expectedFilterPref1);

        DefaultNotificationFilterPreference expectedFilterPref2 = new DefaultNotificationFilterPreference();
        expectedFilterPref2.setFilterName("filter2");
        expectedFilterPref2.setProviderHint(UserProfileNotificationFilterPreferenceProvider.HINT);
        expectedSet.add(expectedFilterPref2);

        DefaultNotificationFilterPreference expectedFilterPref3 = new DefaultNotificationFilterPreference();
        expectedFilterPref3.setFilterName("filter3");
        expectedFilterPref3.setProviderHint(UserProfileNotificationFilterPreferenceProvider.HINT);
        expectedSet.add(expectedFilterPref3);

        when(userDoc.getXObjects(UserAddedEventListener.TOGGLEABLE_FILTER_PREFERENCE_CLASS))
            .thenReturn(Collections.emptyList());
        XWikiDocument wikiNotificationConfiguration = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(UserAddedEventListener.NOTIFICATION_CONFIGURATION, this.xWikiContext))
            .thenReturn(wikiNotificationConfiguration);
        BaseObject obj1 = mock(BaseObject.class);
        BaseObject obj2 = mock(BaseObject.class);

        BaseObject obj1bis = mock(BaseObject.class);
        BaseObject obj2bis = mock(BaseObject.class);

        when(obj1.duplicate()).thenReturn(obj1bis);
        when(obj2.duplicate()).thenReturn(obj2bis);
        when(wikiNotificationConfiguration.getXObjects(UserAddedEventListener.TOGGLEABLE_FILTER_PREFERENCE_CLASS))
            .thenReturn(Arrays.asList(obj1, null, obj2, null));

        this.userAddedEventListener.onEvent(null, userDoc, null);
        verify(this.notificationFilterPreferenceManager).saveFilterPreferences(documentReference, expectedSet);
        verify(userDoc).addXObject(obj1bis);
        verify(userDoc).addXObject(obj2bis);
        verify(this.xWikiContext).setWikiReference(wikiReference);
        verify(this.xWikiContext).setWikiReference(currentWikiReference);
    }
}
