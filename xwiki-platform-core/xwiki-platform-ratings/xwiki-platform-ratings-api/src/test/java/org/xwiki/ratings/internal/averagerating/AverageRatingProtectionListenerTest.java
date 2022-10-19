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

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.ObservationContext;
import org.xwiki.ratings.events.UpdatingAverageRatingEvent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ElementInterface;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AverageRatingProtectionListener}.
 *
 * @version $Id$
 */
@ComponentTest
public class AverageRatingProtectionListenerTest
{
    @InjectMockComponents
    private AverageRatingProtectionListener listener;

    @MockComponent
    private ObservationContext observationContext;

    @Test
    void onEventWrongContext()
    {
        XWikiDocument sourceDoc = mock(XWikiDocument.class);
        XWikiDocument previousDoc = mock(XWikiDocument.class);
        when(sourceDoc.getOriginalDocument()).thenReturn(previousDoc);

        BaseObject ratingObject1 = mock(BaseObject.class);
        when(ratingObject1.getNumber()).thenReturn(1);
        BaseObject ratingObject2 = mock(BaseObject.class);
        when(ratingObject2.getNumber()).thenReturn(2);
        BaseObject ratingObject3 = mock(BaseObject.class);
        when(ratingObject3.getNumber()).thenReturn(3);

        BaseObject previousObject1 = mock(BaseObject.class);
        BaseObject previousObject2 = mock(BaseObject.class);

        when(this.observationContext.isIn(new UpdatingAverageRatingEvent())).thenReturn(false);
        when(sourceDoc.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(Arrays.asList(ratingObject1, ratingObject2, ratingObject3));

        when(previousDoc.getXObject(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE, 1))
            .thenReturn(previousObject1);
        when(previousDoc.getXObject(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE, 2))
            .thenReturn(previousObject2);

        this.listener.onEvent(new DocumentUpdatingEvent(), sourceDoc, null);
        verify(ratingObject1).apply(previousObject1, true);
        verify(ratingObject2).apply(previousObject2, true);
        verify(ratingObject3, never()).apply(any(ElementInterface.class), anyBoolean());
    }

    @Test
    void onEvent()
    {
        XWikiDocument sourceDoc = mock(XWikiDocument.class);
        when(this.observationContext.isIn(new UpdatingAverageRatingEvent())).thenReturn(true);
        this.listener.onEvent(new DocumentUpdatingEvent(), sourceDoc, null);
        verify(sourceDoc, never()).getXObjects(any(EntityReference.class));
    }

    @Test
    void onEventMissingObject()
    {
        XWikiDocument sourceDoc = mock(XWikiDocument.class);
        XWikiDocument previousDoc = mock(XWikiDocument.class);
        when(sourceDoc.getOriginalDocument()).thenReturn(previousDoc);

        BaseObject ratingObject2 = mock(BaseObject.class);
        when(ratingObject2.getNumber()).thenReturn(2);
        BaseObject ratingObject3 = mock(BaseObject.class);
        when(ratingObject3.getNumber()).thenReturn(3);

        BaseObject previousObject1 = mock(BaseObject.class);
        BaseObject previousObject2 = mock(BaseObject.class);

        when(this.observationContext.isIn(new UpdatingAverageRatingEvent())).thenReturn(false);
        when(sourceDoc.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(Arrays.asList(null, ratingObject2, ratingObject3));

        when(previousDoc.getXObject(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE, 2))
            .thenReturn(previousObject2);

        this.listener.onEvent(new DocumentUpdatingEvent(), sourceDoc, null);
        verify(ratingObject2).apply(previousObject2, true);
        verify(ratingObject3, never()).apply(any(ElementInterface.class), anyBoolean());
    }
}
