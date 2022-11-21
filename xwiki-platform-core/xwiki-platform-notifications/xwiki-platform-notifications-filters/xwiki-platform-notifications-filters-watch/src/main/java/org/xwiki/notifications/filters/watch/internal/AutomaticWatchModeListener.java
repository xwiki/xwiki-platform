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
package org.xwiki.notifications.filters.watch.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.watch.WatchedEntitiesConfiguration;
import org.xwiki.notifications.filters.watch.WatchedEntitiesManager;
import org.xwiki.notifications.filters.watch.WatchedEntityFactory;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.BeginEvent;
import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Automatically watch modified documents.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named(AutomaticWatchModeListener.LISTENER_NAME)
@Singleton
public class AutomaticWatchModeListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String LISTENER_NAME = "AutomaticNotificationsWatchModeListener";

    /**
     * The skipped events group matcher.
     */
    private static final BeginEvent SKIPPED_EVENTS = event -> event instanceof BeginFoldEvent;

    /**
     * The events to match.
     */
    private static final List<Event> LISTENER_EVENTS = Arrays.asList(new DocumentCreatedEvent(),
            new DocumentUpdatedEvent());

    /**
     * Used to detect if certain events are not independent, i.e. executed in the context of other events, case in which
     * they should be skipped.
     */
    @Inject
    private ObservationContext observationContext;

    @Inject
    private WatchedEntitiesManager watchedEntitiesManager;

    @Inject
    private WatchedEntityFactory factory;

    @Inject
    private WatchedEntitiesConfiguration configuration;

    @Inject
    private Logger logger;

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
        // Does not auto-watch updated or created documents when they are in the context of other events.
        if (configuration.isEnabled() && !observationContext.isIn(SKIPPED_EVENTS)) {
            documentModifiedHandler(event, (XWikiDocument) source, (XWikiContext) data);
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
        DocumentReference userReference = currentDoc.getAuthorReference();

        // Avoid handling guests or the superadmin user.
        // No automatic filter for the user page, because it causes an infinite loop
        // user page updated -> filter created -> user page updated -> ...
        // But the user can still manually mark its page as watched
        try {
            if (userReference == null || !context.getWiki().exists(userReference, context)
                || userReference.equals(currentDoc.getDocumentReference())) {
                return;
            }
        } catch (XWikiException e) {
            this.logger.error("Failed to check if user with reference [{}] exists", userReference, e);

            return;
        }

        // Determine if the current event should be registered, based on the user's preferences.
        boolean register = shouldRegister(event, currentDoc, userReference);

        if (register) {
            try {
                watchedEntitiesManager.watchEntity(
                        factory.createWatchedLocationReference(currentDoc.getDocumentReference()), userReference);
            } catch (NotificationException e) {
                logger.warn("Failed to watch document [{}] for user [{}]", currentDoc.getDocumentReference(),
                        userReference, e);
            }
        }
    }

    private boolean shouldRegister(Event event, XWikiDocument currentDoc, DocumentReference userReference)
    {
        switch (configuration.getAutomaticWatchMode(userReference)) {
            case NONE:
                return false;
            case ALL:
                return true;
            case MAJOR:
                return !currentDoc.isMinorEdit();
            case NEW:
                return event instanceof DocumentCreatedEvent;
            default:
                return false;
        }
    }
}
