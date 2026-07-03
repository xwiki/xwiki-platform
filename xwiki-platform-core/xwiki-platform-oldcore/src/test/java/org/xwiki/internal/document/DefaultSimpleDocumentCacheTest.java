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
package org.xwiki.internal.document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultSimpleDocumentCache}.
 *
 * @version $Id$
 */
// Tests can depend on many classes.
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
@ComponentTest
class DefaultSimpleDocumentCacheTest
{
    private static final LRUCacheConfiguration CACHE_CONFIGURATION = new LRUCacheConfiguration("test", 100);

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    private static final String DOCUMENT_REFERENCE_STRING = "wiki:space.page";

    @InjectMockComponents
    private DefaultSimpleDocumentCache<String, Exception> cache;

    @Mock
    private Cache<String> internalCache;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @BeforeEach
    void setUp() throws Exception
    {
        doReturn(this.internalCache).when(this.cacheManager).createNewCache(any());
        when(this.entityReferenceSerializer.serialize(DOCUMENT_REFERENCE)).thenReturn(DOCUMENT_REFERENCE_STRING);
    }

    @Test
    void initializeCache() throws CacheException
    {
        this.cache.initializeCache(CACHE_CONFIGURATION);
        verify(this.cacheManager).createNewCache(CACHE_CONFIGURATION);
        verify(this.observationManager).addListener(any(), eq(EventListener.CACHE_INVALIDATION_DEFAULT_PRIORITY));
    }

    @Test
    void getMissing() throws Exception
    {
        this.cache.initializeCache(CACHE_CONFIGURATION);
        String value = "missing value";
        assertEquals(value, this.cache.get(DOCUMENT_REFERENCE, documentReference -> value));
        verify(this.internalCache).get(DOCUMENT_REFERENCE_STRING);
        verify(this.internalCache).set(DOCUMENT_REFERENCE_STRING, value);
    }

    @Test
    void getExisting() throws Exception
    {
        this.cache.initializeCache(CACHE_CONFIGURATION);
        String value = "stored value";
        when(this.internalCache.get(DOCUMENT_REFERENCE_STRING)).thenReturn(value);
        assertEquals(value, this.cache.get(DOCUMENT_REFERENCE, documentReference -> "should not be called"));
        verify(this.internalCache).get(DOCUMENT_REFERENCE_STRING);
        verify(this.internalCache, never()).set(any(), any());
    }

    @Test
    void getThrows() throws CacheException
    {
        this.cache.initializeCache(CACHE_CONFIGURATION);
        XWikiException exception = new XWikiException();
        XWikiException thrownException = assertThrows(XWikiException.class,
            () -> this.cache.get(DOCUMENT_REFERENCE, documentReference -> {
                throw exception;
            }));
        assertSame(exception, thrownException);
        verify(this.internalCache).get(DOCUMENT_REFERENCE_STRING);
        verify(this.internalCache, never()).set(any(), any());
    }

    @Test
    void removeOnEvent() throws CacheException
    {
        this.cache.initializeCache(CACHE_CONFIGURATION);
        // Capture the event listener that got added.
        ArgumentCaptor<EventListener> listenerCaptor = ArgumentCaptor.captor();
        verify(this.observationManager).addListener(listenerCaptor.capture(),
            eq(EventListener.CACHE_INVALIDATION_DEFAULT_PRIORITY));
        XWikiDocument updatedDocument = new XWikiDocument(DOCUMENT_REFERENCE);
        XWikiContext mockContext = mock();
        listenerCaptor.getValue().onEvent(new DocumentUpdatedEvent(DOCUMENT_REFERENCE), updatedDocument, mockContext);
        verify(this.internalCache).remove(DOCUMENT_REFERENCE_STRING);
    }

    @Test
    void updateWaitsOnRemoval() throws Exception
    {
        this.cache.initializeCache(CACHE_CONFIGURATION);
        String value = "value of updateWaitsOnRemoval";

        CompletableFuture<Void> blockRemovalFuture = new CompletableFuture<>();
        CompletableFuture<Void> arrivedInRemovalFuture = new CompletableFuture<>();
        CompletableFuture<Void> arrivedInGetFuture = new CompletableFuture<>();
        CompletableFuture<Void> arrivedInSetFuture = new CompletableFuture<>();
        CompletableFuture<Void> arrivedInProviderFuture = new CompletableFuture<>();
        CompletableFuture<String> providerFuture = new CompletableFuture<>();

        // Initialize an executor with two threads to allow parallel blocking operations.
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            doAnswer(invocationOnMock -> {
                arrivedInRemovalFuture.complete(null);
                blockRemovalFuture.get();
                return null;
            }).when(this.internalCache).remove(DOCUMENT_REFERENCE_STRING);

            when(this.internalCache.get(DOCUMENT_REFERENCE_STRING)).then(invocationOnMock -> {
                arrivedInGetFuture.complete(null);
                return null;
            });

            doAnswer(invocationOnMock -> {
                arrivedInSetFuture.complete(null);
                return null;
            }).when(this.internalCache).set(DOCUMENT_REFERENCE_STRING, value);

            CompletableFuture<Void> removeFuture =
                CompletableFuture.runAsync(() -> this.cache.remove(DOCUMENT_REFERENCE), executor);

            // Wait for the thread to arrive in the actual remove method of the cache. Only wait for 10 seconds to not
            // block the test forever in case the test should fail.
            arrivedInRemovalFuture.get(10, TimeUnit.SECONDS);

            CompletableFuture<Void> updateFuture = CompletableFuture.runAsync(() -> {
                try {
                    this.cache.get(DOCUMENT_REFERENCE, documentReference -> {
                        arrivedInProviderFuture.complete(null);
                        return providerFuture.get();
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);

            arrivedInGetFuture.get(10, TimeUnit.SECONDS);

            // Ensure that we don't arrive in the provider or the set method even when waiting for a second.
            assertThrows(TimeoutException.class,
                () -> CompletableFuture.anyOf(arrivedInProviderFuture, arrivedInSetFuture).get(1, TimeUnit.SECONDS));

            // Unblock the removal operation.
            blockRemovalFuture.complete(null);

            // Wait for the removal to complete as it should be unblocked now.
            removeFuture.get(10, TimeUnit.SECONDS);

            // Ensure that we're getting into the provider now.
            arrivedInProviderFuture.get(10, TimeUnit.SECONDS);

            // Unblock the provider operation.
            providerFuture.complete(value);

            // Ensure that we're getting into the set method now.
            arrivedInSetFuture.get(10, TimeUnit.SECONDS);

            // Wait for the update operation to complete.
            updateFuture.get(10, TimeUnit.SECONDS);

            verify(this.internalCache).set(DOCUMENT_REFERENCE_STRING, value);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void removalWaitsOnUpdate() throws Exception
    {
        this.cache.initializeCache(CACHE_CONFIGURATION);
        String value = "value of removalWaitsOnUpdate";

        CompletableFuture<Void> arrivedInProviderFuture = new CompletableFuture<>();
        CompletableFuture<String> providerFuture = new CompletableFuture<>();
        CompletableFuture<Void> arrivedInRemovalFuture = new CompletableFuture<>();

        // Initialize an executor with two threads to allow parallel blocking operations.
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            doAnswer(invocationOnMock -> {
                arrivedInRemovalFuture.complete(null);
                return null;
            }).when(this.internalCache).remove(DOCUMENT_REFERENCE_STRING);

            CompletableFuture<Void> updateFuture = CompletableFuture.runAsync(() -> {
                try {
                    this.cache.get(DOCUMENT_REFERENCE, documentReference -> {
                        arrivedInProviderFuture.complete(null);
                        return providerFuture.get(20, TimeUnit.SECONDS);
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);

            // Ensure that the cache is blocked in the provider.
            arrivedInProviderFuture.get(10, TimeUnit.SECONDS);

            CompletableFuture<Void> removeFuture =
                CompletableFuture.runAsync(() -> this.cache.remove(DOCUMENT_REFERENCE), executor);

            // Ensure that we don't arrive in the removal method even when waiting for a second.
            assertThrows(TimeoutException.class, () -> arrivedInRemovalFuture.get(1, TimeUnit.SECONDS));

            // Unblock the set operation.
            providerFuture.complete(value);

            // Wait for the set operation to complete as it should be unblocked now.
            updateFuture.get(10, TimeUnit.SECONDS);

            // Ensure that we're getting into the removal method now.
            arrivedInRemovalFuture.get(10, TimeUnit.SECONDS);

            // Wait for the removal operation to complete.
            removeFuture.get(10, TimeUnit.SECONDS);

            verify(this.internalCache).remove(DOCUMENT_REFERENCE_STRING);
            verify(this.internalCache).set(DOCUMENT_REFERENCE_STRING, value);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Test that two requests to get() with the same key don't block each other, that both can access the cache at the
     * same time and also provide values at the same time.
     */
    @Test
    void parallelGetRequestsDontBlock() throws Exception
    {
        this.cache.initializeCache(CACHE_CONFIGURATION);

        List<String> values = List.of("0", "1");

        List<CompletableFuture<Void>> arrivedInGetFuture =
            List.of(new CompletableFuture<>(), new CompletableFuture<>());
        List<CompletableFuture<Void>> blockGetFuture =
            List.of(new CompletableFuture<>(), new CompletableFuture<>());
        List<CompletableFuture<Void>> arrivedInProviderFuture =
            List.of(new CompletableFuture<>(), new CompletableFuture<>());
        List<CompletableFuture<String>> blockProviderFuture =
            List.of(new CompletableFuture<>(), new CompletableFuture<>());
        List<CompletableFuture<Void>> arrivedInSetFuture =
            List.of(new CompletableFuture<>(), new CompletableFuture<>());
        List<CompletableFuture<Void>> blockSetFuture = List.of(new CompletableFuture<>(), new CompletableFuture<>());

        when(this.internalCache.get(DOCUMENT_REFERENCE_STRING))
            .then(invocationOnMock -> {
                arrivedInGetFuture.get(0).complete(null);
                return blockGetFuture.get(0).get(20, TimeUnit.SECONDS);
            })
            .then(invocationOnMock -> {
                arrivedInGetFuture.get(1).complete(null);
                return blockGetFuture.get(1).get(20, TimeUnit.SECONDS);
            });

        doAnswer(invocationOnMock -> {
            int value = Integer.parseInt(invocationOnMock.getArgument(1));
            arrivedInSetFuture.get(value).complete(null);
            blockSetFuture.get(value).get(20, TimeUnit.SECONDS);
            return null;
        }).when(this.internalCache).set(eq(DOCUMENT_REFERENCE_STRING), any());

        // Initialize an executor with two threads to allow parallel blocking operations.
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<CompletableFuture<Void>> getFuture = new ArrayList<>(2);

            for (int i = 0; i < 2; ++i) {
                int finalI = i;
                getFuture.add(CompletableFuture.runAsync(() -> {
                    try {
                        this.cache.get(DOCUMENT_REFERENCE, documentReference -> {
                            arrivedInProviderFuture.get(finalI).complete(null);
                            return blockProviderFuture.get(finalI).get(20, TimeUnit.SECONDS);
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, executor));

                // Wait for the thread to arrive in the actual get method of the cache. Only wait for 10 seconds to not
                // block the test forever in case the test should fail.
                arrivedInGetFuture.get(i).get(10, TimeUnit.SECONDS);
            }

            for (int i = 0; i < 2; ++i) {
                // Unblock one thread after the other and let it call the provider.
                blockGetFuture.get(i).complete(null);

                // Wait for the thread to arrive in the provider.
                arrivedInProviderFuture.get(i).get(10, TimeUnit.SECONDS);
            }

            for (int i = 1; i >= 0; --i) {
                // Unblock the provider for both threads and let them call the set method. Do it in the reverse order
                // to ensure that it doesn't depend on the order.
                blockProviderFuture.get(i).complete(values.get(i));

                // Wait for the thread to arrive in the set method.
                arrivedInSetFuture.get(i).get(10, TimeUnit.SECONDS);
            }

            for (int i = 0; i < 2; ++i) {
                // Unblock the set method for both threads.
                blockSetFuture.get(i).complete(null);

                // Wait for the thread to complete.
                getFuture.get(i).get(10, TimeUnit.SECONDS);
            }

            for (String value : values) {
                verify(this.internalCache).set(DOCUMENT_REFERENCE_STRING, value);
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
