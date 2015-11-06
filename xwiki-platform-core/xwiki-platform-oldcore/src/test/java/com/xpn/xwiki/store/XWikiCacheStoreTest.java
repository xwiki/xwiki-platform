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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.model.internal.reference.UidStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcoreRule;

import static com.xpn.xwiki.test.mockito.OldcoreMatchers.isCacheConfiguration;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
@ComponentList(UidStringEntityReferenceSerializer.class)
public class XWikiCacheStoreTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    private Cache<XWikiDocument> cache;

    private Cache<Boolean> existCache;

    @Before
    public void before() throws Exception
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
    public void testLoadXWikiDoc() throws Exception
    {
        // Save a document
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        this.oldcore.getSpyXWiki().saveDocument(new XWikiDocument(reference), this.oldcore.getXWikiContext());

        XWikiCacheStore store = new XWikiCacheStore(this.oldcore.getMockStore(), this.oldcore.getXWikiContext());

        XWikiDocument existingDocument =
            store.loadXWikiDoc(new XWikiDocument(reference), this.oldcore.getXWikiContext());

        assertFalse(existingDocument.isNew());
        verify(this.cache).set(eq(existingDocument.getKey()), any(XWikiDocument.class));
        verify(this.existCache).set(existingDocument.getKey(), Boolean.TRUE);
        verify(this.cache).get(anyString());
        verify(this.existCache).get(anyString());

        verifyNoMoreInteractions(this.cache);
        verifyNoMoreInteractions(this.existCache);

        XWikiDocument notExistingDocument =
            store.loadXWikiDoc(new XWikiDocument(new DocumentReference("wiki", "space", "nopage")),
                this.oldcore.getXWikiContext());

        assertTrue(notExistingDocument.isNew());

        // Make sure only the existing document has been put in the cache
        verify(this.cache).set(eq(existingDocument.getKey()), any(XWikiDocument.class));
        verify(this.existCache).set(existingDocument.getKey(), Boolean.TRUE);
        verify(this.existCache).set(notExistingDocument.getKey(), Boolean.FALSE);
        verify(this.cache, times(2)).get(anyString());
        verify(this.existCache, times(2)).get(anyString());

        verifyNoMoreInteractions(this.cache);
        verifyNoMoreInteractions(this.existCache);
    }
}
