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
package org.xwiki.lesscss.internal.cache;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.lesscss.internal.colortheme.NamedColorThemeReference;
import org.xwiki.lesscss.internal.resources.LESSSkinFileResourceReference;
import org.xwiki.lesscss.internal.skin.FSSkinReference;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link DefaultLESSResourcesCache}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
class DefaultLESSResourcesCacheTest
{
    @InjectMockComponents
    private DefaultLESSResourcesCache defaultLESSResourcesCache;

    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private CacheKeyFactory cacheKeyFactory;

    private Cache<String> cache;

    @AfterComponent
    void afterComponents() throws Exception
    {
        this.cache = mock(Cache.class);
        CacheFactory cacheFactory = mock(CacheFactory.class);
        when(this.cacheManager.getCacheFactory()).thenReturn(cacheFactory);
        CacheConfiguration configuration = new CacheConfiguration("lesscss.skinfiles.cache");
        when(cacheFactory.<String>newCache(configuration)).thenReturn(this.cache);
    }

    @BeforeEach
    void setUp()
    {
        LESSSkinFileResourceReference lessSkinFileResourceReference =
            new LESSSkinFileResourceReference("lessResource", null, null);
        when(this.cacheKeyFactory.getCacheKey(lessSkinFileResourceReference, new FSSkinReference("skin"),
            new NamedColorThemeReference("colorTheme"), true)).thenReturn(
            "12_lessResource_4_skin_10_colorTheme");
    }

    private LESSSkinFileResourceReference createLESSSkinFileResourceReference(String fileName)
    {
        return new LESSSkinFileResourceReference(fileName, null, null);
    }

    @Test
    void get()
    {
        // Mock
        when(this.cache.get("12_lessResource_4_skin_10_colorTheme")).thenReturn("Expected output");

        // Test
        String result =
            this.defaultLESSResourcesCache.get(new LESSSkinFileResourceReference("lessResource", null, null),
                new FSSkinReference("skin"), new NamedColorThemeReference("colorTheme"));

        // Verify
        assertEquals("Expected output", result);
    }

    @Test
    void setWithVersion()
    {
        long version = this.defaultLESSResourcesCache.getVersion();

        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("lessResource"),
            new FSSkinReference("skin"), new NamedColorThemeReference("colorTheme"), "css", version);

        verify(this.cache).set("12_lessResource_4_skin_10_colorTheme", "css");
    }

    @Test
    void setWithVersionWhenClearedDuringComputation()
    {
        // A compilation reads the version, then the LESS resource is saved while it is compiled: the cache of the
        // resource is cleared before the result, computed from the old content, is stored.
        long version = this.defaultLESSResourcesCache.getVersion();
        this.defaultLESSResourcesCache.clearFromLESSResource(createLESSSkinFileResourceReference("lessResource"));
        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("lessResource"),
            new FSSkinReference("skin"), new NamedColorThemeReference("colorTheme"), "old css", version);

        verify(this.cache, never()).set(anyString(), any());

        // A compilation started after the clear is cached.
        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("lessResource"),
            new FSSkinReference("skin"), new NamedColorThemeReference("colorTheme"), "new css",
            this.defaultLESSResourcesCache.getVersion());

        verify(this.cache).set("12_lessResource_4_skin_10_colorTheme", "new css");
    }

    @Test
    void clearsChangeTheVersion()
    {
        long version = this.defaultLESSResourcesCache.getVersion();
        this.defaultLESSResourcesCache.clear();
        assertNotEquals(version, this.defaultLESSResourcesCache.getVersion());

        version = this.defaultLESSResourcesCache.getVersion();
        this.defaultLESSResourcesCache.clearFromSkin(new FSSkinReference("skin"));
        assertNotEquals(version, this.defaultLESSResourcesCache.getVersion());

        version = this.defaultLESSResourcesCache.getVersion();
        this.defaultLESSResourcesCache.clearFromColorTheme(new NamedColorThemeReference("colorTheme"));
        assertNotEquals(version, this.defaultLESSResourcesCache.getVersion());

        version = this.defaultLESSResourcesCache.getVersion();
        this.defaultLESSResourcesCache.clearFromLESSResource(createLESSSkinFileResourceReference("lessResource"));
        assertNotEquals(version, this.defaultLESSResourcesCache.getVersion());
    }

    @Test
    void clear()
    {
        // Test
        this.defaultLESSResourcesCache.clear();

        // Verify
        verify(this.cache).removeAll();
    }

    @Test
    void clearFromSkin()
    {
        // Mocks
        LESSSkinFileResourceReference file1 = createLESSSkinFileResourceReference("file1");
        LESSSkinFileResourceReference file2 = createLESSSkinFileResourceReference("file2");
        when(this.cacheKeyFactory.getCacheKey(file1, new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme"), true)).thenReturn("k1");
        when(this.cacheKeyFactory.getCacheKey(file1, new FSSkinReference("skin2"),
            new NamedColorThemeReference("colorTheme"), true)).thenReturn("k3");
        when(this.cacheKeyFactory.getCacheKey(file2, new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme"), true)).thenReturn("k4");

        // Add the first one twice
        store(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme"), "css1");
        store(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme"), "css1");

        // Others
        store(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin2"),
            new NamedColorThemeReference("colorTheme"), "css2");
        store(createLESSSkinFileResourceReference("file2"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme"), "css3");

        // Test
        this.defaultLESSResourcesCache.clearFromSkin(new FSSkinReference("skin1"));

        // Verify
        verify(this.cache).remove("k1");
        verify(this.cache).remove("k4");
        verify(this.cache, never()).remove("k3");
    }

    @Test
    void clearFromColorTheme()
    {
        // Mocks
        LESSSkinFileResourceReference file1 = createLESSSkinFileResourceReference("file1");
        LESSSkinFileResourceReference file2 = createLESSSkinFileResourceReference("file2");

        when(this.cacheKeyFactory.getCacheKey(file1, new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), true)).thenReturn("k1");
        when(this.cacheKeyFactory.getCacheKey(file1, new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme2"), true)).thenReturn("k3");
        when(this.cacheKeyFactory.getCacheKey(file2, new FSSkinReference("skin2"),
            new NamedColorThemeReference("colorTheme1"), true)).thenReturn("k4");
        // Add the first one twice
        store(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), "css1");
        store(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), "css1");

        // Others
        store(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme2"), "css2");
        store(createLESSSkinFileResourceReference("file2"), new FSSkinReference("skin2"),
            new NamedColorThemeReference("colorTheme1"), "css3");

        // Test
        this.defaultLESSResourcesCache.clearFromColorTheme(new NamedColorThemeReference("colorTheme1"));

        // Verify
        verify(this.cache).remove("k1");
        verify(this.cache).remove("k4");
        verify(this.cache, never()).remove("k3");
    }

    @Test
    void clearFromLESSResource()
    {
        // Mocks
        LESSSkinFileResourceReference file1 = createLESSSkinFileResourceReference("file1");
        LESSSkinFileResourceReference file2 = createLESSSkinFileResourceReference("file2");

        when(this.cacheKeyFactory.getCacheKey(file1, new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), true)).thenReturn("k1");
        when(this.cacheKeyFactory.getCacheKey(file2, new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), true)).thenReturn("k3");
        when(this.cacheKeyFactory.getCacheKey(file1, new FSSkinReference("skin2"),
            new NamedColorThemeReference("colorTheme1"), true)).thenReturn("k4");

        // Add the first one twice
        store(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), "css1");
        store(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), "css1");

        // Others
        store(createLESSSkinFileResourceReference("file2"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), "css");
        store(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin2"),
            new NamedColorThemeReference("colorTheme1"), "css3");

        // Test
        this.defaultLESSResourcesCache.clearFromLESSResource(createLESSSkinFileResourceReference("file1"));

        // Verify
        verify(this.cache).remove("k1");
        verify(this.cache).remove("k4");
        verify(this.cache, never()).remove("k3");
    }

    @Test
    void setDuringClearFromLESSResource() throws Exception
    {
        LESSSkinFileResourceReference file1 = createLESSSkinFileResourceReference("file1");
        when(this.cacheKeyFactory.getCacheKey(file1, new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme"), true)).thenReturn("k1");
        when(this.cacheKeyFactory.getCacheKey(file1, new FSSkinReference("skin2"),
            new NamedColorThemeReference("colorTheme"), true)).thenReturn("k2");
        store(file1, new FSSkinReference("skin1"), new NamedColorThemeReference("colorTheme"), "css1");

        // Block the clear of the resource while it removes the first entry.
        CompletableFuture<Void> removing = new CompletableFuture<>();
        CompletableFuture<Void> release = new CompletableFuture<>();
        doAnswer(invocation -> {
            removing.complete(null);
            return release.get(10, TimeUnit.SECONDS);
        }).when(this.cache).remove("k1");

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            Future<?> clearFuture =
                executorService.submit(() -> this.defaultLESSResourcesCache.clearFromLESSResource(file1));
            removing.get(10, TimeUnit.SECONDS);

            // Store another entry of the same resource while the clear is blocked: it must wait for the end of the
            // clear, otherwise its key is registered in the list of keys being cleared and is lost.
            CompletableFuture<Thread> setThread = new CompletableFuture<>();
            Future<?> setFuture = executorService.submit(() -> {
                setThread.complete(Thread.currentThread());
                store(file1, new FSSkinReference("skin2"), new NamedColorThemeReference("colorTheme"), "css2");
            });
            Thread thread = setThread.get(10, TimeUnit.SECONDS);
            await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(10))
                .until(() -> thread.getState() == Thread.State.BLOCKED || setFuture.isDone());
            assertFalse(setFuture.isDone(), "The set didn't wait for the end of the clear");

            release.complete(null);
            clearFuture.get(10, TimeUnit.SECONDS);
            setFuture.get(10, TimeUnit.SECONDS);
        } finally {
            executorService.shutdown();
        }
        assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));

        // The next clear of the resource removes the entry stored during the previous clear.
        this.defaultLESSResourcesCache.clearFromLESSResource(file1);

        verify(this.cache).remove("k2");
    }

    private void store(LESSSkinFileResourceReference lessResourceReference, FSSkinReference skin,
        NamedColorThemeReference colorTheme, String content)
    {
        this.defaultLESSResourcesCache.set(lessResourceReference, skin, colorTheme, content,
            this.defaultLESSResourcesCache.getVersion());
    }
}
