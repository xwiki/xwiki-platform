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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.preferences.email.NotificationEmailDiffType;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNotificationEmailUserPreferenceManager}.
 *
 * @version $Id$
 * @since 9.11RC1
 */
public class DefaultNotificationEmailUserPreferenceManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<DefaultNotificationEmailUserPreferenceManager> mocker =
            new MockitoComponentMockingRule<>(DefaultNotificationEmailUserPreferenceManager.class);

    private DocumentAccessBridge documentAccessBridge;
    private DocumentReferenceResolver<String> referenceResolver;
    private WikiDescriptorManager wikiDescriptorManager;
    private DocumentReference currentUser;

    @Before
    public void setUp() throws Exception
    {
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        referenceResolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);

        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");

        currentUser = new DocumentReference("someWiki", "XWiki", "User");
        when(documentAccessBridge.getCurrentUserReference()).thenReturn(currentUser);
    }

    private List<String> getCodeSpace()
    {
        return Arrays.asList("XWiki", "Notifications", "Code");
    }

    @Test
    public void getDiffType() throws Exception
    {
        // Default value
        assertEquals(NotificationEmailDiffType.STANDARD, mocker.getComponentUnderTest().getDiffType());

        // Value from the user profile
        DocumentReference emailClassReference = new DocumentReference("someWiki", Arrays.asList("XWiki",
                "Notifications", "Code"), "NotificationEmailPreferenceClass");
        when(documentAccessBridge.getProperty(currentUser, emailClassReference, "diffType"))
                .thenReturn("NOTHING");
        assertEquals(NotificationEmailDiffType.NOTHING, mocker.getComponentUnderTest().getDiffType());

        // Value from the main wiki config
        DocumentReference user2 = new DocumentReference("someWiki", "XWiki", "User2");
        String user2Id = "someWiki:XWiki.User2";
        when(referenceResolver.resolve(user2Id)).thenReturn(user2);
        when(documentAccessBridge.getProperty(new DocumentReference("mainWiki", getCodeSpace(),
                "NotificationAdministration"), new DocumentReference("mainWiki",
                getCodeSpace(), "NotificationEmailPreferenceClass"),
                "diffType")).thenReturn("NOTHING");
        assertEquals(NotificationEmailDiffType.NOTHING, mocker.getComponentUnderTest().getDiffType(user2Id));

        // Value from the user's wiki config
        when(documentAccessBridge.getProperty(new DocumentReference("someWiki", getCodeSpace(),
                        "NotificationAdministration"), new DocumentReference("mainWiki",
                        getCodeSpace(), "NotificationEmailPreferenceClass"),
                "diffType")).thenReturn("NOTHING");

        assertEquals(NotificationEmailDiffType.NOTHING, mocker.getComponentUnderTest().getDiffType(user2Id));
    }
}
