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
package org.xwiki.notifications.sources.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Rule;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.internal.SimilarityCalculator;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.sources.NewNotificationManager;
import org.xwiki.query.Query;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentList(SimilarityCalculator.class)
public class DefaultNotificationManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<DefaultNotificationManager> mocker =
            new MockitoComponentMockingRule<>(DefaultNotificationManager.class);

    private DocumentAccessBridge documentAccessBridge;
    private DocumentReferenceResolver<String> documentReferenceResolver;
    private NotificationPreferenceManager notificationPreferenceManager;
    private NotificationFilterManager notificationFilterManager;
    private NewNotificationManager newNotificationManager;
    private WikiDescriptorManager wikiDescriptorManager;

    private DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "UserA");
    private Query query;
    private Date startDate;

    @Before
    public void setUp() throws Exception
    {
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        documentReferenceResolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);
        notificationPreferenceManager = mocker.getInstance(NotificationPreferenceManager.class);
        notificationFilterManager = mocker.getInstance(NotificationFilterManager.class);
        newNotificationManager = mocker.getInstance(NewNotificationManager.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        startDate = new Date(10);

        when(documentReferenceResolver.resolve("xwiki:XWiki.UserA")).thenReturn(userReference);
        query = mock(Query.class);

        NotificationPreference pref1 = mock(NotificationPreference.class);
        when(pref1.getProperties()).thenReturn(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "create"));
        when(pref1.isNotificationEnabled()).thenReturn(true);

        when(notificationPreferenceManager.getAllPreferences(userReference)).thenReturn(Arrays.asList(pref1));
    }

}