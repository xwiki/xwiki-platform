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

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.RatingsManagerFactory;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RatingDeletedEntityListener}.
 *
 * @version $Id$
 */
@ComponentTest
public class RatingDeletedEntityListenerTest
{
    @InjectMockComponents
    private RatingDeletedEntityListener listener;

    @MockComponent
    private RatingsManagerFactory ratingsManagerFactory;

    @Test
    void onDocumentDeletedEvent() throws Exception
    {
        RatingsManager manager1 = mock(RatingsManager.class);
        RatingsManager manager2 = mock(RatingsManager.class);
        when(ratingsManagerFactory.getInstantiatedManagers()).thenReturn(Arrays.asList(manager1, manager2));

        XWikiDocument sourceDoc = mock(XWikiDocument.class);
        DocumentReference reference = mock(DocumentReference.class);
        when(sourceDoc.getDocumentReference()).thenReturn(reference);

        this.listener.onEvent(new DocumentDeletedEvent(reference), sourceDoc, null);
        verify(manager1).removeRatings(reference);
        verify(manager2).removeRatings(reference);
    }

    @Test
    void onXObjectDeletedEventEvent() throws Exception
    {
        RatingsManager manager1 = mock(RatingsManager.class);
        RatingsManager manager2 = mock(RatingsManager.class);
        when(ratingsManagerFactory.getInstantiatedManagers()).thenReturn(Arrays.asList(manager1, manager2));

        EntityReference reference = mock(EntityReference.class);
        this.listener.onEvent(new XObjectDeletedEvent(reference), null, null);
        verify(manager1).removeRatings(reference);
        verify(manager2).removeRatings(reference);
    }

    @Test
    void onWikiDeletedEventEvent() throws Exception
    {
        RatingsManager manager1 = mock(RatingsManager.class);
        RatingsManager manager2 = mock(RatingsManager.class);
        when(ratingsManagerFactory.getInstantiatedManagers()).thenReturn(Arrays.asList(manager1, manager2));

        EntityReference reference = new WikiReference("foobar");
        this.listener.onEvent(new WikiDeletedEvent("foobar"), null, null);
        verify(manager1).removeRatings(reference);
        verify(manager2).removeRatings(reference);
    }
}
