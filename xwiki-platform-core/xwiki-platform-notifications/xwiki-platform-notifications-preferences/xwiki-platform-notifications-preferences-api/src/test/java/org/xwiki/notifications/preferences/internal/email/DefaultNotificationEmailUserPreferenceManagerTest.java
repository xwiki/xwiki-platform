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
package org.xwiki.notifications.preferences.internal.email;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.preferences.NotificationEmailInterval;
import org.xwiki.notifications.preferences.email.NotificationEmailDiffType;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNotificationEmailUserPreferenceManager}.
 *
 * @version $Id$
 * @since 9.11RC1
 */
@ComponentTest
class DefaultNotificationEmailUserPreferenceManagerTest
{
    @InjectMockComponents
    private DefaultNotificationEmailUserPreferenceManager emailUserPreferenceManager;
    
    @MockComponent
    private DocumentAccessBridge documentAccessBridge;
    
    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserSerializer;

    private DocumentReference currentUser;

    @BeforeEach
    void setUp() throws Exception
    {
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");

        currentUser = new DocumentReference("someWiki", "XWiki", "User");
        when(documentAccessBridge.getCurrentUserReference()).thenReturn(currentUser);

        when(this.documentUserSerializer.serialize(CurrentUserReference.INSTANCE)).thenReturn(currentUser);
    }

    private List<String> getCodeSpace()
    {
        return Arrays.asList("XWiki", "Notifications", "Code");
    }

    @Test
    void getDiffType() throws Exception
    {
        // Default value
        assertEquals(NotificationEmailDiffType.STANDARD, this.emailUserPreferenceManager.getDiffType());

        // Value from the user profile
        DocumentReference emailClassReference = new DocumentReference("someWiki", Arrays.asList("XWiki",
                "Notifications", "Code"), "NotificationEmailPreferenceClass");
        when(documentAccessBridge.getProperty(currentUser, emailClassReference, "diffType"))
                .thenReturn("NOTHING");
        assertEquals(NotificationEmailDiffType.NOTHING, this.emailUserPreferenceManager.getDiffType());

        // Value from the main wiki config
        DocumentReference user2 = new DocumentReference("someWiki", "XWiki", "User2");
        UserReference user2Ref =  mock(UserReference.class, "user2");
        when(this.documentUserSerializer.serialize(user2Ref)).thenReturn(user2);
        when(documentAccessBridge.getProperty(new DocumentReference("mainWiki", getCodeSpace(),
                "NotificationAdministration"), new DocumentReference("mainWiki",
                getCodeSpace(), "NotificationEmailPreferenceClass"),
                "diffType")).thenReturn("NOTHING");
        assertEquals(NotificationEmailDiffType.NOTHING,
            this.emailUserPreferenceManager.getDiffType(user2Ref));

        // Value from the user's wiki config
        when(documentAccessBridge.getProperty(new DocumentReference("someWiki", getCodeSpace(),
                        "NotificationAdministration"), new DocumentReference("mainWiki",
                        getCodeSpace(), "NotificationEmailPreferenceClass"),
                "diffType")).thenReturn("NOTHING");

        assertEquals(NotificationEmailDiffType.NOTHING,
            this.emailUserPreferenceManager.getDiffType(user2Ref));
    }

    @Test
    void getInterval() throws Exception
    {
        // Default value
        assertEquals(NotificationEmailInterval.DAILY, this.emailUserPreferenceManager.getInterval());

        // Value from the user profile
        DocumentReference emailClassReference = new DocumentReference("someWiki", Arrays.asList("XWiki",
            "Notifications", "Code"), "NotificationEmailPreferenceClass");
        when(documentAccessBridge.getProperty(currentUser, emailClassReference, "interval"))
            .thenReturn("DAILY");
        assertEquals(NotificationEmailInterval.DAILY, this.emailUserPreferenceManager.getInterval());

        // Value from the main wiki config
        DocumentReference user2 = new DocumentReference("someWiki", "XWiki", "User2");
        UserReference user2Ref =  mock(UserReference.class, "user2");
        when(this.documentUserSerializer.serialize(user2Ref)).thenReturn(user2);
        when(documentAccessBridge.getProperty(new DocumentReference("mainWiki", getCodeSpace(),
                "NotificationAdministration"), new DocumentReference("mainWiki",
                getCodeSpace(), "NotificationEmailPreferenceClass"),
            "interval")).thenReturn("DAILY");
        assertEquals(NotificationEmailInterval.DAILY,
            this.emailUserPreferenceManager.getInterval(user2Ref));

        // Value from the user's wiki config
        when(documentAccessBridge.getProperty(new DocumentReference("someWiki", getCodeSpace(),
                "NotificationAdministration"), new DocumentReference("mainWiki",
                getCodeSpace(), "NotificationEmailPreferenceClass"),
            "interval")).thenReturn("DAILY");

        assertEquals(NotificationEmailInterval.DAILY,
            this.emailUserPreferenceManager.getInterval(user2Ref));
    }
}
