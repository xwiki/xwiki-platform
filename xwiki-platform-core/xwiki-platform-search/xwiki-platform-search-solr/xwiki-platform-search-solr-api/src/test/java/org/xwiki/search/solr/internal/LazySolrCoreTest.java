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
package org.xwiki.search.solr.internal;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LazySolrCore}.
 *
 * @version $Id$
 */
@ComponentTest
class LazySolrCoreTest
{
    private static final String CORE_NAME = "search";

    private static final String UNAVAILABLE_MESSAGE = "The Solr core [search] is not available: [%s]. Check that the"
        + " Solr server can be reached and that the core exists on it. The core will be requested again when needed.";

    private static final String AVAILABLE_MESSAGE = "The Solr core [search] is available again";

    private static final String MISSING_CORE_MESSAGE = "The Solr core [search] could not be found: with a remote Solr"
        + " server it must be created beforehand, with the name [<corePrefix>_search_<Solr major version>]";

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG, LazySolrCore.class.getName());

    @InjectMockComponents
    private LazySolrCore lazyCore;

    @MockComponent
    private Solr solr;

    @MockComponent
    private SolrConfiguration configuration;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private final XWikiSolrCore core = mock();

    @BeforeEach
    void beforeEach()
    {
        when(this.configuration.getServerType()).thenReturn(RemoteSolr.TYPE);
    }

    private static String unavailable(String rootCause)
    {
        return String.format(UNAVAILABLE_MESSAGE, rootCause);
    }

    @Test
    void isPerLookup() throws ComponentLookupException
    {
        // Each component using a core gets its own instance, which holds the state of that core.
        assertNotSame(this.componentManager.getInstance(LazySolrCore.class),
            this.componentManager.getInstance(LazySolrCore.class));
    }

    @Test
    void getResolvesTheCoreOnce() throws Exception
    {
        when(this.configuration.getServerType()).thenReturn(DefaultSolrConfiguration.SOLR_EMBEDDED_TYPE);
        when(this.solr.getCore(CORE_NAME)).thenReturn(this.core);

        assertSame(this.core, this.lazyCore.initialize(CORE_NAME));
        assertSame(this.core, this.lazyCore.get());
        assertSame(this.core, this.lazyCore.get());

        verify(this.solr).getCore(CORE_NAME);
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void initializeWithRemoteSolrWhenAvailable() throws Exception
    {
        when(this.solr.getCore(CORE_NAME)).thenReturn(this.core);

        assertSame(this.core, this.lazyCore.initialize(CORE_NAME));
        assertSame(this.core, this.lazyCore.get());

        verify(this.solr).getCore(CORE_NAME);
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void initializeWithRemoteSolrIsTolerant() throws Exception
    {
        when(this.solr.getCore(CORE_NAME)).thenThrow(new SolrException("Connection refused")).thenReturn(this.core);
        this.lazyCore.setRetryDelay(0);

        // The failure is logged, not thrown, and the core is requested again when needed.
        assertNull(this.lazyCore.initialize(CORE_NAME));
        assertEquals(unavailable("SolrException: Connection refused"), this.logCapture.getMessage(0));

        assertSame(this.core, this.lazyCore.get());
        assertEquals(AVAILABLE_MESSAGE, this.logCapture.getMessage(1));
        verify(this.solr, times(2)).getCore(CORE_NAME);
    }

    @Test
    void initializeWithEmbeddedSolrIsStrict() throws Exception
    {
        when(this.configuration.getServerType()).thenReturn(DefaultSolrConfiguration.SOLR_EMBEDDED_TYPE);
        SolrException failure = new SolrException("Failed to load the core");
        when(this.solr.getCore(CORE_NAME)).thenThrow(failure).thenReturn(this.core);
        this.lazyCore.setRetryDelay(TimeUnit.HOURS.toNanos(1));

        // The failure is thrown to the caller, without being logged.
        assertSame(failure, assertThrows(SolrException.class, () -> this.lazyCore.initialize(CORE_NAME)));
        assertEquals(0, this.logCapture.size());

        // No retry state is kept: the next request contacts Solr again despite the retry delay.
        assertSame(this.core, this.lazyCore.get());
        verify(this.solr, times(2)).getCore(CORE_NAME);
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void initializeWithEmbeddedSolrMissingCore() throws Exception
    {
        when(this.configuration.getServerType()).thenReturn(DefaultSolrConfiguration.SOLR_EMBEDDED_TYPE);

        SolrException exception = assertThrows(SolrException.class, () -> this.lazyCore.initialize(CORE_NAME));
        assertEquals(MISSING_CORE_MESSAGE, exception.getMessage());
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void getBeforeInitialize()
    {
        IllegalStateException exception = assertThrows(IllegalStateException.class, this.lazyCore::get);
        assertEquals("The Solr core is requested before the initialization of this instance", exception.getMessage());
        verifyNoInteractions(this.solr);
    }

    @Test
    void initializeTwice() throws Exception
    {
        when(this.solr.getCore(CORE_NAME)).thenReturn(this.core);
        this.lazyCore.initialize(CORE_NAME);

        IllegalStateException exception =
            assertThrows(IllegalStateException.class, () -> this.lazyCore.initialize("events"));
        assertEquals("This instance was already initialized with the Solr core [search]", exception.getMessage());
        assertSame(this.core, this.lazyCore.get());
        verify(this.solr).getCore(CORE_NAME);
    }

    @Test
    void concurrentCallerFailsFast() throws Exception
    {
        SolrException failure = new SolrException("Connection refused");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(this.solr.getCore(CORE_NAME)).thenThrow(failure).thenAnswer(invocation -> {
            entered.countDown();
            release.await();
            return this.core;
        });
        this.lazyCore.setRetryDelay(0);

        assertNull(this.lazyCore.initialize(CORE_NAME));
        assertEquals(unavailable("SolrException: Connection refused"), this.logCapture.getMessage(0));

        // A thread is requesting the core again (and waits for the Solr server).
        CompletableFuture<XWikiSolrCore> attempt = CompletableFuture.supplyAsync(() -> {
            try {
                return this.lazyCore.get();
            } catch (SolrException e) {
                throw new CompletionException(e);
            }
        });
        assertTrue(entered.await(10, TimeUnit.SECONDS));

        try {
            // Another caller does not wait for it: it gets the last failure. The timeout makes a caller which waits
            // fail the test instead of blocking it.
            SolrException exception = assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> assertThrows(SolrException.class, this.lazyCore::get));
            assertSame(failure, exception.getCause());
        } finally {
            release.countDown();
        }

        assertSame(this.core, attempt.get(10, TimeUnit.SECONDS));
        assertSame(this.core, this.lazyCore.get());
        verify(this.solr, times(2)).getCore(CORE_NAME);
        assertEquals(AVAILABLE_MESSAGE, this.logCapture.getMessage(1));
    }

    @Test
    void failureDelaysTheNextAttempt() throws Exception
    {
        SolrException failure = new SolrException("Connection refused");
        when(this.solr.getCore(CORE_NAME)).thenThrow(failure).thenReturn(this.core);
        this.lazyCore.setRetryDelay(TimeUnit.HOURS.toNanos(1));

        assertNull(this.lazyCore.initialize(CORE_NAME));
        assertEquals(unavailable("SolrException: Connection refused"), this.logCapture.getMessage(0));

        // Like any other failure, the one of the initialization delays the next attempt: the Solr server is not
        // contacted again before the retry delay, the same failure is reported.
        SolrException exception = assertThrows(SolrException.class, this.lazyCore::get);
        assertEquals("Connection refused", exception.getMessage());
        assertSame(failure, exception.getCause());
        assertSame(failure, assertThrows(SolrException.class, this.lazyCore::get).getCause());

        verify(this.solr).getCore(CORE_NAME);
        assertEquals(1, this.logCapture.size());
    }

    @Test
    void retryAfterTheDelay() throws Exception
    {
        when(this.solr.getCore(CORE_NAME)).thenThrow(new SolrException("first"), new SolrException("second"))
            .thenReturn(this.core);
        this.lazyCore.setRetryDelay(0);

        assertNull(this.lazyCore.initialize(CORE_NAME));
        assertThrows(SolrException.class, this.lazyCore::get);
        assertSame(this.core, this.lazyCore.get());
        assertSame(this.core, this.lazyCore.get());

        verify(this.solr, times(3)).getCore(CORE_NAME);
        // Only one warning while the core is not available, then one information message when it's back.
        assertEquals(3, this.logCapture.size());
        assertEquals(unavailable("SolrException: first"), this.logCapture.getMessage(0));
        assertEquals(LogLevel.WARN.name(), this.logCapture.getLogEvent(0).getLevel().toString());
        assertEquals("The Solr core [search] is still not available: [SolrException: second]",
            this.logCapture.getMessage(1));
        assertEquals(LogLevel.DEBUG.name(), this.logCapture.getLogEvent(1).getLevel().toString());
        assertEquals(AVAILABLE_MESSAGE, this.logCapture.getMessage(2));
        assertEquals(LogLevel.INFO.name(), this.logCapture.getLogEvent(2).getLevel().toString());
    }

    @Test
    void missingCore() throws Exception
    {
        this.lazyCore.setRetryDelay(0);

        assertNull(this.lazyCore.initialize(CORE_NAME));
        assertEquals(unavailable("SolrException: " + MISSING_CORE_MESSAGE), this.logCapture.getMessage(0));

        SolrException exception = assertThrows(SolrException.class, this.lazyCore::get);
        assertEquals(MISSING_CORE_MESSAGE, exception.getMessage());
        assertEquals("The Solr core [search] is still not available: [SolrException: " + MISSING_CORE_MESSAGE + "]",
            this.logCapture.getMessage(1));
    }

    @Test
    void solrComponentFailure() throws Exception
    {
        // The Solr component cannot be found (or fails to initialize): the component Provider throws a
        // RuntimeException.
        this.componentManager.unregisterComponent(Solr.class, "default");
        this.lazyCore.setRetryDelay(0);

        // The lookup does not need the Solr component.
        assertNotNull(this.componentManager.getInstance(LazySolrCore.class));

        assertNull(this.lazyCore.initialize(CORE_NAME));
        assertTrue(this.logCapture.getMessage(0)
            .startsWith("The Solr core [search] is not available: [ComponentLookupException: "));

        SolrException exception = assertThrows(SolrException.class, this.lazyCore::get);
        assertEquals("Failed to get the Solr component", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception.getCause());
        assertInstanceOf(ComponentLookupException.class, exception.getCause().getCause());
        assertTrue(this.logCapture.getMessage(1)
            .startsWith("The Solr core [search] is still not available: [ComponentLookupException: "));
    }
}
