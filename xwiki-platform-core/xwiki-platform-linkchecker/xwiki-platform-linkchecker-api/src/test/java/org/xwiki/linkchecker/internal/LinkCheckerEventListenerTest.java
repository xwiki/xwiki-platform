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

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.transformation.linkchecker.LinkState;
import org.xwiki.rendering.transformation.linkchecker.LinkStateManager;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Unit tests for {@link LinkCheckerEventListener}.
 *
 * @version $Id$
 * @since 3.3M1
 */
public class LinkCheckerEventListenerTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private LinkCheckerEventListener listener;
    
    @Test
    public void testOnEvent() throws Exception
    {
        final DocumentModelBridge documentModelBridge = getMockery().mock(DocumentModelBridge.class);
        final EntityReferenceSerializer serializer = getComponentManager().getInstance(
            EntityReferenceSerializer.TYPE_STRING);
        final DocumentReference reference = new DocumentReference("wiki", "space", "page");
        final LinkStateManager linkStateManager = getComponentManager().getInstance(LinkStateManager.class);

        final Map<String, Map<String, LinkState>> states = new HashMap<String, Map<String, LinkState>>();
        Map<String, LinkState> referenceStates1 = new HashMap<String, LinkState>();
        referenceStates1.put("wiki1:space1.page1", new LinkState(200, System.currentTimeMillis()));
        referenceStates1.put("wiki2:space2.page2", new LinkState(200, System.currentTimeMillis()));
        states.put("url1", referenceStates1);
        Map<String, LinkState> referenceStates2 = new HashMap<String, LinkState>();
        referenceStates2.put("wiki1:space1.page1", new LinkState(200, System.currentTimeMillis()));
        states.put("url2", referenceStates2);
        
        getMockery().checking(new Expectations() {{
            oneOf(documentModelBridge).getDocumentReference();
            will(returnValue(reference));
            oneOf(serializer).serialize(reference);
            will(returnValue("wiki1:space1.page1"));
            oneOf(linkStateManager).getLinkStates();
            will(returnValue(states));
        }});
        
        this.listener.onEvent(new DocumentUpdatingEvent(), documentModelBridge, null);
        
        Assert.assertEquals(1, states.size());
        Assert.assertEquals(1, states.get("url1").size());
        Assert.assertNull(states.get("url2"));
    }
}
