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
package org.xwiki.store.filesystem.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiAttachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TemporaryAttachmentSession}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
class TemporaryAttachmentSessionTest
{
    private TemporaryAttachmentSession temporaryAttachmentSession;

    @BeforeEach
    void setup()
    {
        this.temporaryAttachmentSession = new TemporaryAttachmentSession("session");
    }

    @Test
    void hasOpenEditionSession()
    {
        DocumentReference documentReference = mock(DocumentReference.class);
        assertFalse(this.temporaryAttachmentSession.hasOpenEditionSession(documentReference));

        TemporaryAttachmentDocumentSession temporaryAttachmentDocumentSession =
            mock(TemporaryAttachmentDocumentSession.class);
        this.temporaryAttachmentSession.getTemporaryAttachmentDocumentSessionMap()
            .put(documentReference, temporaryAttachmentDocumentSession);
        assertTrue(this.temporaryAttachmentSession.hasOpenEditionSession(documentReference));

        this.temporaryAttachmentSession.getTemporaryAttachmentDocumentSessionMap().remove(documentReference);
        assertFalse(this.temporaryAttachmentSession.hasOpenEditionSession(documentReference));
    }

    @Test
    void startEditionSession()
    {
        Cache<XWikiAttachment> cache = mock(Cache.class);
        DocumentReference documentReference = mock(DocumentReference.class);
        TemporaryAttachmentDocumentSession temporaryAttachmentDocumentSession =
            new TemporaryAttachmentDocumentSession("session", documentReference, cache);
        Map<DocumentReference, TemporaryAttachmentDocumentSession> temporaryAttachmentDocumentSessionMap =
            this.temporaryAttachmentSession.getTemporaryAttachmentDocumentSessionMap();
        assertTrue(temporaryAttachmentDocumentSessionMap.isEmpty());

        this.temporaryAttachmentSession.startEditionSession(documentReference, cache);
        assertTrue(temporaryAttachmentDocumentSessionMap.containsKey(documentReference));
        assertEquals(temporaryAttachmentDocumentSession, temporaryAttachmentDocumentSessionMap.get(documentReference));
    }

    @Test
    void dispose()
    {
        Map<DocumentReference, TemporaryAttachmentDocumentSession> map =
            this.temporaryAttachmentSession.getTemporaryAttachmentDocumentSessionMap();

        DocumentReference documentReference1 = mock(DocumentReference.class);
        TemporaryAttachmentDocumentSession session1 = mock(TemporaryAttachmentDocumentSession.class);
        map.put(documentReference1, session1);

        DocumentReference documentReference2 = mock(DocumentReference.class);
        TemporaryAttachmentDocumentSession session2 = mock(TemporaryAttachmentDocumentSession.class);
        map.put(documentReference2, session2);

        this.temporaryAttachmentSession.dispose();
        verify(session1).dispose();
        verify(session2).dispose();
    }

    @Test
    void getCache()
    {
        DocumentReference documentReference = mock(DocumentReference.class);
        assertNull(this.temporaryAttachmentSession.getCache(documentReference));

        TemporaryAttachmentDocumentSession session = mock(TemporaryAttachmentDocumentSession.class);
        this.temporaryAttachmentSession.getTemporaryAttachmentDocumentSessionMap().put(documentReference, session);
        Cache<XWikiAttachment> cache = mock(Cache.class);
        when(session.getAttachmentCache()).thenReturn(cache);

        assertSame(cache, this.temporaryAttachmentSession.getCache(documentReference));
    }

    @Test
    void getFilenames()
    {
        DocumentReference documentReference = mock(DocumentReference.class);
        assertEquals(Collections.emptySet(), this.temporaryAttachmentSession.getFilenames(documentReference));

        TemporaryAttachmentDocumentSession session = mock(TemporaryAttachmentDocumentSession.class);
        this.temporaryAttachmentSession.getTemporaryAttachmentDocumentSessionMap().put(documentReference, session);
        Set<String> filenames = new HashSet<>(Arrays.asList("one", "two", "three"));
        when(session.getFilenames()).thenReturn(filenames);

        assertEquals(filenames, this.temporaryAttachmentSession.getFilenames(documentReference));
    }
}
