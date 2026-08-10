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
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Test;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.search.SearchCoreInitializer;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

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
    private Solr solr;

    private final SolrClient client = mock();

    @InjectMockComponents
    private SolrClientInstance instance;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        XWikiSolrCore core = mock();
        when(core.getClient()).thenReturn(this.client);
        when(this.solr.getCore(SearchCoreInitializer.CORE_NAME)).thenReturn(core);
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
