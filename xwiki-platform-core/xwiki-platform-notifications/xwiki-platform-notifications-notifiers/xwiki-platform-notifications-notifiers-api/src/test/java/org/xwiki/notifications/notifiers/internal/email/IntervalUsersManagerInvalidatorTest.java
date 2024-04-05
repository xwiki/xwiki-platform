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
package org.xwiki.notifications.notifiers.internal.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.event.Event;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.internal.UserPropertyConstants;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link IntervalUsersManagerInvalidator}.
 *
 * @version $Id$
 * @since 12.10.7
 * @since 13.3RC1
 */
@OldcoreTest
@ReferenceComponentList
class IntervalUsersManagerInvalidatorTest
{
    @InjectMockComponents
    private IntervalUsersManagerInvalidator invalidator;

    @MockComponent
    private IntervalUsersManager users;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private static final EntityReference USER_OBJECT = BaseObjectReference.any("XWiki.XWikiUsers");

    private static final EntityReference INTERVAL_OBJECT =
        BaseObjectReference.any("XWiki.Notifications.Code.NotificationEmailPreferenceClass");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("wiki", "space", "document");

    private XWikiDocument userDocument;

    private void setActive(XWikiDocument document, boolean active)
    {
        BaseObject userObject = document.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE, true,
            this.oldcore.getXWikiContext());
        userObject.setIntValue(UserPropertyConstants.ACTIVE, active ? 1 : 0);
    }

    @BeforeEach
    void beforeEach() throws XWikiException
    {
        this.userDocument = this.oldcore.getSpyXWiki().getDocument(USER_REFERENCE, this.oldcore.getXWikiContext());
        setActive(this.userDocument, true);
        this.oldcore.getSpyXWiki().saveDocument(userDocument, this.oldcore.getXWikiContext());
    }

    @Test
    void onEventUserAdded()
    {
        Event event = new XObjectAddedEvent(USER_OBJECT);
        this.invalidator.onEvent(event, this.userDocument, null);
        verify(this.users).invalidateUser(USER_REFERENCE);
    }

    @Test
    void onEventUserUpdated()
    {
        Event event = new XObjectUpdatedEvent(USER_OBJECT);
        this.invalidator.onEvent(event, this.userDocument, null);
        verify(this.users).invalidateUser(USER_REFERENCE);
    }

    @Test
    void onEventUserDeleted()
    {
        Event event = new XObjectDeletedEvent(USER_OBJECT);
        this.invalidator.onEvent(event, this.userDocument, null);
        verify(this.users).invalidateUser(USER_REFERENCE);
    }

    @Test
    void onEventIntervalAdded()
    {
        Event event = new XObjectAddedEvent(INTERVAL_OBJECT);
        this.invalidator.onEvent(event, this.userDocument, null);
        verify(this.users).invalidateUser(USER_REFERENCE);
    }

    @Test
    void onEventIntervalUpdated()
    {
        Event event = new XObjectUpdatedEvent(INTERVAL_OBJECT);
        this.invalidator.onEvent(event, this.userDocument, null);
        verify(this.users).invalidateUser(USER_REFERENCE);
    }

    @Test
    void onEventIntervalDeleted()
    {
        Event event = new XObjectDeletedEvent(INTERVAL_OBJECT);
        this.invalidator.onEvent(event, this.userDocument, null);
        verify(this.users).invalidateUser(USER_REFERENCE);
    }

    @Test
    void onEventWikiDeleted()
    {
        Event event = new WikiDeletedEvent("mywiki");
        this.invalidator.onEvent(event, null, null);
        verify(this.users).invalidateWiki("mywiki");
    }

    @Test
    void modifyInActiveUser()
    {
        Event event = new XObjectUpdatedEvent(INTERVAL_OBJECT);

        // The user is not and was not active
        setActive(this.userDocument, false);
        setActive(this.userDocument.getOriginalDocument(), false);

        this.invalidator.onEvent(event, this.userDocument, null);
        verify(this.users, never()).invalidateUser(USER_REFERENCE);

        // The user is not but was active
        setActive(this.userDocument, false);
        setActive(this.userDocument.getOriginalDocument(), true);

        this.invalidator.onEvent(event, this.userDocument, null);
        verify(this.users, times(1)).invalidateUser(USER_REFERENCE);

        // The user is but was not active
        setActive(this.userDocument, false);
        setActive(this.userDocument.getOriginalDocument(), true);

        this.invalidator.onEvent(event, this.userDocument, null);
        verify(this.users, times(2)).invalidateUser(USER_REFERENCE);
    }
}
