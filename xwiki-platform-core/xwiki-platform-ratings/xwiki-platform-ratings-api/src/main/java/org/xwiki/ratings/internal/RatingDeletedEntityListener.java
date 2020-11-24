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
package org.xwiki.ratings.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.RatingsManagerFactory;
import org.xwiki.refactoring.event.DocumentRenamingEvent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;

/**
 * This listener aims at removing any ratings related to the deleted entities.
 * This component listens on {@link DocumentDeletedEvent}, {@link XObjectDeletedEvent} and {@link WikiDeletedEvent},
 * for any of those events it calls {@link RatingsManager#removeRatings(EntityReference)} with the appropriate reference
 * on all instantiated ratings managers.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Singleton
@Named(RatingDeletedEntityListener.NAME)
public class RatingDeletedEntityListener extends AbstractEventListener
{
    static final String NAME = "RatingDeletedEntityListener";
    private static final List<Event> EVENT_LIST = Arrays.asList(
        new XObjectDeletedEvent(),
        new DocumentDeletedEvent(),
        new WikiDeletedEvent());

    private static final DocumentRenamingEvent RENAMING_EVENT = new DocumentRenamingEvent();

    @Inject
    private Logger logger;

    @Inject
    private RatingsManagerFactory ratingsManagerFactory;

    @Inject
    private ObservationContext observationContext;

    /**
     * Default constructor.
     */
    public RatingDeletedEntityListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // if the event is sent because of a rename, we ignore it: in that case RatingMovedEntityListener will be called
        // and will handle properly the changes to be done.
        if (event instanceof DocumentDeletedEvent && !this.observationContext.isIn(RENAMING_EVENT)) {
            XWikiDocument sourceDoc = (XWikiDocument) source;
            this.handleDeletedReference(sourceDoc.getDocumentReference());
        } else if (event instanceof XObjectDeletedEvent) {
            XObjectDeletedEvent xObjectDeletedEvent = (XObjectDeletedEvent) event;
            this.handleDeletedReference(xObjectDeletedEvent.getReference());
        } else if (event instanceof WikiDeletedEvent) {
            WikiDeletedEvent wikiDeletedEvent = (WikiDeletedEvent) event;
            this.handleDeletedReference(new WikiReference(wikiDeletedEvent.getWikiId()));
        }
    }

    private void handleDeletedReference(EntityReference deletedReference)
    {
        try {
            for (RatingsManager manager : this.ratingsManagerFactory.getInstantiatedManagers()) {
                manager.removeRatings(deletedReference);
            }
        } catch (RatingsException e) {
            logger.error("Error while removing ratings related to reference [{}] from ratings: [{}]",
                deletedReference, ExceptionUtils.getRootCause(e));
        }
    }
}
