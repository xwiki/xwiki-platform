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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiCreatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.JobEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.BeginEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.watchlist.internal.api.AutomaticWatchMode;
import org.xwiki.watchlist.internal.api.WatchListStore;
import org.xwiki.watchlist.internal.api.WatchedElementType;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XARImportingEvent;

/**
 * Automatically watch modified documents.
 * 
 * @version $Id$
 */
@Component
@Named(AutomaticWatchModeListener.LISTENER_NAME)
@Singleton
public class AutomaticWatchModeListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String LISTENER_NAME = "AutomaticWatchModeListener";

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
     * The skipped events group matcher.
     */
    private static final BeginEvent SKIPPED_EVENTS = new BeginEvent()
    {
        @Override
        public boolean matches(Object otherEvent)
        {
            // 1. Skip bulk user actions as do not reflect expressed interest in the particular pages.

            // 2. Skip events coming from background jobs, since they are not attributed and are not relevant to a
            // particular user's behavior. E.g.: XWiki initialization job (mandatory document initializers, plugin
            // initializers, etc.)

            return otherEvent instanceof WikiCreatingEvent || otherEvent instanceof XARImportingEvent
                || otherEvent instanceof JobEvent;
        }
    };

    /**
     * Logging helper object.
     */
    @Inject
    private Logger logger;

    /**
     * The watchlist storage manager.
     */
    @Inject
    private WatchListStore store;

    /**
     * Used to detect if certain events are not independent, i.e. executed in the context of other events, case in which
     * they should be skipped.
     */
    @Inject
    private ObservationContext observationContext;

    /**
     * Default constructor.
     */
    public AutomaticWatchModeListener()
    {
        super(LISTENER_NAME, LISTENER_EVENTS);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument currentDoc = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        // Does not auto-watch updated or created documents when they are in the context of a XAR import or a Wiki
        // creation.
        if (!observationContext.isIn(SKIPPED_EVENTS)) {
            documentModifiedHandler(event, currentDoc, context);
        }
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
        String user = currentDoc.getContentAuthor();
        DocumentReference userReference = currentDoc.getContentAuthorReference();

        // Avoid handling guests or the superadmin user.
        if (userReference == null || !context.getWiki().exists(userReference, context)) {
            return;
        }

        // Determine if the current event should be registered, based on the user's prefereces.
        boolean register = false;

        AutomaticWatchMode mode = this.store.getAutomaticWatchMode(user);
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
                this.store.addWatchedElement(user, currentDoc.getPrefixedFullName(), WatchedElementType.DOCUMENT);
            } catch (XWikiException e) {
                logger.warn("Failed to watch document [{}] for user [{}]", currentDoc.getPrefixedFullName(), user, e);
            }
        }
    }
}
