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
package org.xwiki.search.solr.internal.job;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.job.AbstractDocumentIterator.DocumentIteratorEntry;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IndexerJob}.
 *
 * @version $Id$
 */
@ComponentTest
class IndexerJobTest
{
    private static final String WIKI = "wiki";

    private static final String SPACE = "space";

    private static final DocumentReference DOCUMENT_ONE = new DocumentReference(WIKI, SPACE, "document1");

    private static final DocumentReference DOCUMENT_TWO = new DocumentReference(WIKI, SPACE, "document2");

    private static final String VERSION_ONE = "1.0";

    private static final String VERSION_TWO = "2.0";

    private static final DocumentIteratorEntry ENTRY_ONE =
        new DocumentIteratorEntry(DOCUMENT_ONE.getWikiReference(), 1, VERSION_ONE);

    private static final DocumentIteratorEntry ENTRY_TWO =
        new DocumentIteratorEntry(DOCUMENT_TWO.getWikiReference(), 1, VERSION_TWO);

    private static final JobGroupPath INDEXER_JOB_GROUP_PATH = new JobGroupPath(List.of("solr", "indexer"));

    @MockComponent
    private SolrIndexer mockIndexer;

    @MockComponent
    @Named("database")
    private DocumentIterator<DocumentIteratorEntry> mockDatabaseIterator;

    @MockComponent
    @Named("solr")
    private DocumentIterator<DocumentIteratorEntry> mockSolrIterator;

    @MockComponent
    private EntityReferenceSerializer<String> mockEntityReferenceSerializer;

    @MockComponent
    private DocumentAccessBridge mockDocumentAccessBridge;

    @MockComponent
    private SolrInstance mockSolrInstance;

    @InjectMockComponents
    private IndexerJob indexerJob;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private final IndexerRequest request = new IndexerRequest();

    @BeforeEach
    void setUp()
    {
        this.indexerJob.initialize(this.request);
    }

    @Test
    void testRunInternalOverwrite() throws Exception
    {
        this.request.setOverwrite(true);
        this.request.setRootReference(DOCUMENT_ONE);

        this.indexerJob.runInternal();

        verify(this.mockIndexer).index(DOCUMENT_ONE, true);
        assertEquals("Index documents in [{}].", this.logCapture.getLogEvent(0).getMessage());
        assertEquals(DOCUMENT_ONE, this.logCapture.getLogEvent(0).getArgumentArray()[0]);
    }

    @Test
    void testRunInternalUpdateSolrIndexNoChanges() throws Exception
    {
        mockIterator(this.mockDatabaseIterator, Pair.of(DOCUMENT_ONE, ENTRY_ONE));
        mockIterator(this.mockSolrIterator, Pair.of(DOCUMENT_ONE, ENTRY_ONE));

        this.indexerJob.runInternal();

        verifyNoInteractions(this.mockIndexer);
        assertLog(0, 0, 0);
    }

    @Test
    void testRunInternalUpdateSolrIndexAddAction() throws Exception
    {
        mockIterator(this.mockDatabaseIterator, Pair.of(DOCUMENT_ONE, ENTRY_ONE), Pair.of(DOCUMENT_TWO, ENTRY_TWO));
        mockIterator(this.mockSolrIterator, Pair.of(DOCUMENT_ONE, ENTRY_ONE));

        this.indexerJob.runInternal();

        verify(this.mockIndexer).index(DOCUMENT_TWO, true);

        assertLog(1, 0, 0);
    }

    @Test
    void testGetGroupPathWithoutRootReference()
    {
        JobGroupPath result = this.indexerJob.getGroupPath();

        assertEquals(INDEXER_JOB_GROUP_PATH, result);
    }

    @Test
    void testGetGroupPathWithRootReference()
    {
        EntityReference rootReference = new DocumentReference(WIKI, SPACE, "documentRoot");
        String expectedPath = "serializedRootReference";
        when(this.mockEntityReferenceSerializer.serialize(rootReference)).thenReturn(expectedPath);

        this.request.setRootReference(rootReference);

        JobGroupPath result = this.indexerJob.getGroupPath();

        assertEquals(new JobGroupPath(expectedPath, INDEXER_JOB_GROUP_PATH), result);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testRunInternalUpdateSolrIndexDeleteAction(boolean exists) throws Exception
    {
        mockIterator(this.mockDatabaseIterator, Pair.of(DOCUMENT_ONE, ENTRY_ONE));
        mockIterator(this.mockSolrIterator, Pair.of(DOCUMENT_ONE, ENTRY_ONE), Pair.of(DOCUMENT_TWO, ENTRY_TWO));

        when(this.mockDocumentAccessBridge.exists(DOCUMENT_TWO)).thenReturn(exists);

        this.indexerJob.runInternal();

        if (!exists) {
            verify(this.mockIndexer).delete(DOCUMENT_TWO, true);
            assertLog(0, 1, 0);
        } else {
            assertLog(0, 0, 0);
        }
        verifyNoMoreInteractions(this.mockIndexer);
    }

    @Test
    void testRunInternalUpdateSolrIndexUpdateAction() throws Exception
    {
        mockIterator(this.mockDatabaseIterator, Pair.of(DOCUMENT_ONE, ENTRY_ONE), Pair.of(DOCUMENT_TWO, ENTRY_TWO));
        mockIterator(this.mockSolrIterator, Pair.of(DOCUMENT_ONE, ENTRY_ONE), Pair.of(DOCUMENT_TWO, ENTRY_ONE));

        this.indexerJob.runInternal();

        verify(this.mockIndexer).index(DOCUMENT_TWO, true);
        verifyNoMoreInteractions(this.mockIndexer);
        assertLog(0, 0, 1);
    }

    private void assertLog(int added, int deleted, int updated)
    {
        assertEquals("%d documents added, %d deleted and %d updated during the synchronization of the Solr index."
            .formatted(added, deleted, updated), this.logCapture.getLogEvent(0).getFormattedMessage());
    }

    @SafeVarargs
    private void mockIterator(DocumentIterator<DocumentIteratorEntry> mockIterator,
        Pair<DocumentReference, DocumentIteratorEntry>... pairs)
    {
        var iterator = Arrays.asList(pairs).iterator();
        when(mockIterator.hasNext()).then(invocation -> iterator.hasNext());
        when(mockIterator.next()).then(invocation -> iterator.next());
    }
}
