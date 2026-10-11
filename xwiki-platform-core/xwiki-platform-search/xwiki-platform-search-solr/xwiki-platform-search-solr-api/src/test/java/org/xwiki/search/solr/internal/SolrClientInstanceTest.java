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

import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Test;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SolrClientInstance} and the behaviour it inherits from {@link AbstractSolrInstance}.
 *
 * @version $Id$
 */
@ComponentTest
class SolrClientInstanceTest
{
    @MockComponent
    private LazySolrCore searchCore;

    private final SolrClient client = mock();

    private final XWikiSolrCore core = mock();

    @InjectMockComponents
    private SolrClientInstance instance;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        when(this.core.getClient()).thenReturn(this.client);
        when(this.searchCore.initialize(SolrClientInstance.CORE_NAME)).thenReturn(this.core);
    }

    @BeforeComponent("searchCoreResolvedOnFirstUse")
    void beforeSearchCoreResolvedOnFirstUse() throws Exception
    {
        // The search core is not available when the instance is initialized (a remote Solr server which cannot be
        // reached), then it is
        when(this.searchCore.initialize(SolrClientInstance.CORE_NAME)).thenReturn(null);
        when(this.searchCore.get()).thenReturn(this.core);
    }

    @Test
    void searchCoreResolvedOnFirstUse() throws Exception
    {
        assertNull(this.instance.server);
        assertNull(this.instance.getServer());

        SolrDocument document = new SolrDocument();
        when(this.client.getById("id")).thenReturn(document);

        // Every operation resolves the search core when needed, the get by id included
        assertSame(document, this.instance.get("id"));
        assertSame(this.core, this.instance.server);
        assertSame(this.client, this.instance.getServer());

        // The resolved search core is kept
        this.instance.commit();
        verify(this.client).commit();
        verify(this.searchCore).get();
    }

    @Test
    void addEmptyDocumentListSendsNoRequest() throws Exception
    {
        this.instance.add(List.of());

        verifyNoInteractions(this.client);
    }

    @Test
    void addDocumentList() throws Exception
    {
        List<SolrInputDocument> documents = List.of(new SolrInputDocument());

        this.instance.add(documents);

        verify(this.client).add(documents);
    }
}
