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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultSolrConfiguration}.
 * 
 * @version $Id$
 */
@ComponentTest
public class DefaultSolrConfigurationTest
{
    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource source;

    @InjectMockComponents
    private DefaultSolrConfiguration configuration;

    @Test
    public void getServerType()
    {
        when(this.source.getProperty(DefaultSolrConfiguration.SOLR_TYPE_PROPERTY,
            DefaultSolrConfiguration.SOLR_TYPE_DEFAULT)).thenReturn("toto");

        assertEquals("toto", this.configuration.getServerType());
    }

    @Test
    public void getIndexerBatchSize()
    {
        when(this.source.getProperty(DefaultSolrConfiguration.SOLR_INDEXER_BATCH_SIZE_PROPERTY,
            DefaultSolrConfiguration.SOLR_INDEXER_BATCH_SIZE_DEFAULT)).thenReturn(42);

        assertEquals(42, this.configuration.getIndexerBatchSize());
    }

    @Test
    public void getIndexerBatchMaxLengh()
    {
        when(this.source.getProperty(DefaultSolrConfiguration.SOLR_INDEXER_BATCH_MAXLENGH_PROPERTY,
            DefaultSolrConfiguration.SOLR_INDEXER_BATCH_MAXLENGH_DEFAULT)).thenReturn(42);

        assertEquals(42, this.configuration.getIndexerBatchMaxLengh());
    }

    @Test
    public void getIndexerQueueCapacity()
    {
        when(this.source.getProperty(DefaultSolrConfiguration.SOLR_INDEXER_QUEUE_CAPACITY_PROPERTY,
            DefaultSolrConfiguration.SOLR_INDEXER_QUEUE_CAPACITY_DEFAULT)).thenReturn(42);

        assertEquals(42, this.configuration.getIndexerQueueCapacity());

    }

    @Test
    public void synchronizeAtStartup()
    {
        when(this.source.getProperty(DefaultSolrConfiguration.SOLR_SYNCHRONIZE_AT_STARTUP,
            DefaultSolrConfiguration.SOLR_SYNCHRONIZE_AT_STARTUP_DEFAULT)).thenReturn(true);

        assertTrue(this.configuration.synchronizeAtStartup());

        when(this.source.getProperty(DefaultSolrConfiguration.SOLR_SYNCHRONIZE_AT_STARTUP,
            DefaultSolrConfiguration.SOLR_SYNCHRONIZE_AT_STARTUP_DEFAULT)).thenReturn(false);

        assertFalse(this.configuration.synchronizeAtStartup());
    }
}
