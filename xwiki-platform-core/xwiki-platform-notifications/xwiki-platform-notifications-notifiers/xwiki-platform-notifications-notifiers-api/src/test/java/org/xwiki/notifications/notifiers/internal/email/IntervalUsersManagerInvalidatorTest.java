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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link IntervalUsersManagerInvalidator}.
 *
 * @version $Id$
 * @since 12.10.7
 * @since 13.3RC1
 */
@ComponentTest
public class IntervalUsersManagerInvalidatorTest
{
    @InjectMockComponents
    private IntervalUsersManagerInvalidator invalidator;

    @MockComponent
    private IntervalUsersManager users;

    private static final EntityReference USER_OBJECT = BaseObjectReference.any("XWiki.XWikiUsers");

    private static final EntityReference INTERVAL_OBJECT =
        BaseObjectReference.any("XWiki.Notifications.Code.NotificationEmailPreferenceClass");

    private XWikiDocument document;
    private DocumentReference documentReference;

    @BeforeEach
    void setup()
    {
        this.document = mock(XWikiDocument.class);
        this.documentReference = mock(DocumentReference.class);
        when(this.document.getDocumentReference()).thenReturn(documentReference);
    }

    @Test
    void onEventUserAdded()
    {
        Event event = new XObjectAddedEvent(USER_OBJECT);
        this.invalidator.onEvent(event, this.document, null);
        verify(this.users).invalidateUser(this.documentReference);
    }

    @Test
    void onEventUserUpdated()
    {
        Event event = new XObjectUpdatedEvent(USER_OBJECT);
        this.invalidator.onEvent(event, this.document, null);
        verify(this.users).invalidateUser(this.documentReference);
    }

    @Test
    void onEventUserDeleted()
    {
        Event event = new XObjectDeletedEvent(USER_OBJECT);
        this.invalidator.onEvent(event, this.document, null);
        verify(this.users).invalidateUser(this.documentReference);
    }

    @Test
    void onEventIntervalAdded()
    {
        Event event = new XObjectAddedEvent(INTERVAL_OBJECT);
        this.invalidator.onEvent(event, this.document, null);
        verify(this.users).invalidateUser(this.documentReference);
    }

    @Test
    void onEventIntervalUpdated()
    {
        Event event = new XObjectUpdatedEvent(INTERVAL_OBJECT);
        this.invalidator.onEvent(event, this.document, null);
        verify(this.users).invalidateUser(this.documentReference);
    }

    @Test
    void onEventIntervalDeleted()
    {
        Event event = new XObjectDeletedEvent(INTERVAL_OBJECT);
        this.invalidator.onEvent(event, this.document, null);
        verify(this.users).invalidateUser(this.documentReference);
    }

    @Test
    void onEventWikiDeleted()
    {
        Event event = new WikiDeletedEvent("mywiki");
        this.invalidator.onEvent(event, null, null);
        verify(this.users).invalidateWiki("mywiki");
    }
}
