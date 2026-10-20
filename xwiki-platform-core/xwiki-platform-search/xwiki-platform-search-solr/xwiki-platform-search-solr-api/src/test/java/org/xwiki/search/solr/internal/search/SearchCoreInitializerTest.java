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
package org.xwiki.search.solr.internal.search;

import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.SolrSchemaUtils;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link SearchCoreInitializer}.
 * 
 * @version $Id$
 */
@ComponentTest
class SearchCoreInitializerTest
{
    private static final long OLD_VERSION = AbstractSolrCoreInitializer.SCHEMA_VERSION_16_6;

    private static final long NEW_VERSION = AbstractSolrCoreInitializer.SCHEMA_VERSION_18_10;

    @InjectMockComponents
    private SearchCoreInitializer initializer;

    @MockComponent
    private SearchCoreMigrationManager migrations;

    @MockComponent
    private SolrSchemaUtils solrSchema;

    @MockComponent
    private Solr solr;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Mock
    private XWikiSolrCore core;

    @Mock
    private SolrClient client;

    @BeforeEach
    void beforeEach()
    {
        when(this.core.getClient()).thenReturn(this.client);
        when(this.core.getSolrName()).thenReturn("xwiki_search_9");
    }

    private void setConfigurationVersion(long version) throws Exception
    {
        when(this.solrSchema.getVersion(this.core, SolrSchemaUtils.SOLR_TYPENAME_CVERSION)).thenReturn(version);
    }

    private void setIndexVersion(long version) throws Exception
    {
        SolrDocument document = new SolrDocument();
        document.setField(SearchCoreInitializer.INDEX_VERSION_FIELD, version);
        when(this.client.getById(SearchCoreInitializer.INDEX_VERSION_ID)).thenReturn(document);
    }

    private void setDocumentCount(long count) throws Exception
    {
        SolrDocumentList results = new SolrDocumentList();
        results.setNumFound(count);
        QueryResponse response = new QueryResponse();
        response.setResponse(new NamedList<>(Map.of("response", results)));
        when(this.client.query(any(SolrQuery.class))).thenReturn(response);
    }

    private void assertIndexVersionSet(long version) throws Exception
    {
        ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
        verify(this.client).add(captor.capture());
        assertEquals(SearchCoreInitializer.INDEX_VERSION_ID, captor.getValue().getFieldValue("id"));
        assertEquals(version, captor.getValue().getFieldValue(SearchCoreInitializer.INDEX_VERSION_FIELD));
        verify(this.client).commit();
    }

    @Test
    void initializeWithOldConfigurationAndNoIndexVersion() throws Exception
    {
        setConfigurationVersion(OLD_VERSION);

        this.initializer.initialize(this.core);

        verify(this.solr, never()).deleteIndex(any());
        verify(this.client, never()).add(any(SolrInputDocument.class));
        verify(this.migrations).update(this.core);
    }

    @Test
    void initializeWithMatchingIndexVersion() throws Exception
    {
        setConfigurationVersion(NEW_VERSION);
        setIndexVersion(NEW_VERSION);

        this.initializer.initialize(this.core);

        verify(this.solr, never()).deleteIndex(any());
        verify(this.client, never()).add(any(SolrInputDocument.class));
        verify(this.migrations).update(this.core);
    }

    @Test
    void initializeWithNewConfigurationAndOldData() throws Exception
    {
        setConfigurationVersion(NEW_VERSION);
        setDocumentCount(42);

        this.initializer.initialize(this.core);

        verify(this.solr).deleteIndex(this.core);
        assertIndexVersionSet(NEW_VERSION);
        verify(this.migrations).update(this.core);

        assertEquals("The data of the Solr core [xwiki_search_9] was produced for a different version of its"
            + " configuration ([null] instead of [181000000]), deleting its index. The index will be filled again by"
            + " the synchronization with the database (if solr.synchronizeAtStartup is enabled, which is the default,"
            + " otherwise it needs to be triggered manually).", this.logCapture.getMessage(0));
    }

    @Test
    void initializeWithNewConfigurationAndEmptyIndex() throws Exception
    {
        setConfigurationVersion(NEW_VERSION);
        setDocumentCount(0);

        this.initializer.initialize(this.core);

        verify(this.solr, never()).deleteIndex(any());
        assertIndexVersionSet(NEW_VERSION);
        verify(this.migrations).update(this.core);
    }

    @Test
    void initializeWithOldConfigurationAndNewData() throws Exception
    {
        setConfigurationVersion(OLD_VERSION);
        setIndexVersion(NEW_VERSION);
        setDocumentCount(42);

        this.initializer.initialize(this.core);

        verify(this.solr).deleteIndex(this.core);
        verify(this.client, never()).add(any(SolrInputDocument.class));
        verify(this.migrations).update(this.core);

        assertEquals("The data of the Solr core [xwiki_search_9] was produced for a different version of its"
            + " configuration ([181000000] instead of [null]), deleting its index. The index will be filled again by"
            + " the synchronization with the database (if solr.synchronizeAtStartup is enabled, which is the default,"
            + " otherwise it needs to be triggered manually).", this.logCapture.getMessage(0));
    }
}
