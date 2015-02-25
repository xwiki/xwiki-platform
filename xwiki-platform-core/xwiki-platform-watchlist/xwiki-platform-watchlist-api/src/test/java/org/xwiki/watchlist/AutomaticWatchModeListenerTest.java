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
package com.xpn.xwiki.plugin.watchlist;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.mockito.Mockito.*;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.WikiCreatingEvent;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.internal.DefaultObservationContext;
import org.xwiki.observation.internal.ObservationContextListener;
import org.xwiki.test.ComponentManagerRule;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XARImportingEvent;
import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for {@link AutomaticWatchModeListener}.
 * 
 * @version $Id$
 */
@ComponentList({
    ObservationContextListener.class,
    DefaultExecution.class,
    DefaultObservationContext.class
})
public class AutomaticWatchModeListenerTest
{
    private WatchListStore mockStore;

    private AutomaticWatchModeListener listener;

    private EventListener observationContextListener;

    @Rule
    public final ComponentManagerRule componentManager = new ComponentManagerRule();

    @Before
    public void setUp() throws Exception
    {
        this.mockStore = mock(WatchListStore.class);
        this.listener = new AutomaticWatchModeListener(this.mockStore);

        Utils.setComponentManager(this.componentManager);

        // Make sure we have an Execution Context since the observationContextListener will store current events in it
        Execution execution = this.componentManager.getInstance(Execution.class);
        execution.setContext(new ExecutionContext());

        this.observationContextListener =
            this.componentManager.getInstance(EventListener.class, "ObservationContextListener");
    }

    /**
     * Verify that we don't do anything when the current event is inside a WikiCreatingEvent.
     */
    @Test
    public void onEventWhenInContextOfWikiCreatingEvent()
    {
        // We simulate a WikiCreatingEvent in the Execution Context
        this.observationContextListener.onEvent(new WikiCreatingEvent(), null, null);

        this.listener.onEvent(new DocumentCreatedEvent(), null, null);
    }

    /**
     * Verify that we don't do anything when the current event is inside a XARImportingEvent.
     */
    @Test
    public void onEventWhenInContextOXARImportingEvent()
    {
        // We simulate a XARImportingEvent in the Execution Context
        this.observationContextListener.onEvent(new XARImportingEvent(), null, null);

        this.listener.onEvent(new DocumentCreatedEvent(), null, null);
    }

    @Test
    public void onEventWhenDocumentCreatedEvent() throws Exception
    {
        XWikiContext context = mock(XWikiContext.class);

        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference documentReference = new DocumentReference("authorWiki", "authorSpace", "authorPage");
        when(document.getContentAuthor()).thenReturn("content author");
        when(document.getContentAuthorReference()).thenReturn(documentReference);
        when(document.getPrefixedFullName()).thenReturn("authorSpace.authorPage");

        when(this.mockStore.getAutomaticWatchMode("content author", context)).thenReturn(
                AutomaticWatchMode.ALL);

        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xwiki.exists(documentReference, context)).thenReturn(true);

        when(context.getWiki()).thenReturn(xwiki);

        this.listener.onEvent(new DocumentCreatedEvent(), document, context);

        // Verify that the document is added to the watchlist
        verify(this.mockStore).addWatchedElement(
            "content author", "authorSpace.authorPage", WatchListStore.ElementType.DOCUMENT, context);
    }
}
