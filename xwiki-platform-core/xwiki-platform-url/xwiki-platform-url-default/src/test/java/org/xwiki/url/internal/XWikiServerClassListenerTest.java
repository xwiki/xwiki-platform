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
package org.xwiki.url.internal;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.observation.event.Event;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.URLSecurityManager;

import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link XWikiServerClassListener}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiServerClassListenerTest
{
    private static final DocumentReference DESCRIPTOR_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "XWikiServerXwiki");

    @InjectMockComponents
    private XWikiServerClassListener listener;

    // The listener only invalidates the cache of the default implementation, so mock that one.
    @MockComponent(classToMock = DefaultURLSecurityManager.class)
    private URLSecurityManager urlSecurityManager;

    static Stream<Event> descriptorEvents()
    {
        // The reference of the xobject as it is passed by XObjectEventGeneratorListener.
        BaseObjectReference objectReference =
            new BaseObjectReference(new ObjectReference("XWiki.XWikiServerClass[0]", DESCRIPTOR_REFERENCE));

        return Stream.of(new XObjectAddedEvent(objectReference), new XObjectUpdatedEvent(objectReference),
            new XObjectDeletedEvent(objectReference));
    }

    @ParameterizedTest
    @MethodSource("descriptorEvents")
    void listensToDescriptorEvents(Event firedEvent)
    {
        List<Event> events = this.listener.getEvents();
        assertTrue(events.stream().anyMatch(event -> event.matches(firedEvent)),
            () -> String.format("None of the events [%s] matches [%s]", events, firedEvent));
    }

    @Test
    void invalidatesTheCache()
    {
        this.listener.onEvent(new XObjectUpdatedEvent(), null, null);

        verify((DefaultURLSecurityManager) this.urlSecurityManager).invalidateCache();
    }
}
