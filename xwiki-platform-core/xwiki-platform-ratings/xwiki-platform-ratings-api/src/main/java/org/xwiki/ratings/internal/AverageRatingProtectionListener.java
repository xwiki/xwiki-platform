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

import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.UpdatingRatingEvent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Make sure that rating average is modified only trough rating API.
 * 
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Singleton
@Named("AverageRatingProtectionListener")
public class AverageRatingProtectionListener extends AbstractEventListener
{
    private static final UpdatingRatingEvent PARENT = new UpdatingRatingEvent();

    @Inject
    private ObservationContext observationContext;

    /**
     * Default constructor.
     */
    public AverageRatingProtectionListener()
    {
        super("AverageRatingProtectionListener", new DocumentUpdatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        BaseObject ratingObject = document.getXObject(RatingsManager.AVERAGE_RATINGS_CLASSREFERENCE);

        if (ratingObject != null) {
            // If the modification is not part of an official rating cancel it
            if (!this.observationContext.isIn(PARENT)) {
                XWikiDocument previousDocument = document.getOriginalDocument();
                BaseObject previousObject = previousDocument.getXObject(RatingsManager.AVERAGE_RATINGS_CLASSREFERENCE);

                if (previousObject != null) {
                    ratingObject.apply(previousObject, true);
                }
            }
        }
    }
}
