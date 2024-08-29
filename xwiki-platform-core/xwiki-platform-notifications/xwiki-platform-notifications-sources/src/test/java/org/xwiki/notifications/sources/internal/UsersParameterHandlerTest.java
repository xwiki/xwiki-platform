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
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link UsersParameterHandler}.
 *
 * @version $Id$
 */
@ComponentTest
class UsersParameterHandlerTest
{
    @InjectMockComponents
    private UsersParameterHandler usersParameterHandler;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @MockComponent
    private DocumentReferenceResolver<String> defaultDocumentReferenceResolver;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Test
    void handlerUsersParameter() throws Exception
    {
        NotificationParameters notificationParameters = new NotificationParameters();
        this.usersParameterHandler.handleUsersParameter(null, notificationParameters);
        assertEquals(new NotificationParameters(), notificationParameters);

        this.usersParameterHandler.handleUsersParameter("", notificationParameters);
        assertEquals(new NotificationParameters(), notificationParameters);

        DocumentReference fooRef = new DocumentReference("currentwiki", "XWiki", "Foo");
        DocumentReference currentBarRef = new DocumentReference("currentwiki", "XWiki", "Bar");
        DocumentReference defaultBarRef = new DocumentReference("xwiki", "XWiki", "Bar");
        DocumentReference bazRef = new DocumentReference("otherwiki", "XWiki", "Baz");

        when(this.currentDocumentReferenceResolver.resolve("XWiki.Foo")).thenReturn(fooRef);
        when(this.currentDocumentReferenceResolver.resolve("XWiki.Bar")).thenReturn(currentBarRef);
        when(this.currentDocumentReferenceResolver.resolve("otherwiki:XWiki.Baz")).thenReturn(bazRef);

        when(this.defaultDocumentReferenceResolver.resolve("XWiki.Bar")).thenReturn(defaultBarRef);

        when(this.documentAccessBridge.exists(fooRef)).thenReturn(true);
        when(this.documentAccessBridge.exists(currentBarRef)).thenReturn(false);
        when(this.documentAccessBridge.exists(defaultBarRef)).thenReturn(true);
        when(this.documentAccessBridge.exists(bazRef)).thenReturn(true);

        when(this.entityReferenceSerializer.serialize(fooRef)).thenReturn("currentwiki:XWiki.Foo");
        when(this.entityReferenceSerializer.serialize(defaultBarRef)).thenReturn("xwiki:XWiki.Bar");
        when(this.entityReferenceSerializer.serialize(bazRef)).thenReturn("otherwiki:XWiki.Baz");

        List<NotificationFilter> notificationFilterList = Collections.singletonList(
            new FollowedUserOnlyEventFilter(entityReferenceSerializer, Arrays.asList(
                "currentwiki:XWiki.Foo", "xwiki:XWiki.Bar", "otherwiki:XWiki.Baz"
            ))
        );
        List<NotificationFilterPreference> notificationFilterPreferenceList = Arrays.asList(
            getFilterPreference("currentwiki:XWiki.Foo"),
            getFilterPreference("xwiki:XWiki.Bar"),
            getFilterPreference("otherwiki:XWiki.Baz")
        );

        NotificationParameters expectedParameters = new NotificationParameters();
        expectedParameters.format = NotificationFormat.EMAIL;
        expectedParameters.filters = notificationFilterList;
        expectedParameters.filterPreferences = notificationFilterPreferenceList;

        notificationParameters.format = NotificationFormat.EMAIL;
        this.usersParameterHandler.handleUsersParameter("Foo,XWiki.Bar,otherwiki:XWiki.Baz", notificationParameters);
        assertEquals(expectedParameters, notificationParameters);
    }

    private NotificationFilterPreference getFilterPreference(String userId)
    {
        DefaultNotificationFilterPreference pref = new DefaultNotificationFilterPreference();
        pref.setId(String.format("userRestFilters_%s", userId));
        pref.setFilterType(NotificationFilterType.INCLUSIVE);
        pref.setEnabled(true);
        pref.setNotificationFormats(Collections.singleton(NotificationFormat.EMAIL));
        pref.setUser(userId);
        return pref;
    }
}
