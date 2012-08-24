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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
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
        EventConverterManager eventConverterManager = getComponentManager().getInstance(EventConverterManager.class);

        // local -> remote

        LocalEventData localEvent = new LocalEventData();
        localEvent.setEvent(new DocumentUpdatedEvent(new DocumentReference("wiki","space","page")));
        localEvent.setSource(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
        localEvent.setData(getContext());

        RemoteEventData remoteEvent = eventConverterManager.createRemoteEventData(localEvent);

        assertFalse(remoteEvent.getSource() instanceof XWikiDocument);
        assertFalse(remoteEvent.getData() instanceof XWikiContext);

        // serialize/unserialize
        ByteArrayOutputStream sos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(sos);
        oos.writeObject(remoteEvent);
        ByteArrayInputStream sis = new ByteArrayInputStream(sos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(sis);
        remoteEvent = (RemoteEventData) ois.readObject();

        // remote -> local

        LocalEventData localEvent2 = eventConverterManager.createLocalEventData(remoteEvent);

        assertTrue(localEvent2.getSource() instanceof XWikiDocument);
        assertTrue(localEvent2.getData() instanceof XWikiContext);
        assertEquals("wiki", ((XWikiDocument) localEvent2.getSource()).getWikiName());
        assertEquals("space", ((XWikiDocument) localEvent2.getSource()).getSpaceName());
        assertEquals("page", ((XWikiDocument) localEvent2.getSource()).getPageName());
        assertTrue(((XWikiDocument) localEvent2.getSource()).getOriginalDocument().isNew());
    }
}
