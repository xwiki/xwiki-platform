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
package org.xwiki.notifications.rest.internal;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.internal.MapCache;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.notifiers.internal.DefaultNotificationCacheManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link NotificationEventExecutor}.
 *
 * @version $Id$
 */
@ComponentTest
class NotificationEventExecutorTest
{
    private static final String CACHE_KEY = "mykey";

    /**
     * A non-zero epoch, so that the tests fail if the epoch isn't actually passed from the cache manager to the cache
     * accesses.
     */
    private static final long EPOCH = 42L;

    @InjectMockComponents
    private NotificationEventExecutor eventExecutor;

    @MockComponent
    private DefaultNotificationCacheManager notificationCacheManager;

    @MockComponent
    private NotificationConfiguration notificationConfiguration;

    @MockComponent
    private ExecutionContextManager contextManager;

    @MockComponent
    private Execution execution;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private CacheManager cacheManager;

    private final AtomicLong epoch = new AtomicLong(EPOCH);

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        // The executor is only used when the pool size is greater than zero.
        when(this.notificationConfiguration.getRESTPoolSize()).thenReturn(1);
        Cache<Object> shortCache = new MapCache<>();
        when(this.cacheManager.<Object>createNewCache(any())).thenReturn(shortCache);
        XWikiContext xcontextMock = mock();
        when(this.xcontextProvider.get()).thenReturn(xcontextMock);
        when(this.notificationCacheManager.getEpoch()).thenAnswer(invocation -> this.epoch.get());
    }

    @Test
    void submitStoresResultWithTheEpoch() throws Exception
    {
        List<Object> events = List.of(mock(Event.class), mock(Event.class));

        assertEquals(events, this.eventExecutor.submit(CACHE_KEY, () -> events, false, false, true));

        // Once in submit() to check the cache before queuing anything, once in the callable entry itself.
        verify(this.notificationCacheManager, times(2)).getFromCache(CACHE_KEY, false, true, EPOCH);
        verify(this.notificationCacheManager).setInCache(CACHE_KEY, events, false, true, EPOCH);
    }

    /**
     * Ensure that the epoch used to store the result is the one that was current before the events were retrieved, so
     * that a result computed from events that changed in the meantime isn't stored for the new epoch.
     */
    @Test
    void submitWithEpochChangeWhileRetrievingEvents() throws Exception
    {
        List<Object> events = List.of(mock(Event.class), mock(Event.class));

        // Simulate a cache flush that happens while the events are being retrieved.
        Callable<List> callable = () -> {
            this.epoch.incrementAndGet();
            return events;
        };

        assertEquals(events, this.eventExecutor.submit(CACHE_KEY, callable, false, false, true));

        verify(this.notificationCacheManager).setInCache(CACHE_KEY, events, false, true, EPOCH);
        verify(this.notificationCacheManager, never()).setInCache(any(), any(), anyBoolean(), anyBoolean(),
            eq(EPOCH + 1));
    }

    @Test
    void submitReturnsCachedResultForTheCurrentEpoch() throws Exception
    {
        when(this.notificationCacheManager.getFromCache(CACHE_KEY, true, true, EPOCH)).thenReturn(5);

        Callable<List> callable = () -> {
            throw new AssertionError("The callable must not be executed when the result is cached.");
        };

        assertEquals(5, this.eventExecutor.submit(CACHE_KEY, callable, false, true, true));

        verify(this.notificationCacheManager, never()).setInCache(any(), any(), anyBoolean(), anyBoolean(),
            anyLong());
    }
}
