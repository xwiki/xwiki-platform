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
package org.xwiki.watchlist.internal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.WikiCreatingEvent;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.internal.DefaultObservationContext;
import org.xwiki.observation.internal.ObservationContextListener;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.watchlist.WatchListConfiguration;
import org.xwiki.watchlist.internal.api.AutomaticWatchMode;
import org.xwiki.watchlist.internal.api.WatchListStore;
import org.xwiki.watchlist.internal.api.WatchedElementType;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XARImportingEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AutomaticWatchModeListener}.
 * 
 * @version $Id$
 */
@ComponentList({ObservationContextListener.class, DefaultExecution.class, DefaultObservationContext.class})
public class AutomaticWatchModeListenerTest
{
    private WatchListStore mockStore;

    private EventListener observationContextListener;

    @Rule
    public final MockitoComponentMockingRule<EventListener> mocker = new MockitoComponentMockingRule<EventListener>(
        AutomaticWatchModeListener.class);

    private WatchListConfiguration configuration;

    @Before
    public void setUp() throws Exception
    {
        this.mockStore = mocker.getInstance(WatchListStore.class);

        // Make sure we have an Execution Context since the observationContextListener will store current events in it
        Execution execution = mocker.getInstance(Execution.class);
        execution.setContext(new ExecutionContext());

        this.observationContextListener = mocker.getInstance(EventListener.class, "ObservationContextListener");

        this.configuration = mocker.getInstance(WatchListConfiguration.class);
        when(this.configuration.isEnabled()).thenReturn(true);
    }

    /**
     * Verify that we don't do anything when the current event is inside a WikiCreatingEvent.
     */
    @Test
    public void onEventWhenInContextOfWikiCreatingEvent() throws Exception
    {
        // We simulate a WikiCreatingEvent in the Execution Context
        this.observationContextListener.onEvent(new WikiCreatingEvent(), null, null);

        mocker.getComponentUnderTest().onEvent(new DocumentCreatedEvent(), null, null);

        verify(mockStore, never()).getAutomaticWatchMode(any());
        verify(mockStore, never()).addWatchedElement(any(), any(), any(WatchedElementType.class));
    }

    /**
     * Verify that we don't do anything when the current event is inside a XARImportingEvent.
     */
    @Test
    public void onEventWhenInContextOXARImportingEvent() throws Exception
    {
        // We simulate a XARImportingEvent in the Execution Context
        this.observationContextListener.onEvent(new XARImportingEvent(), null, null);

        mocker.getComponentUnderTest().onEvent(new DocumentCreatedEvent(), null, null);

        verify(mockStore, never()).getAutomaticWatchMode(any());
        verify(mockStore, never()).addWatchedElement(any(), any(), any(WatchedElementType.class));
    }

    /**
     * Verify that we don't do anything when the watchlist is disabled.
     */
    @Test
    public void onEventWhenWatchListDisabled() throws Exception
    {
        when(this.configuration.isEnabled()).thenReturn(false);
        mocker.getComponentUnderTest().onEvent(new DocumentCreatedEvent(), null, null);

        verify(mockStore, never()).getAutomaticWatchMode(any());
        verify(mockStore, never()).addWatchedElement(any(), any(), any(WatchedElementType.class));
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

        when(this.mockStore.getAutomaticWatchMode("content author")).thenReturn(AutomaticWatchMode.ALL);

        XWiki xwiki = mock(XWiki.class);
        when(xwiki.exists(documentReference, context)).thenReturn(true);

        when(context.getWiki()).thenReturn(xwiki);

        mocker.getComponentUnderTest().onEvent(new DocumentCreatedEvent(), document, context);

        // Verify that the document is added to the watchlist
        verify(this.mockStore).addWatchedElement("content author", "authorSpace.authorPage",
            org.xwiki.watchlist.internal.api.WatchedElementType.DOCUMENT);
    }
}
