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
package com.xpn.xwiki.internal.observation.remote.converter;

import org.xwiki.observation.event.DocumentUpdateEvent;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.EventConverterManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Validate {@link DocumentEventConverter};
 * 
 * @version $Id$
 */
public class DocumentEventConverterTest extends AbstractBridgedXWikiComponentTestCase
{
    public void testConvertWithOriginalDocNull() throws Exception
    {
        EventConverterManager eventConverterManager = getComponentManager().lookup(EventConverterManager.class);

        LocalEventData localEvent = new LocalEventData();
        localEvent.setEvent(new DocumentUpdateEvent("wiki:space.page"));
        localEvent.setSource(new XWikiDocument("wiki", "space", "page"));
        localEvent.setData(new XWikiContext());

        RemoteEventData remoteEvent = eventConverterManager.createRemoteEventData(localEvent);

        assertFalse(remoteEvent.getSource() instanceof XWikiDocument);
        assertFalse(remoteEvent.getData() instanceof XWikiContext);

        LocalEventData localEvent2 = eventConverterManager.createLocalEventData(remoteEvent);

        assertTrue(localEvent2.getSource() instanceof XWikiDocument);
        assertTrue(localEvent2.getData() instanceof XWikiContext);
        assertEquals("wiki", ((XWikiDocument) localEvent2.getSource()).getWikiName());
        assertEquals("space", ((XWikiDocument) localEvent2.getSource()).getSpaceName());
        assertEquals("page", ((XWikiDocument) localEvent2.getSource()).getPageName());
        assertTrue(((XWikiDocument) localEvent2.getSource()).getOriginalDocument().isNew());
    }
}
