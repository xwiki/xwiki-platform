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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiCreatingEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.BeginEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XARImportingEvent;
import com.xpn.xwiki.plugin.watchlist.WatchListStore.ElementType;
import com.xpn.xwiki.web.Utils;

/**
 * Automatically watch modified documents.
 * 
 * @version $Id$
 */
public class AutomaticWatchModeListener implements EventListener
{
    /**
     * Logging helper object.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AutomaticWatchModeListener.class);

    /**
     * The name of the listener.
     */
    private static final String LISTENER_NAME = "AutomaticWatchModeListener";

    /**
     * The events to match.
     */
    private static final List<Event> LISTENER_EVENTS = new ArrayList<Event>()
    {
        {
            add(new DocumentCreatedEvent());
            add(new DocumentUpdatedEvent());
        }
    };

    /**
     * The skipped events groups matcher.
     */
    private static final BeginEvent SKIPPED_EVENTS = new BeginEvent()
    {
        @Override
        public boolean matches(Object otherEvent)
        {
            return otherEvent instanceof WikiCreatingEvent || otherEvent instanceof XARImportingEvent;
        }
    };

    /**
     * The watchlist storage manager.
     */
    private WatchListStore store;

    /**
     * @param store the watchlist storage manager
     */
    public AutomaticWatchModeListener(WatchListStore store)
    {
        this.store = store;
    }

    /**
     * Automatically watch modified document depending on the configuration.
     * 
     * @param event the observation event we check for a deleted document event
     * @param currentDoc document version after event occurred
     * @param context the XWiki context
     */
    private void documentModifiedHandler(Event event, XWikiDocument currentDoc, XWikiContext context)
    {
        boolean register = false;

        String user = currentDoc.getContentAuthor();
        DocumentReference userReference = currentDoc.getContentAuthorReference();

        AutomaticWatchMode mode = this.store.getAutomaticWatchMode(user, context);

        switch (mode) {
            case ALL:
                register = true;
                break;
            case MAJOR:
                register = !currentDoc.isMinorEdit();
                break;
            case NEW:
                register = event instanceof DocumentCreatedEvent;
                break;
            default:
                break;
        }

        if (register) {
            try {
                if (StringUtils.isNotEmpty(user) && context.getWiki().exists(userReference, context)) {
                    this.store.addWatchedElement(user, currentDoc.getPrefixedFullName(), ElementType.DOCUMENT, context);
                }
            } catch (XWikiException e) {
                LOGGER.warn("Failed to watch document [" + currentDoc.getPrefixedFullName() + "] for user [" + user
                    + "]", e);
            }
        }
    }

    // EventListener

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument currentDoc = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        ObservationContext observationContext = Utils.getComponent(ObservationContext.class);

        // Does not auto-watch imported document, that's not the goal of this feature
        if (!(event instanceof DocumentDeletedEvent) && !observationContext.isIn(SKIPPED_EVENTS)) {
            documentModifiedHandler(event, currentDoc, context);
        }
    }

    @Override
    public List<Event> getEvents()
    {
        return LISTENER_EVENTS;
    }

    @Override
    public String getName()
    {
        return LISTENER_NAME;
    }
}
