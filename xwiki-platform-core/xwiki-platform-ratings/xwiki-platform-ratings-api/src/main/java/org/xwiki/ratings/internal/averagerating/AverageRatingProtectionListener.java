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
package org.xwiki.ratings.internal.averagerating;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;
import org.xwiki.ratings.events.UpdatingAverageRatingEvent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Make sure that rating average is modified only trough rating API.
 * 
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Singleton
@Named("AverageRatingProtectionListener")
public class AverageRatingProtectionListener extends AbstractEventListener
{
    private static final UpdatingAverageRatingEvent PARENT = new UpdatingAverageRatingEvent();

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

        // If the modification is not part of an official rating cancel it
        if (!this.observationContext.isIn(PARENT)) {
            List<BaseObject> xObjects =
                document.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE);
            XWikiDocument previousDocument = document.getOriginalDocument();

            for (BaseObject ratingXObject : xObjects) {
                if (ratingXObject != null) {
                    int number = ratingXObject.getNumber();
                    BaseObject previousObject = previousDocument
                        .getXObject(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE, number);
                    if (previousObject != null) {
                        ratingXObject.apply(previousObject, true);
                    }
                }
            }
        }
    }
}
