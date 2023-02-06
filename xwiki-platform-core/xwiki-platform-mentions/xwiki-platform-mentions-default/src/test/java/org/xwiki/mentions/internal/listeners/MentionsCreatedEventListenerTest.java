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
package org.xwiki.mentions.internal.listeners;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.index.TaskManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.test.LogLevel.DEBUG;

/**
 * Test of {@link MentionsCreatedEventListener}.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@ComponentTest
class MentionsCreatedEventListenerTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(DEBUG);

    @InjectMockComponents
    private MentionsCreatedEventListener listener;

    @MockComponent
    private TaskManager taskManager;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Mock
    private XWikiDocument document;

    @Test
    void onEvent()
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        DocumentCreatedEvent event = new DocumentCreatedEvent(documentReference);

        when(this.document.getDocumentReference()).thenReturn(documentReference);
        when(this.document.getId()).thenReturn(42L);
        when(this.document.getVersion()).thenReturn("1.1");

        this.listener.onEvent(event, this.document, null);

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.DEBUG, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Event [org.xwiki.bridge.event.DocumentCreatedEvent] received from [document] with data [null].",
            this.logCapture.getMessage(0));

        verify(this.taskManager).addTask("xwiki", 42, "1.1", "mention");
    }
}
