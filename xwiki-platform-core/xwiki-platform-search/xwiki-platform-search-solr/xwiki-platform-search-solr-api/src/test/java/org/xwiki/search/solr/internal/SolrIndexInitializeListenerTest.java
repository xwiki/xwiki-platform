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

import java.util.Arrays;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.job.IndexerRequest;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Check the behaviour of {@link SolrIndexInitializeListener}.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@ComponentTest
public class SolrIndexInitializeListenerTest
{
    @InjectMockComponents
    private SolrIndexInitializeListener solrIndexInitializeListener;

    @MockComponent
    private Provider<SolrIndexer> solrIndexerProvider;

    @MockComponent
    private SolrIndexer solrIndexer;

    @MockComponent
    private SolrConfiguration configuration;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @BeforeEach
    void setup()
    {
        when(solrIndexerProvider.get()).thenReturn(solrIndexer);
    }

    @Test
    void onEventApplicationReadyForFarm() throws SolrIndexerException
    {
        when(this.configuration.synchronizeAtStartupMode()).thenReturn(SolrConfiguration.SynchronizeAtStartupMode.FARM);

        when(this.configuration.synchronizeAtStartup()).thenReturn(false);
        this.solrIndexInitializeListener.onEvent(new ApplicationReadyEvent(), null, null);
        verify(this.solrIndexer, never()).startIndex(any());

        when(this.configuration.synchronizeAtStartup()).thenReturn(true);
        this.solrIndexInitializeListener.onEvent(new ApplicationReadyEvent(), null, null);
        IndexerRequest indexerRequest = new IndexerRequest();
        indexerRequest.setId(Arrays.asList("solr", "indexer"));
        verify(this.solrIndexer).startIndex(indexerRequest);
    }

    @Test
    void onEventWikiReadyForWiki() throws SolrIndexerException
    {
        when(this.configuration.synchronizeAtStartupMode()).thenReturn(SolrConfiguration.SynchronizeAtStartupMode.WIKI);
        when(this.entityReferenceSerializer.serialize(new WikiReference("mywiki"))).thenReturn("wiki:mywiki");

        when(this.configuration.synchronizeAtStartup()).thenReturn(false);
        this.solrIndexInitializeListener.onEvent(new WikiReadyEvent("mywiki"), null, null);
        verify(this.solrIndexer, never()).startIndex(any());

        when(this.configuration.synchronizeAtStartup()).thenReturn(true);
        this.solrIndexInitializeListener.onEvent(new WikiReadyEvent("mywiki"), null, null);
        IndexerRequest indexerRequest = new IndexerRequest();
        indexerRequest.setRootReference(new WikiReference("mywiki"));
        indexerRequest.setId(Arrays.asList("solr", "indexer", "wiki:mywiki"));
        verify(this.solrIndexer).startIndex(indexerRequest);
    }

    @Test
    void onEventWikiReadyForFarm() throws SolrIndexerException
    {
        when(this.configuration.synchronizeAtStartupMode()).thenReturn(SolrConfiguration.SynchronizeAtStartupMode.FARM);

        when(this.configuration.synchronizeAtStartup()).thenReturn(false);
        this.solrIndexInitializeListener.onEvent(new WikiReadyEvent("foo"), null, null);
        verify(this.solrIndexer, never()).startIndex(any());

        when(this.configuration.synchronizeAtStartup()).thenReturn(true);
        this.solrIndexInitializeListener.onEvent(new WikiReadyEvent("foo"), null, null);
        verify(this.solrIndexer, never()).startIndex(any());
    }

    @Test
    void onEventApplicationReadyForWiki() throws SolrIndexerException, WikiManagerException
    {
        when(this.configuration.synchronizeAtStartupMode()).thenReturn(SolrConfiguration.SynchronizeAtStartupMode.WIKI);
        WikiReference wikiReference = new WikiReference("foo");
        WikiDescriptor wikiDescriptor = mock(WikiDescriptor.class);
        when(this.wikiDescriptorManager.getMainWikiDescriptor()).thenReturn(wikiDescriptor);
        when(wikiDescriptor.getReference()).thenReturn(wikiReference);
        when(this.entityReferenceSerializer.serialize(wikiReference)).thenReturn("wiki:foo");

        when(this.configuration.synchronizeAtStartup()).thenReturn(false);
        this.solrIndexInitializeListener.onEvent(new ApplicationReadyEvent(), null, null);
        verify(this.solrIndexer, never()).startIndex(any());

        when(this.configuration.synchronizeAtStartup()).thenReturn(true);
        this.solrIndexInitializeListener.onEvent(new ApplicationReadyEvent(), null, null);

        IndexerRequest indexerRequest = new IndexerRequest();
        indexerRequest.setRootReference(wikiReference);
        indexerRequest.setId(Arrays.asList("solr", "indexer", "wiki:foo"));
        verify(this.solrIndexer).startIndex(indexerRequest);
    }
}
