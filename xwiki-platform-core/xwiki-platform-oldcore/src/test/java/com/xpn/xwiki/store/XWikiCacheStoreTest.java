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

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.internal.MapCache;
import org.xwiki.model.internal.reference.UidStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static com.xpn.xwiki.test.mockito.OldcoreMatchers.isCacheConfiguration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private Cache<XWikiDocument> cache;

    private Cache<Boolean> existCache;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.oldcore.getMocker().registerMockComponent(RemoteObservationManagerContext.class);
        this.oldcore.getMocker().registerMockComponent(ObservationManager.class);

        CacheManager cacheManager = this.oldcore.getMocker().registerMockComponent(CacheManager.class);
        this.cache = spy(new MapCache<>());
        when(cacheManager.<XWikiDocument>createNewCache(isCacheConfiguration("xwiki.store.pagecache")))
            .thenReturn(this.cache);
        this.existCache = spy(new MapCache<>());
        when(cacheManager.<Boolean>createNewCache(isCacheConfiguration("xwiki.store.pageexistcache")))
            .thenReturn(existCache);
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
        assertTrue(existingDocument.isCached());
        assertSame(existingDocument, this.cache.get("4:wiki5:space4:page0:"));
        assertTrue(this.existCache.get("4:wiki5:space4:page0:"));

        XWikiDocument notExistingDocument = store.loadXWikiDoc(
            new XWikiDocument(new DocumentReference("otherwiki", "space", "nopage")), this.oldcore.getXWikiContext());

        assertTrue(notExistingDocument.isNew());

        // Make sure only the existing document has been put in the cache
        assertSame(existingDocument, this.cache.get("4:wiki5:space4:page0:"));
        String noPageKey = "4:wiki5:space6:nopage0:";
        assertNull(this.cache.get(noPageKey));
        assertTrue(this.existCache.get("4:wiki5:space4:page0:"));
        assertFalse(this.existCache.get(noPageKey));

        store.saveXWikiDoc(existingDocument, this.oldcore.getXWikiContext());

        assertNull(this.cache.get("4:wiki5:space4:page0:"));
        assertFalse(existingDocument.isCached());
    }

    @Test
    void loadXWikiDocWhenModified() throws Exception
    {
        // Save a document
        this.oldcore.getXWikiContext().setWikiId("wiki");
        XWikiDocument documentReference = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.oldcore.getSpyXWiki().saveDocument(documentReference, this.oldcore.getXWikiContext());

        XWikiCacheStore store = new XWikiCacheStore(this.oldcore.getMockStore(), this.oldcore.getXWikiContext());

        XWikiDocument cacheDocument = store.loadXWikiDoc(documentReference, this.oldcore.getXWikiContext());

        assertFalse(cacheDocument.isMetaDataDirty());

        assertSame(cacheDocument, store.loadXWikiDoc(documentReference, this.oldcore.getXWikiContext()));

        cacheDocument.setAttachment("file.ext", new ByteArrayInputStream("content".getBytes()),
            this.oldcore.getXWikiContext());

        assertEquals("Abusive modification of the cached document [wiki:space.page()]", this.logCapture.getMessage(0));

        assertTrue(cacheDocument.isMetaDataDirty());

        XWikiDocument newCachedDocument = store.loadXWikiDoc(documentReference, this.oldcore.getXWikiContext());
        assertNotSame(cacheDocument, newCachedDocument);

        String key = "4:wiki5:space4:page0:";
        assertSame(newCachedDocument, this.cache.get(key));
        assertTrue(this.existCache.get(key));
        // Verify that while the page cache is set twice, the page exists cache is only set once.
        verify(this.existCache).set(eq(key), any());
        verify(this.cache, times(2)).set(eq(key), any());
    }

    @Test
    void documentSavedDuringLoadIsVisibleInNextLoad() throws Exception
    {
        // Save a document
        this.oldcore.getXWikiContext().setWikiId("wiki");
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        XWikiDocument initialDocument = document.clone();

        XWikiStoreInterface hibernateStore = this.oldcore.getMockStore();
        XWikiCacheStore store = new XWikiCacheStore(hibernateStore, this.oldcore.getXWikiContext());
        CompletableFuture<XWikiDocument> arrivedLoadFuture = new CompletableFuture<>();
        CompletableFuture<XWikiDocument> storeLoadFuture = new CompletableFuture<>();

        when(hibernateStore.loadXWikiDoc(any(), any())).then(invocation -> {
            arrivedLoadFuture.complete(document);
            return storeLoadFuture.get(10, TimeUnit.SECONDS);
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try {
            Future<XWikiDocument> firstLoadFuture =
                executorService.submit(() -> store.loadXWikiDoc(document, this.oldcore.getXWikiContext()));

            assertSame(document, arrivedLoadFuture.get(10, TimeUnit.SECONDS));

            XWikiDocument savedDocument = document.clone();
            savedDocument.setTitle("Saved");
            store.saveXWikiDoc(savedDocument, this.oldcore.getXWikiContext());

            storeLoadFuture.complete(initialDocument);

            assertSame(initialDocument, firstLoadFuture.get(10, TimeUnit.SECONDS));

            assertEquals("{}", this.cache.toString());
            verify(hibernateStore).saveXWikiDoc(savedDocument, this.oldcore.getXWikiContext(), true);
            assertEquals(savedDocument, this.oldcore.getDocuments().get(document.getDocumentReferenceWithLocale()));

            when(hibernateStore.loadXWikiDoc(any(), any())).thenReturn(savedDocument);

            XWikiDocument secondLoadDocument = store.loadXWikiDoc(document, this.oldcore.getXWikiContext());
            assertSame(savedDocument, secondLoadDocument);
            // This should call the hibernate store again as the document shouldn't have been put in the cache.
            verify(hibernateStore, times(2)).loadXWikiDoc(document, this.oldcore.getXWikiContext());

            XWikiDocument thirdDocument = store.loadXWikiDoc(document, this.oldcore.getXWikiContext());
            assertSame(savedDocument, thirdDocument);
            // Now the document should be cached, so no more call.
            verify(hibernateStore, times(2)).loadXWikiDoc(document, this.oldcore.getXWikiContext());
        } finally {
            executorService.shutdown();
        }
        assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    void parallelLoadTriggerOnlyOneLoad() throws Exception
    {
        this.oldcore.getXWikiContext().setWikiId("wiki");
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        XWikiDocument initialDocument = document.clone();

        XWikiStoreInterface hibernateStore = this.oldcore.getMockStore();
        XWikiCacheStore store = new XWikiCacheStore(hibernateStore, this.oldcore.getXWikiContext());
        CompletableFuture<XWikiDocument> arrivedLoadFuture = new CompletableFuture<>();
        CompletableFuture<XWikiDocument> storeLoadFuture = new CompletableFuture<>();

        when(hibernateStore.loadXWikiDoc(any(), any()))
            .then(invocation -> {
                arrivedLoadFuture.complete(document);
                return storeLoadFuture.get(10, TimeUnit.SECONDS);
            })
            .thenThrow(new RuntimeException("Load should never be called twice in this test."));

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            Future<XWikiDocument> firstLoadFuture =
                executorService.submit(() -> store.loadXWikiDoc(document, this.oldcore.getXWikiContext()));
            Future<XWikiDocument> secondLoadFuture =
                executorService.submit(() -> store.loadXWikiDoc(document, this.oldcore.getXWikiContext()));

            assertSame(document, arrivedLoadFuture.get(10, TimeUnit.SECONDS));

            // Ensure that both futures wait.
            assertThrows(TimeoutException.class, () -> firstLoadFuture.get(500, TimeUnit.MILLISECONDS));
            assertThrows(TimeoutException.class, () -> secondLoadFuture.get(500, TimeUnit.MILLISECONDS));

            storeLoadFuture.complete(initialDocument);

            assertSame(initialDocument, firstLoadFuture.get(10, TimeUnit.SECONDS));
            assertSame(initialDocument, secondLoadFuture.get(10, TimeUnit.SECONDS));
            verify(hibernateStore).loadXWikiDoc(document, this.oldcore.getXWikiContext());
        } finally {
            executorService.shutdown();
        }
        assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    void loadExceptionIsPropagated() throws Exception
    {
        this.oldcore.getXWikiContext().setWikiId("wiki");
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));

        XWikiStoreInterface hibernateStore = this.oldcore.getMockStore();
        XWikiCacheStore store = new XWikiCacheStore(hibernateStore, this.oldcore.getXWikiContext());

        XWikiException exception = new XWikiException();
        when(hibernateStore.loadXWikiDoc(any(), any())).thenThrow(exception);

        XWikiException actualException =
            assertThrows(XWikiException.class, () -> store.loadXWikiDoc(document, this.oldcore.getXWikiContext()));
        assertSame(exception, actualException);
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

        assertNull(this.cache.get("4:wiki5:space4:page0:"));
        assertNull(this.existCache.get("4:wiki5:space4:page0:"));
    }
}
