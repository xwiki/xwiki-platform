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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.RatingsManagerFactory;
import org.xwiki.refactoring.event.DocumentRenamedEvent;

/**
 * This listener aims at updating any ratings related to the moved entities.
 * This component listens on {@link DocumentRenamedEvent} and calls
 * {@link RatingsManager#moveRatings(EntityReference, EntityReference)} with the appropriate references
 * on all instantiated ratings managers.
 *
 * @version $Id$
 * @since 12.10
 */
@Component
@Singleton
@Named(RatingMovedEntityListener.NAME)
public class RatingMovedEntityListener extends AbstractLocalEventListener
{
    static final String NAME = "RatingMovedEntityListener";

    @Inject
    private Logger logger;

    @Inject
    private RatingsManagerFactory ratingsManagerFactory;

    /**
     * Default constructor.
     */
    public RatingMovedEntityListener()
    {
        super(NAME, new DocumentRenamedEvent());
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        DocumentRenamedEvent renamedEvent = (DocumentRenamedEvent) event;
        DocumentReference oldReference = renamedEvent.getSourceReference();
        DocumentReference newReference = renamedEvent.getTargetReference();

        try {
            for (RatingsManager manager : this.ratingsManagerFactory.getInstantiatedManagers()) {
                manager.moveRatings(oldReference, newReference);
            }
        } catch (RatingsException e) {
            logger.error("Error while updating ratings related to old reference [{}] from ratings: [{}]", oldReference,
                ExceptionUtils.getRootCause(e));
        }
    }
}
