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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.bridge.internal.DocumentContextExecutor;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.metadata.SolrLinkExistenceDependencies;
import org.xwiki.search.solr.internal.metadata.SolrMetadataExtractor;
import org.xwiki.search.solr.internal.metadata.XWikiSolrInputDocument;
import org.xwiki.store.ReadyIndicator;
import org.xwiki.store.StoreStoppedException;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultSolrIndexer}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultSolrIndexerTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private SolrConfiguration configuration;

    // Needed by the resolve and index threads to initialize their execution context.
    @MockComponent
    private Execution execution;

    @MockComponent
    private ExecutionContextManager executionContextManager;

    @MockComponent
    private SolrInstance solrInstance;

    @MockComponent
    private SolrLinkExistenceDependencies linkExistenceDependencies;

    @MockComponent
    private EntityReferenceFactory entityReferenceFactory;

    @MockComponent
    private DocumentContextExecutor documentContextExecutor;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("document")
    private SolrMetadataExtractor documentExtractor;

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
    void indexExistenceDependents() throws Exception
    {
        // Make the indexer able to extract documents.
        XWikiContext xcontext = mock();
        when(xcontext.getWiki()).thenReturn(mock());
        when(this.xcontextProvider.get()).thenReturn(xcontext);
        doAnswer(invocation -> {
            ExecutionContext executionContext = invocation.getArgument(0);
            executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xcontext);
            return null;
        }).when(this.executionContextManager).initialize(any());
        when(this.documentContextExecutor.call(any(), any()))
            .then(invocation -> invocation.<Callable<?>>getArgument(0).call());

        // Don't commit after each document, so that the indexing of the source document and the creation of the
        // target document end up in the same batch.
        when(this.configuration.getIndexerBatchSize()).thenReturn(1000);
        when(this.configuration.getIndexerBatchMaxLengh()).thenReturn(Integer.MAX_VALUE);

        // The resolver thread dispatches the entries in order: once it reaches the entry queued after the creation of
        // the target document, the creation is in the index queue.
        DocumentReference lastReference = new DocumentReference("wiki", "Space", "Last");
        Thread testThread = Thread.currentThread();
        CountDownLatch creationQueued = new CountDownLatch(1);
        when(this.entityReferenceFactory.getReference(any())).then(invocation -> {
            if (lastReference.equals(invocation.getArgument(0)) && Thread.currentThread() != testThread) {
                creationQueued.countDown();
            }
            return invocation.getArgument(0);
        });

        // Record the interactions with the index, in the order of the indexer thread.
        List<String> events = new CopyOnWriteArrayList<>();

        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Source", Locale.ROOT);
        when(this.documentExtractor.getSolrDocument(sourceReference)).then(invocation -> {
            // Extracting the source document takes until the target document is created.
            assertTrue(creationQueued.await(10, TimeUnit.SECONDS));
            events.add("extract");
            return new XWikiSolrInputDocument();
        });
        doAnswer(invocation -> events.add("add")).when(this.solrInstance).add(anyList());
        doAnswer(invocation -> events.add("commit")).when(this.solrInstance).commit();

        DocumentReference targetReference = new DocumentReference("wiki", "Space", "Target");
        AtomicInteger queueSizeDuringSearch = new AtomicInteger(-1);
        when(this.linkExistenceDependencies.getDependents(Set.of(targetReference))).then(invocation -> {
            events.add("search");
            queueSizeDuringSearch.set(this.indexer.getQueueSize());
            return Set.of(sourceReference);
        });

        // The source document is indexed while the target document doesn't exist yet, then the target is created.
        this.indexer.index(sourceReference, false);
        this.indexer.indexExistenceDependents(new DocumentReference(targetReference, Locale.FRENCH));
        this.indexer.index(lastReference, false);

        // Indexing the dependents is part of what the ready indicator waits for.
        this.indexer.waitReady().get(10, TimeUnit.SECONDS);

        // The dependents are searched only once the source document indexed before the creation of the target is
        // visible to the search, otherwise it would be missed and keep its outdated links.
        // Empty batches are committed too, which is not relevant here.
        List<String> relevantEvents = new ArrayList<>();
        for (String event : events) {
            if (!event.equals("commit") || (!relevantEvents.isEmpty() && !relevantEvents.get(relevantEvents.size() - 1)
                .equals("commit"))) {
                relevantEvents.add(event);
            }
        }
        assertEquals(List.of("extract", "add", "commit", "search", "extract", "add", "commit"), relevantEvents);

        // The creation of the target is still part of the queue until its dependents are indexed, so that waiting for
        // an empty queue includes them.
        assertTrue(queueSizeDuringSearch.get() > 0);
    }

    @Test
    void indexExistenceDependentsWhenSearchFails() throws Exception
    {
        doAnswer(invocation -> {
            ExecutionContext executionContext = invocation.getArgument(0);
            executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, mock(XWikiContext.class));
            return null;
        }).when(this.executionContextManager).initialize(any());
        when(this.entityReferenceFactory.getReference(any())).then(invocation -> invocation.getArgument(0));

        DocumentReference targetReference = new DocumentReference("wiki", "Space", "Target");
        when(this.linkExistenceDependencies.getDependents(any()))
            .thenThrow(new SolrIndexerException("Search failure"));

        this.indexer.indexExistenceDependents(targetReference);

        // The failure doesn't prevent the indexer from becoming ready.
        this.indexer.waitReady().get(10, TimeUnit.SECONDS);

        assertEquals("Failed to search the documents to index after the creation or deletion of [[wiki:Space.Target]]",
            this.logCapture.getMessage(0));
        verify(this.documentExtractor, never()).getSolrDocument(any());
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
