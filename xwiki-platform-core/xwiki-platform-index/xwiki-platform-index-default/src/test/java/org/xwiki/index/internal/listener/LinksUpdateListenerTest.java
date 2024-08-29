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
package org.xwiki.index.internal.listener;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.index.TaskManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.xwiki.index.internal.DefaultLinksTaskConsumer.LINKS_TASK_TYPE;

/**
 * Test of {@link LinksUpdateListener}.
 *
 * @version $Id$
 * @since 14.2RC1
 */
@ComponentTest
class LinksUpdateListenerTest
{
    @InjectMockComponents
    private LinksUpdateListener linksUpdateListener;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @MockComponent
    private TaskManager taskManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @BeforeEach
    void setUp()
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
    }

    @Test
    void onEventIsRemote()
    {
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);
        this.linksUpdateListener.onEvent(new DocumentCreatedEvent(), null, this.context);
        verifyNoInteractions(this.taskManager);
    }

    @Test
    void onEventBacklinksDeactivated()
    {
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);
        when(this.wiki.hasBacklinks(this.context)).thenReturn(false);
        this.linksUpdateListener.onEvent(new DocumentCreatedEvent(), null, this.context);
        verifyNoInteractions(this.taskManager);
    }

    @Test
    void onEvent()
    {
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);
        when(this.wiki.hasBacklinks(this.context)).thenReturn(true);
        XWikiDocument doc = mock(XWikiDocument.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        when(doc.getId()).thenReturn(42L);
        when(doc.getDocumentReference()).thenReturn(documentReference);
        this.linksUpdateListener.onEvent(new DocumentCreatedEvent(), doc, this.context);
        verify(this.taskManager).addTask("wiki", 42L, LINKS_TASK_TYPE);
    }
}
