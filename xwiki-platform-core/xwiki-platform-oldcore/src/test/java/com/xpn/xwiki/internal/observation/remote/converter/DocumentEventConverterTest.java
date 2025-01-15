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

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.internal.converter.DefaultEventConverterManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link DocumentEventConverter};
 * 
 * @version $Id$
 */
@OldcoreTest
@ComponentList(DocumentEventConverter.class)
class DocumentEventConverterTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private DefaultEventConverterManager converterManager;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Test
    void testConvertCreatedDocument() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        // Setup of document which just been created and recived by a document event listener
        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(documentReference, this.oldcore.getXWikiContext());
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());
        document = document.clone();
        document.setOriginalDocument(new XWikiDocument(documentReference));

        // local -> remote

        LocalEventData localEvent = new LocalEventData();
        localEvent.setEvent(new DocumentUpdatedEvent());
        localEvent.setSource(document);
        localEvent.setData(this.oldcore.getXWikiContext());

        RemoteEventData remoteEvent = this.converterManager.createRemoteEventData(localEvent);

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

        LocalEventData localEvent2 = this.converterManager.createLocalEventData(remoteEvent);

        assertTrue(localEvent2.getSource() instanceof XWikiDocument);
        assertTrue(localEvent2.getData() instanceof XWikiContext);
        assertEquals(documentReference, ((XWikiDocument) localEvent2.getSource()).getDocumentReference());
        assertEquals("space", ((XWikiDocument) localEvent2.getSource()).getSpaceName());
        assertEquals("page", ((XWikiDocument) localEvent2.getSource()).getPageName());
        assertTrue(((XWikiDocument) localEvent2.getSource()).getOriginalDocument().isNew());
        assertNotSame(this.oldcore.getSpyXWiki().getDocument(documentReference, this.oldcore.getXWikiContext()),
            localEvent2.getSource());
    }

    @Test
    void testConvertWithOriginalDocNull() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        // local -> remote

        LocalEventData localEvent = new LocalEventData();
        localEvent.setEvent(new DocumentUpdatedEvent());
        localEvent.setSource(new XWikiDocument(documentReference));
        localEvent.setData(this.oldcore.getXWikiContext());

        RemoteEventData remoteEvent = this.converterManager.createRemoteEventData(localEvent);

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

        LocalEventData localEvent2 = this.converterManager.createLocalEventData(remoteEvent);

        assertTrue(localEvent2.getSource() instanceof XWikiDocument);
        assertTrue(localEvent2.getData() instanceof XWikiContext);
        assertEquals(documentReference, ((XWikiDocument) localEvent2.getSource()).getDocumentReference());
        assertEquals("space", ((XWikiDocument) localEvent2.getSource()).getSpaceName());
        assertEquals("page", ((XWikiDocument) localEvent2.getSource()).getPageName());
        assertTrue(((XWikiDocument) localEvent2.getSource()).getOriginalDocument().isNew());
        assertNotSame(this.oldcore.getSpyXWiki().getDocument(documentReference, this.oldcore.getXWikiContext()),
            localEvent2.getSource());
    }
}
