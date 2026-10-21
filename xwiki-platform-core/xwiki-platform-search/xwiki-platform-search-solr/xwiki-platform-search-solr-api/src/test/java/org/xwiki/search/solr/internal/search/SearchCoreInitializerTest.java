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

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.SolrSchemaUtils;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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
    @InjectMockComponents
    private SearchCoreInitializer initializer;

    @MockComponent
    private SearchCoreMigrationManager migrations;

    @MockComponent
    private SolrSchemaUtils solrSchema;

    @MockComponent
    private Solr solr;

    @MockComponent
    private SolrConfiguration configuration;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Mock
    private XWikiSolrCore core;

    @BeforeEach
    void beforeEach()
    {
        when(this.core.getSolrName()).thenReturn("xwiki_search_9");
    }

    @Test
    void initializeWithCompliantIndex() throws Exception
    {
        when(this.solrSchema.getNonCompliantFields(this.core)).thenReturn(Set.of());

        this.initializer.initialize(this.core);

        verify(this.migrations).update(this.core);
        verify(this.solr, never()).deleteIndex(any());
    }

    @Test
    void initializeWithNonCompliantIndex() throws Exception
    {
        when(this.solrSchema.getNonCompliantFields(this.core)).thenReturn(Set.of("hidden"));
        when(this.configuration.synchronizeAtStartup()).thenReturn(true);

        this.initializer.initialize(this.core);

        // The compliance is checked against the schema resulting from the migrations
        InOrder order = inOrder(this.migrations, this.solrSchema, this.solr);
        order.verify(this.migrations).update(this.core);
        order.verify(this.solrSchema).getNonCompliantFields(this.core);
        order.verify(this.solr).deleteIndex(this.core);

        assertEquals("The index of the Solr core [xwiki_search_9] contains fields which are not compatible with their"
            + " definition in the schema [hidden] (usually because the configuration of the core was upgraded),"
            + " deleting its index. It will be filled again by the synchronization with the database.",
            this.logCapture.getMessage(0));
    }

    @Test
    void initializeWithNonCompliantIndexWithoutSynchronizationAtStartup() throws Exception
    {
        when(this.solrSchema.getNonCompliantFields(this.core)).thenReturn(Set.of("hidden"));
        when(this.configuration.synchronizeAtStartup()).thenReturn(false);

        this.initializer.initialize(this.core);

        verify(this.solr).deleteIndex(this.core);

        assertEquals("The index of the Solr core [xwiki_search_9] contains fields which are not compatible with their"
            + " definition in the schema [hidden] (usually because the configuration of the core was upgraded),"
            + " deleting its index. Since solr.synchronizeAtStartup is disabled, it needs to be filled again by"
            + " triggering a synchronization with the database manually.", this.logCapture.getMessage(0));
    }

    @Test
    void initializeWhenComplianceCheckFails() throws Exception
    {
        when(this.solrSchema.getNonCompliantFields(this.core))
            .thenThrow(new SolrException("Failed to get the segments", new IOException("Forbidden")));

        this.initializer.initialize(this.core);

        verify(this.migrations).update(this.core);
        verify(this.solr, never()).deleteIndex(any());

        assertEquals("Failed to check if the index of the Solr core [xwiki_search_9] is compliant with its schema:"
            + " IOException: Forbidden", this.logCapture.getMessage(0));
    }
}
