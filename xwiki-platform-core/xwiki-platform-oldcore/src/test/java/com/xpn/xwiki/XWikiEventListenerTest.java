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
package com.xpn.xwiki;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.WikiCopiedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.event.ComponentDescriptorAddedEvent;
import org.xwiki.job.event.JobFinishedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectPropertyAddedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;
import com.xpn.xwiki.internal.mandatory.XWikiPreferencesDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link org.xwiki.observation.EventListener} implemented by {@link XWiki}, i.e. for
 * {@link XWiki#getEvents()} and {@link XWiki#onEvent(Event, Object, Object)}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class XWikiEventListenerTest
{
    private static final String BACKLINKS = "backlinks";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private XWiki xwiki;

    @BeforeEach
    void beforeEach()
    {
        this.xwiki = this.oldcore.getSpyXWiki();
    }

    @Test
    void onEventUpdatesTheBacklinksCacheWhenTheBacklinksPreferenceIsModified()
    {
        XWikiDocument preferences = preferencesWithBacklinks(1);

        this.xwiki.onEvent(new XObjectPropertyUpdatedEvent(propertyReference(preferences, BACKLINKS)), preferences,
            this.oldcore.getXWikiContext());

        assertTrue(this.xwiki.hasBacklinks(this.oldcore.getXWikiContext()));
    }

    @Test
    void onEventDisablesTheBacklinksCacheWhenTheBacklinksPreferenceIsUnset()
    {
        XWikiDocument preferences = preferencesWithBacklinks(0);

        this.xwiki.onEvent(new XObjectPropertyDeletedEvent(propertyReference(preferences, BACKLINKS)), preferences,
            this.oldcore.getXWikiContext());

        assertFalse(this.xwiki.hasBacklinks(this.oldcore.getXWikiContext()));
    }

    @Test
    void onEventLeavesTheBacklinksCacheAloneWhenAnotherPreferenceIsModified()
    {
        XWikiDocument preferences = preferencesWithBacklinks(1);
        preferences.getXObject(XWikiPreferencesDocumentInitializer.LOCAL_REFERENCE).setIntValue("tags", 1);

        // Fill the cache first, so that the assertion below tells "left alone" from "not set yet".
        this.xwiki.onEvent(new XObjectPropertyAddedEvent(propertyReference(preferences, BACKLINKS)), preferences,
            this.oldcore.getXWikiContext());

        this.xwiki.onEvent(new XObjectPropertyUpdatedEvent(propertyReference(preferences, "tags")), preferences,
            this.oldcore.getXWikiContext());

        assertTrue(this.xwiki.hasBacklinks(this.oldcore.getXWikiContext()));
    }

    @Test
    void onEventIgnoresAnEventItDoesNotHandle()
    {
        // The source of an event this listener doesn't handle is not a document, so it must not be dereferenced as
        // one. Only the events getEvents() returns can reach onEvent(), which is why this is only a safety net.
        assertDoesNotThrow(() -> this.xwiki.onEvent(new WikiCopiedEvent("source", "target"), "not a document",
            this.oldcore.getXWikiContext()));
    }

    @Test
    void getEventsReturnsTheEventsOnEventHandles()
    {
        // onEvent() dispatches on the type of each of these and ignores anything else, so adding an event here
        // means adding a case there.
        List<Class<?>> eventTypes = this.xwiki.getEvents().stream().<Class<?>>map(Object::getClass).toList();

        assertEquals(List.of(
            XObjectPropertyAddedEvent.class,
            XObjectPropertyDeletedEvent.class,
            XObjectPropertyUpdatedEvent.class,
            WikiDeletedEvent.class,
            ComponentDescriptorAddedEvent.class,
            JobFinishedEvent.class), eventTypes);
    }

    private XWikiDocument preferencesWithBacklinks(int backlinks)
    {
        XWikiDocument preferences = new XWikiDocument(new DocumentReference(
            this.oldcore.getXWikiContext().getWikiId(), XWiki.SYSTEM_SPACE,
            XWikiPreferencesDocumentInitializer.NAME));
        BaseObject object = new BaseObject();
        object.setXClassReference(XWikiPreferencesDocumentInitializer.LOCAL_REFERENCE);
        preferences.addXObject(object);
        object.setIntValue(BACKLINKS, backlinks);

        return preferences;
    }

    private EntityReference propertyReference(XWikiDocument preferences, String property)
    {
        PropertyInterface field =
            preferences.getXObject(XWikiPreferencesDocumentInitializer.LOCAL_REFERENCE).getField(property);

        return field.getReference();
    }
}
