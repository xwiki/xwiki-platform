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
package org.xwiki.linkchecker.internal;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.transformation.linkchecker.LinkState;
import org.xwiki.rendering.transformation.linkchecker.LinkStateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LinkCheckerEventListener}.
 *
 * @version $Id$
 * @since 3.3M1
 */
@ComponentTest
class LinkCheckerEventListenerTest
{
    @InjectMockComponents
    private LinkCheckerEventListener listener;

    @MockComponent
    private DocumentModelBridge documentModelBridge;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private LinkStateManager linkStateManager;

    @Test
    void testOnEvent() throws Exception
    {
        final DocumentReference reference = new DocumentReference("wiki", "space", "page");

        final Map<String, Map<String, LinkState>> states = new HashMap<String, Map<String, LinkState>>();
        Map<String, LinkState> referenceStates1 = new HashMap<String, LinkState>();
        referenceStates1.put("wiki1:space1.page1", new LinkState(200, System.currentTimeMillis()));
        referenceStates1.put("wiki2:space2.page2", new LinkState(200, System.currentTimeMillis()));
        states.put("url1", referenceStates1);
        Map<String, LinkState> referenceStates2 = new HashMap<String, LinkState>();
        referenceStates2.put("wiki1:space1.page1", new LinkState(200, System.currentTimeMillis()));
        states.put("url2", referenceStates2);
        
        when(this.documentModelBridge.getDocumentReference()).thenReturn(reference);
        when(this.serializer.serialize(reference)).thenReturn("wiki1:space1.page1");
        when(this.linkStateManager.getLinkStates()).thenReturn(states);
        
        this.listener.onEvent(new DocumentUpdatingEvent(), documentModelBridge, null);

        assertEquals(1, states.size());
        assertEquals(1, states.get("url1").size());
        assertNull(states.get("url2"));

        verify(this.documentModelBridge).getDocumentReference();
        verify(this.serializer).serialize(reference);
        verify(this.linkStateManager).getLinkStates();
    }
}
