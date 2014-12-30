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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.ratings.ConfiguredProvider;
import org.xwiki.ratings.ReputationAlgorithm;
import org.xwiki.ratings.UpdateRatingEvent;

/**
 * @version $Id$
 */
@Component
@Named("updatereputation")
@Singleton
public class UpdateReputationListener implements EventListener
{
    @Inject
    private ConfiguredProvider<ReputationAlgorithm> reputationAlgorithm;

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(new UpdateRatingEvent());
    }

    @Override
    public String getName()
    {
        return "updatereputation";
    }

    @Override
    public void onEvent(Event event, Object arg1, Object arg2)
    {
        UpdateRatingEvent ratingEvent = (UpdateRatingEvent) event;
        DocumentReference documentRef = ratingEvent.getDocumentReference();
        reputationAlgorithm.get(documentRef).updateReputation(documentRef, ratingEvent.getNewRating(),
            ratingEvent.getOldRating());
    }

}
