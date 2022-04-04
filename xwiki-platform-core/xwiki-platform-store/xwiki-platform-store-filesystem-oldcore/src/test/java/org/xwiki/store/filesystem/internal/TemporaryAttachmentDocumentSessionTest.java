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

import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheEntry;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.event.CacheEntryListener;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiAttachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TemporaryAttachmentDocumentSession}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
class TemporaryAttachmentDocumentSessionTest
{
    private CacheEntryListener<XWikiAttachment> listener;

    @Test
    void filenameSynchronization()
    {
        Cache<XWikiAttachment> cache = mock(Cache.class);
        doAnswer(invocationOnMock -> {
            listener = invocationOnMock.getArgument(0);
            return null;
        }).when(cache).addCacheEntryListener(any(CacheEntryListener.class));
        TemporaryAttachmentDocumentSession temporaryAttachmentDocumentSession =
            new TemporaryAttachmentDocumentSession("session", mock(DocumentReference.class), cache);

        CacheEntryEvent event = mock(CacheEntryEvent.class);
        CacheEntry cacheEntry = mock(CacheEntry.class);
        when(event.getEntry()).thenReturn(cacheEntry);
        when(cacheEntry.getKey())
            .thenReturn("filename1")
            .thenReturn("filename2")
            .thenReturn("filename1")
            .thenReturn("filename2");

        listener.cacheEntryAdded(event);
        assertEquals(Collections.singleton("filename1"), temporaryAttachmentDocumentSession.getFilenames());

        listener.cacheEntryModified(event);
        assertEquals(Collections.singleton("filename1"), temporaryAttachmentDocumentSession.getFilenames());

        listener.cacheEntryAdded(event);
        assertEquals(new HashSet<>(Arrays.asList("filename1", "filename2")),
            temporaryAttachmentDocumentSession.getFilenames());

        listener.cacheEntryRemoved(event);
        assertEquals(Collections.singleton("filename2"), temporaryAttachmentDocumentSession.getFilenames());

        listener.cacheEntryRemoved(event);
        assertEquals(Collections.emptySet(), temporaryAttachmentDocumentSession.getFilenames());
    }

    @Test
    void dispose()
    {
        Cache<XWikiAttachment> cache = mock(Cache.class);
        TemporaryAttachmentDocumentSession temporaryAttachmentDocumentSession =
            new TemporaryAttachmentDocumentSession("session", mock(DocumentReference.class), cache);
        temporaryAttachmentDocumentSession.dispose();
        verify(cache).dispose();
    }
}
