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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.store.ReadyIndicator;
import org.xwiki.store.StoreStoppedException;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultSolrIndexer}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultSolrIndexerTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private SolrConfiguration configuration;

    // Needed by the resolve and index threads to initialize their execution context.
    @MockComponent
    private Execution execution;

    @MockComponent
    private ExecutionContextManager executionContextManager;

    @InjectMockComponents
    private DefaultSolrIndexer indexer;

    @BeforeComponent
    void beforeComponent()
    {
        when(this.configuration.getIndexerQueueCapacity()).thenReturn(10);

        // The resolve and index threads get their execution context components through the static component manager.
        Utils.setComponentManager(this.componentManager);
    }

    @Test
    void waitReadyWhenDisposed() throws Exception
    {
        this.indexer.dispose();

        ReadyIndicator readyIndicator = this.indexer.waitReady();

        // The indexer won't become ready again, which is reported as a stop and not as a failure.
        ExecutionException exception =
            assertThrows(ExecutionException.class, () -> readyIndicator.get(10, TimeUnit.SECONDS));
        StoreStoppedException cause = assertInstanceOf(StoreStoppedException.class, exception.getCause());
        assertEquals("The indexer has been disposed", cause.getMessage());
    }
}
