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
package org.xwiki.like.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.like.LikeEvent;
import org.xwiki.like.events.LikeRecordableEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LikeEventListener}.
 *
 * @version $Id$
 */
@ComponentTest
public class LikeEventListenerTest
{
    @InjectMockComponents
    private LikeEventListener likeEventListener;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @Test
    void onEvent() throws Exception
    {
        this.likeEventListener.onEvent(null, null, null);
        verify(this.observationManager, never()).notify(any(), any());

        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Page");
        DocumentModelBridge documentModelBridge = mock(DocumentModelBridge.class);
        when(this.documentAccessBridge.getDocumentInstance(entityReference)).thenReturn(documentModelBridge);
        this.likeEventListener.onEvent(new LikeEvent(), null, entityReference);
        verify(this.observationManager).notify(eq(new LikeRecordableEvent()), eq(LikeEventDescriptor.EVENT_SOURCE),
            eq(documentModelBridge));
    }
}
