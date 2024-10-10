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

import java.util.Collections;
import java.util.Date;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentTest
public abstract class AbstractQueryGeneratorTest
{
    protected static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "UserA");
    protected static final String SERIALIZED_USER_REFERENCE = "xwiki:XWiki.UserA";

    @MockComponent
    protected WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    protected NotificationFilterManager notificationFilterManager;

    @MockComponent
    protected RecordableEventDescriptorHelper recordableEventDescriptorHelper;

    @MockComponent
    protected UserPropertiesResolver userPropertiesResolver;

    @MockComponent
    @Named("document")
    protected UserReferenceResolver<DocumentReference> userReferenceResolver;

    protected Date startDate;

    protected Date pref1StartDate;

    protected NotificationFilterPreference fakeFilterPreference;

    protected NotificationPreference pref1;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        this.startDate = new Date(10);

        this.pref1StartDate = new Date(100000000);

        this.pref1 = mock(NotificationPreference.class);
        when(this.pref1.getProperties())
            .thenReturn(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "create"));
        when(this.pref1.getFormat()).thenReturn(NotificationFormat.ALERT);
        when(this.pref1.getStartDate()).thenReturn(pref1StartDate);
        when(this.pref1.isNotificationEnabled()).thenReturn(true);

        this.fakeFilterPreference = mock(NotificationFilterPreference.class);
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");

        when(this.recordableEventDescriptorHelper.hasDescriptor(anyString(), any(DocumentReference.class)))
            .thenReturn(true);

        UserProperties userProperties = mock(UserProperties.class);
        when(userProperties.displayHiddenDocuments()).thenReturn(false);
        when(this.userPropertiesResolver.resolve(any(UserReference.class))).thenReturn(userProperties);
        when(this.userReferenceResolver.resolve(USER_REFERENCE)).thenReturn(mock(UserReference.class));
    }
}
