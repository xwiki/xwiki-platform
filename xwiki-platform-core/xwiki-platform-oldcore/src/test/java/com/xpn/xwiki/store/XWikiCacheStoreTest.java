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
package com.xpn.xwiki.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.model.internal.reference.UidStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static com.xpn.xwiki.test.mockito.OldcoreMatchers.isCacheConfiguration;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Validate {@link XWikiCacheStore} behavior.
 * 
 * @version $Id$
 */
@OldcoreTest
@ComponentList(UidStringEntityReferenceSerializer.class)
class XWikiCacheStoreTest
{    
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private Cache<XWikiDocument> cache;

    private Cache<Boolean> existCache;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.oldcore.getMocker().registerMockComponent(RemoteObservationManagerContext.class);
        this.oldcore.getMocker().registerMockComponent(ObservationManager.class);

        CacheManager cacheManager = this.oldcore.getMocker().registerMockComponent(CacheManager.class);
        cache = mock(Cache.class);
        when(cacheManager.<XWikiDocument>createNewCache(isCacheConfiguration("xwiki.store.pagecache"))).thenReturn(
            cache);
        existCache = mock(Cache.class);
        when(cacheManager.<Boolean>createNewCache(isCacheConfiguration("xwiki.store.pageexistcache"))).thenReturn(
            existCache);
    }

    @Test
    void loadXWikiDoc() throws Exception
    {
        // Set current wiki
        this.oldcore.getXWikiContext().setWikiId("wiki");
        this.oldcore.getSpyXWiki().saveDocument(new XWikiDocument(new DocumentReference("wiki", "space", "page")),
            this.oldcore.getXWikiContext());

        // Save a document
        DocumentReference reference = new DocumentReference("otherwiki", "space", "page");

        XWikiCacheStore store = new XWikiCacheStore(this.oldcore.getMockStore(), this.oldcore.getXWikiContext());

        XWikiDocument existingDocument =
            store.loadXWikiDoc(new XWikiDocument(reference), this.oldcore.getXWikiContext());

        assertFalse(existingDocument.isNew());
        verify(this.cache).set(eq("4:wiki5:space4:page0:"), any(XWikiDocument.class));
        verify(this.existCache).set("4:wiki5:space4:page0:", Boolean.TRUE);
        verify(this.cache).get(any());
        verify(this.existCache).get(any());

        verifyNoMoreInteractions(this.cache);
        verifyNoMoreInteractions(this.existCache);

        XWikiDocument notExistingDocument =
            store.loadXWikiDoc(new XWikiDocument(new DocumentReference("otherwiki", "space", "nopage")),
                this.oldcore.getXWikiContext());

        assertTrue(notExistingDocument.isNew());

        // Make sure only the existing document has been put in the cache
        verify(this.cache).set(eq("4:wiki5:space4:page0:"), any(XWikiDocument.class));
        verify(this.existCache).set("4:wiki5:space4:page0:", Boolean.TRUE);
        verify(this.existCache).set("4:wiki5:space6:nopage0:", Boolean.FALSE);
        verify(this.cache, times(2)).get(any());
        verify(this.existCache, times(2)).get(any());

        verifyNoMoreInteractions(this.cache);
        verifyNoMoreInteractions(this.existCache);
    }

    @Test
    void saveXWikiDocumentFailing() throws XWikiException
    {
        // Set current wiki
        this.oldcore.getXWikiContext().setWikiId("wiki");

        // Save a document
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        XWikiCacheStore store = new XWikiCacheStore(this.oldcore.getMockStore(), this.oldcore.getXWikiContext());

        XWikiDocument document = new XWikiDocument(reference);

        doThrow(XWikiException.class).when(this.oldcore.getMockStore()).saveXWikiDoc(document,
            this.oldcore.getXWikiContext(), true);

        assertThrows(XWikiException.class, () -> store.saveXWikiDoc(document, this.oldcore.getXWikiContext(), true));

        verify(this.cache).remove("4:wiki5:space4:page0:");
        verify(this.existCache).remove("4:wiki5:space4:page0:");
    }
}
